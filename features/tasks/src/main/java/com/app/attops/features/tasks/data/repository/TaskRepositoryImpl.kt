package com.app.attops.features.tasks.data.repository

import android.util.Log
import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.*
import com.app.attops.features.tasks.domain.repository.TaskRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import java.time.Instant
import com.app.attops.core.common.database.AttendanceDao
import com.app.attops.core.common.database.AttendanceEntity
import com.app.attops.features.tasks.data.worker.SyncScheduler
import io.github.jan.supabase.storage.Storage
import java.io.File
import java.util.UUID

class TaskRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val storage: Storage,
    private val attendanceDao: AttendanceDao,
    private val syncScheduler: SyncScheduler
) : TaskRepository {

    private val tag = "TaskRepo"

    private suspend fun getFullProfile(): User? {
        val userId = auth.currentUserOrNull()?.id ?: return null
        return try {
            postgrest.from("users")
                .select(columns = Columns.ALL) {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch profile", e)
            null
        }
    }

    override fun getTasks(): Flow<Result<List<Task>>> = flow {
        emit(Result.Loading)
        val profile = getFullProfile() ?: return@flow emit(Result.Error(message = "Session expired"))
        try {
            val tasks = postgrest.from("tasks")
                .select(columns = Columns.ALL) {
                    filter { eq("organization_id", profile.organizationId) }
                }
                .decodeList<Task>()
            emit(Result.Success(tasks))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun getEmployees(): Flow<Result<List<User>>> = flow {
        emit(Result.Loading)
        val profile = getFullProfile() ?: return@flow emit(Result.Error(message = "Unauthorized"))
        try {
            val employees = postgrest.from("users")
                .select(columns = Columns.ALL) {
                    filter { eq("organization_id", profile.organizationId) }
                }
                .decodeList<User>()
            emit(Result.Success(employees))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun getTaskById(id: String): Flow<Result<Task>> = flow {
        emit(Result.Loading)
        try {
            val task = postgrest.from("tasks")
                .select(columns = Columns.ALL) {
                    filter { eq("id", id) }
                }
                .decodeSingleOrNull<Task>()
            
            if (task != null) {
                emit(Result.Success(task))
            } else {
                emit(Result.Error(message = "Task not found."))
            }
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override suspend fun createTask(task: Task): Result<Unit> {
        return try {
            val profile = getFullProfile() ?: return Result.Error(message = "Session expired")
            val taskToInsert = task.copy(
                organizationId = profile.organizationId,
                createdBy = profile.id,
                status = TaskStatus.PENDING
            )
            postgrest.from("tasks").insert(taskToInsert)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit> {
        return try {
            val updateMap = mutableMapOf<String, Any>("status" to status.name)
            if (status == TaskStatus.COMPLETED) {
                updateMap["completed_at"] = Instant.now().toString()
            }
            
            postgrest.from("tasks").update(updateMap) {
                filter { eq("id", taskId) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update task status", e)
            Result.Error(e)
        }
    }

    override suspend fun checkIn(
        taskId: String,
        latitude: Double,
        longitude: Double,
        distance: Double,
        imagePath: String?
    ): Result<Unit> {
        return try {
            val profile = getFullProfile() ?: return Result.Error(message = "Unauthorized")

            val record = AttendanceEntity(
                clientSideId = UUID.randomUUID().toString(),
                taskId = taskId,
                organizationId = profile.organizationId,
                employeeId = profile.id,
                type = "CHECK_IN",
                latitude = latitude,
                longitude = longitude,
                timestamp = Instant.now().toString(),
                localImagePath = imagePath ?: "",
                status = "PENDING"
            )

            val localId = attendanceDao.insertAttendance(record)
            
            // ATOMIC SYNC: Force wait for cloud update
            val syncResult = syncRecord(record.copy(localId = localId.toInt()))
            
            if (syncResult is Result.Error) {
                syncScheduler.scheduleAttendanceSync()
                Result.Success(Unit)
            } else {
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Log.e(tag, "Check-in failed", e)
            Result.Error(e, "Check-in captured locally.")
        }
    }

    override suspend fun checkOut(
        taskId: String,
        latitude: Double,
        longitude: Double,
        imagePath: String?
    ): Result<Unit> {
        return try {
            val profile = getFullProfile() ?: return Result.Error(message = "Unauthorized")

            val clientSideId = attendanceDao.getLatestCheckInId(taskId) 
                ?: UUID.randomUUID().toString()

            val record = AttendanceEntity(
                clientSideId = clientSideId,
                taskId = taskId,
                organizationId = profile.organizationId,
                employeeId = profile.id,
                type = "CHECK_OUT",
                latitude = latitude,
                longitude = longitude,
                timestamp = Instant.now().toString(),
                localImagePath = imagePath ?: "",
                status = "PENDING"
            )

            val localId = attendanceDao.insertAttendance(record)
            
            // ATOMIC SYNC: Force wait
            val syncResult = syncRecord(record.copy(localId = localId.toInt()))
            
            if (syncResult is Result.Error) {
                Result.Success(Unit)
            } else {
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Log.e(tag, "Check-out failed", e)
            Result.Error(e, "Check-out captured locally.")
        }
    }

    override suspend fun syncRecord(record: AttendanceEntity): Result<Unit> {
        return try {
            attendanceDao.updateStatus(record.localId, "UPLOADING")
            
            var imageUrl: String? = null
            if (record.localImagePath.isNotEmpty()) {
                val file = File(record.localImagePath)
                if (file.exists()) {
                    val fileName = "attendance/${record.organizationId}/${record.taskId}_${record.employeeId}_${record.type.lowercase()}.jpg"
                    storage.from("attendance-proof").upload(fileName, file.readBytes()) {
                        upsert = true
                    }
                    imageUrl = storage.from("attendance-proof").publicUrl(fileName)
                }
            }

            if (record.type == "CHECK_IN") {
                val data = mapOf(
                    "client_side_id" to record.clientSideId,
                    "organization_id" to record.organizationId,
                    "task_id" to record.taskId,
                    "employee_id" to record.employeeId,
                    "check_in_time" to record.timestamp,
                    "check_in_lat" to record.latitude,
                    "check_in_lng" to record.longitude,
                    "check_in_image_url" to imageUrl,
                    "status" to "CHECKED_IN"
                )
                
                postgrest.from("attendance_logs").upsert(data) {
                    onConflict = "client_side_id"
                }
                
                postgrest.from("tasks").update(mapOf("status" to TaskStatus.IN_PROGRESS.name)) {
                    filter { eq("id", record.taskId) }
                }
            } else {
                val data = mapOf(
                    "check_out_time" to record.timestamp,
                    "check_out_lat" to record.latitude,
                    "check_out_lng" to record.longitude,
                    "check_out_image_url" to imageUrl,
                    "status" to "CHECKED_OUT"
                )
                
                // Use UPDATE here to preserve check-in data
                postgrest.from("attendance_logs").update(data) {
                    filter { eq("client_side_id", record.clientSideId) }
                }
                
                postgrest.from("tasks").update(mapOf(
                    "status" to TaskStatus.COMPLETED.name,
                    "completed_at" to record.timestamp
                )) {
                    filter { eq("id", record.taskId) }
                }
            }

            attendanceDao.updateStatus(record.localId, "SYNCED")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Sync Error (${record.type}): ${e.message}")
            attendanceDao.updateStatus(record.localId, "PENDING")
            Result.Error(e)
        }
    }

    override fun getTaskAttendance(taskId: String): Flow<Result<TaskAttendance?>> = flow {
        emit(Result.Loading)
        val userId = auth.currentUserOrNull()?.id ?: return@flow
        try {
            val attendance = postgrest.from("attendance_logs")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("task_id", taskId)
                        eq("employee_id", userId)
                    }
                    order("check_in_time", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<TaskAttendance>()
            emit(Result.Success(attendance))
        } catch (e: Exception) {
            Log.e(tag, "Error fetching task attendance", e)
            emit(Result.Error(e))
        }
    }

    override fun getSites(): Flow<Result<List<OrganizationSite>>> = flow {
        emit(Result.Loading)
        val profile = getFullProfile()
        if (profile == null) return@flow
        try {
            val sites = postgrest.from("organization_sites")
                .select(columns = Columns.ALL) {
                    filter { eq("organization_id", profile.organizationId) }
                }
                .decodeList<OrganizationSite>()
            emit(Result.Success(sites))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override suspend fun createSite(name: String, latitude: Double, longitude: Double): Result<Unit> {
        return try {
            val profile = getFullProfile() ?: return Result.Error(message = "Unauthorized")
            val site = OrganizationSite(
                organizationId = profile.organizationId,
                name = name,
                latitude = latitude,
                longitude = longitude
            )
            postgrest.from("organization_sites").insert(site)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
