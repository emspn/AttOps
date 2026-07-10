package com.app.attops.features.tasks.domain.repository

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.OrganizationSite
import com.app.attops.core.network.model.Task
import com.app.attops.core.network.model.TaskAttendance
import com.app.attops.core.network.model.TaskStatus
import com.app.attops.core.network.model.User
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasks(): Flow<Result<List<Task>>>
    fun getTaskById(id: String): Flow<Result<Task>>
    fun getEmployees(): Flow<Result<List<User>>>
    suspend fun createTask(task: Task): Result<Unit>
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit>
    suspend fun deleteTask(taskId: String): Result<Unit>
    
    suspend fun checkIn(
        taskId: String, 
        latitude: Double, 
        longitude: Double, 
        imagePath: String? = null
    ): Result<Unit>
    
    suspend fun checkOut(
        taskId: String, 
        latitude: Double, 
        longitude: Double,
        imagePath: String? = null
    ): Result<Unit>
    
    fun getTaskAttendance(taskId: String): Flow<Result<TaskAttendance?>>
    
    // Sync
    suspend fun syncRecord(record: com.app.attops.core.common.database.AttendanceEntity): Result<Unit>
    fun getPendingSyncCount(): Flow<Int>
    
    // Site Registry
    fun getSites(): Flow<Result<List<OrganizationSite>>>
    suspend fun createSite(name: String, latitude: Double, longitude: Double): Result<Unit>
}
