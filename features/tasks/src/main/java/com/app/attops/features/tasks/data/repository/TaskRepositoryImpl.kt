package com.app.attops.features.tasks.data.repository

import android.util.Log
import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.*
import com.app.attops.features.tasks.domain.repository.TaskRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import java.time.Instant
import com.app.attops.core.common.database.AttendanceDao
import com.app.attops.core.common.database.AttendanceEntity
import com.app.attops.features.tasks.data.worker.SyncScheduler
import io.github.jan.supabase.storage.Storage
import java.io.File
import java.util.UUID
import android.content.Context
import com.app.attops.core.common.util.ImageUtils
import com.app.attops.core.common.util.RefreshBus
import com.app.attops.core.common.util.SessionBus
import com.app.attops.core.location.LocationUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TaskRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val storage: Storage,
    private val attendanceDao: AttendanceDao,
    private val syncScheduler: SyncScheduler,
    private val refreshBus: RefreshBus,
    private val sessionBus: SessionBus,
    @ApplicationContext private val context: Context
) : TaskRepository {

    private val tag = "TaskRepo"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Caching for Modern App Standard (Flicker-free navigation)
    private val _tasksCache = MutableStateFlow<Result<List<Task>>>(Result.Loading)
    private val _employeesCache = MutableStateFlow<Result<List<User>>>(Result.Loading)
    private val _sitesCache = MutableStateFlow<Result<List<OrganizationSite>>>(Result.Loading)

    init {
        // PRODUCTION-GRADE SECURITY: Listen to Auth status changes
        scope.launch {
            auth.sessionStatus.collect { status ->
                if (status !is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
                    clearCache()
                } else {
                    refreshData()
                }
            }
        }

        scope.launch {
            refreshBus.refreshEvent.collect {
                refreshData()
            }
        }
    }

    private fun clearCache() {
        Log.d("TaskRepo", "Auth session changed. Purging task cache.")
        _tasksCache.value = Result.Loading
        _employeesCache.value = Result.Loading
        _sitesCache.value = Result.Loading
    }

    private suspend fun refreshData() {
        val profile = getFullProfile() ?: return
        fetchTasks(profile.organizationId)
        fetchEmployees(profile)
        fetchSites(profile.organizationId)
    }

    private suspend fun fetchTasks(orgId: String) {
        try {
            val tasks = postgrest.from("tasks")
                .select(columns = Columns.ALL) { filter { eq("organization_id", orgId) } }
                .decodeList<Task>()
            _tasksCache.value = Result.Success(tasks)
        } catch (e: Exception) {
            if (_tasksCache.value !is Result.Success) _tasksCache.value = Result.Error(e)
        }
    }

    private suspend fun fetchEmployees(profile: User) {
        try {
            val users = postgrest.from("users")
                .select(columns = Columns.ALL) { filter { eq("organization_id", profile.organizationId) } }
                .decodeList<User>()

            val filtered = when (profile.role) {
                UserRole.OWNER -> users
                UserRole.ADMIN -> users.filter { it.role == UserRole.ADMIN || it.role == UserRole.EMPLOYEE }
                UserRole.EMPLOYEE -> users.filter { it.id == profile.id }
            }
            _employeesCache.value = Result.Success(filtered)
        } catch (e: Exception) {
            if (_employeesCache.value !is Result.Success) _employeesCache.value = Result.Error(e)
        }
    }

    private suspend fun fetchSites(orgId: String) {
        try {
            val sites = postgrest.from("organization_sites")
                .select(columns = Columns.ALL) { filter { eq("organization_id", orgId) } }
                .decodeList<OrganizationSite>()
            _sitesCache.value = Result.Success(sites)
        } catch (e: Exception) {
            if (_sitesCache.value !is Result.Success) _sitesCache.value = Result.Error(e)
        }
    }

    private suspend fun getFullProfile(): User? {
        val userId = auth.currentUserOrNull()?.id ?: return null
        return try {
            postgrest.from("users")
                .select(columns = Columns.ALL) { filter { eq("id", userId) } }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            null
        }
    }

    override fun getTasks(): Flow<Result<List<Task>>> = _tasksCache.asStateFlow()

    override fun getEmployees(): Flow<Result<List<User>>> = _employeesCache.asStateFlow()

    override fun getSites(): Flow<Result<List<OrganizationSite>>> = _sitesCache.asStateFlow()

    override fun loadTasks() {
        refreshBus.trigger()
    }

    override fun getTaskById(id: String): Flow<Result<Task>> = flow {
        emit(Result.Loading)
        try {
            val task = postgrest.from("tasks")
                .select(columns = Columns.ALL) { filter { eq("id", id) } }
                .decodeSingleOrNull<Task>()
            if (task != null) emit(Result.Success(task)) else emit(Result.Error(message = "Task not found."))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override suspend fun createTask(task: Task): Result<Unit> {
        return try {
            val profile = getFullProfile() ?: return Result.Error(message = "Session expired")
            val taskToInsert = task.copy(organizationId = profile.organizationId, createdBy = profile.id, status = TaskStatus.PENDING)
            postgrest.from("tasks").insert(taskToInsert)
            refreshBus.trigger()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit> {
        return try {
            val profile = getFullProfile() ?: return Result.Error(message = "Unauthorized")
            val updateData = buildJsonObject {
                put("status", status.name)
                if (status == TaskStatus.COMPLETED) {
                    put("completed_at", Instant.now().toString())
                }
                if (status == TaskStatus.APPROVED) {
                    put("approved_at", Instant.now().toString())
                    put("approved_by", profile.id)
                }
            }
            postgrest.from("tasks").update(updateData) { filter { eq("id", taskId) } }
            refreshBus.trigger()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            postgrest.from("tasks").delete { filter { eq("id", taskId) } }
            refreshBus.trigger()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun checkIn(taskId: String, latitude: Double, longitude: Double, imagePath: String?): Result<Unit> {
        return try {
            val profile = getFullProfile() ?: return Result.Error(message = "Unauthorized")
            val record = AttendanceEntity(
                clientSideId = UUID.randomUUID().toString(), taskId = taskId, organizationId = profile.organizationId,
                employeeId = profile.id, type = "CHECK_IN", latitude = latitude, longitude = longitude,
                timestamp = Instant.now().toString(), localImagePath = imagePath ?: "", status = "PENDING"
            )
            val localId = attendanceDao.insertAttendance(record)
            val syncResult = syncRecord(record.copy(localId = localId.toInt()))
            if (syncResult is Result.Error) { syncScheduler.scheduleAttendanceSync(); syncResult } else { refreshBus.trigger(); Result.Success(Unit) }
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Check-in failed locally.")
        }
    }

    override suspend fun checkOut(taskId: String, latitude: Double, longitude: Double, imagePath: String?): Result<Unit> {
        return try {
            val profile = getFullProfile() ?: return Result.Error(message = "Unauthorized")
            val checkInRecord = attendanceDao.getLatestCheckInRecord(taskId)
            val clientSideId = checkInRecord?.clientSideId ?: UUID.randomUUID().toString()
            val integrityDist = checkInRecord?.let { LocationUtils.calculateDistance(it.latitude, it.longitude, latitude, longitude) }
            val record = AttendanceEntity(
                clientSideId = clientSideId, taskId = taskId, organizationId = profile.organizationId,
                employeeId = profile.id, type = "CHECK_OUT", latitude = latitude, longitude = longitude,
                integrityDistance = integrityDist, timestamp = Instant.now().toString(), localImagePath = imagePath ?: "", status = "PENDING"
            )
            val localId = attendanceDao.insertAttendance(record)
            val syncResult = syncRecord(record.copy(localId = localId.toInt()))
            if (syncResult is Result.Error) { syncScheduler.scheduleAttendanceSync(); syncResult } else { refreshBus.trigger(); Result.Success(Unit) }
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Check-out failed locally.")
        }
    }

    override suspend fun syncRecord(record: AttendanceEntity, shouldTriggerRefresh: Boolean): Result<Unit> {
        return try {
            attendanceDao.updateStatus(record.localId, "UPLOADING")
            var imageUrl: String? = null
            if (record.localImagePath.isNotEmpty()) {
                val originalFile = File(record.localImagePath)
                if (originalFile.exists()) {
                    val compressedPath = ImageUtils.processAndCompressImage(
                        context, 
                        record.localImagePath,
                        record.latitude,
                        record.longitude,
                        record.type
                    )
                    val fileToUpload = File(compressedPath)
                    val fileName = "attendance/${record.organizationId}/${record.taskId}_${record.employeeId}_${record.type.lowercase()}.jpg"
                    storage.from("attendance-proof").upload(fileName, fileToUpload.readBytes()) { upsert = true }
                    imageUrl = storage.from("attendance-proof").publicUrl(fileName)
                    if (compressedPath != record.localImagePath) fileToUpload.delete()
                }
            }
            if (record.type == "CHECK_IN") {
                val data = buildJsonObject {
                    put("client_side_id", record.clientSideId); put("organization_id", record.organizationId)
                    put("task_id", record.taskId); put("employee_id", record.employeeId); put("check_in_time", record.timestamp)
                    put("check_in_lat", record.latitude); put("check_in_lng", record.longitude); put("check_in_image_url", imageUrl)
                    put("status", "CHECKED_IN")
                }
                postgrest.from("attendance_logs").upsert(data) { onConflict = "client_side_id" }
                postgrest.from("tasks").update(buildJsonObject { put("status", TaskStatus.IN_PROGRESS.name) }) {
                    filter { eq("id", record.taskId); eq("status", TaskStatus.PENDING.name) }
                }
            } else {
                val data = buildJsonObject {
                    put("check_out_time", record.timestamp)
                    put("check_out_lat", record.latitude); put("check_out_lng", record.longitude)
                    put("check_out_image_url", imageUrl); put("integrity_distance", record.integrityDistance); put("status", "CHECKED_OUT")
                }
                // USE UPDATE INSTEAD OF UPSERT to avoid wiping out Check-In data
                postgrest.from("attendance_logs").update(data) {
                    filter { eq("client_side_id", record.clientSideId) }
                }
                postgrest.from("tasks").update(buildJsonObject { put("status", TaskStatus.COMPLETED.name); put("completed_at", record.timestamp) }) {
                    filter { eq("id", record.taskId) }
                }
            }
            attendanceDao.updateStatus(record.localId, "SYNCED")
            if (shouldTriggerRefresh) refreshBus.trigger()
            Result.Success(Unit)
        } catch (e: Exception) {
            attendanceDao.updateStatus(record.localId, "PENDING")
            Result.Error(e, e.message ?: "Sync failed")
        }
    }

    override fun getPendingSyncCount(): Flow<Int> = attendanceDao.getPendingCountFlow()

    override fun getTaskAttendance(taskId: String): Flow<Result<TaskAttendance?>> = flow {
        emit(Result.Loading)
        val profile = getFullProfile() ?: return@flow
        try {
            val attendance = postgrest.from("attendance_logs").select(columns = Columns.ALL) {
                filter { eq("task_id", taskId); if (profile.role == UserRole.EMPLOYEE) eq("employee_id", profile.id) }
                order("check_in_time", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }.decodeSingleOrNull<TaskAttendance>()
            emit(Result.Success(attendance))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override suspend fun createSite(name: String, latitude: Double, longitude: Double): Result<Unit> {
        return try {
            val profile = getFullProfile() ?: return Result.Error(message = "Unauthorized")
            val site = OrganizationSite(organizationId = profile.organizationId, name = name, latitude = latitude, longitude = longitude)
            postgrest.from("organization_sites").insert(site)
            refreshBus.trigger()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
