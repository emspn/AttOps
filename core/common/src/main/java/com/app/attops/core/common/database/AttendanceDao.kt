package com.app.attops.core.common.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    @Query("SELECT * FROM attendance_queue WHERE status = 'PENDING' ORDER BY localId ASC")
    suspend fun getPendingAttendance(): List<AttendanceEntity>

    @Query("SELECT clientSideId FROM attendance_queue WHERE taskId = :taskId AND type = 'CHECK_IN' ORDER BY localId DESC LIMIT 1")
    suspend fun getLatestCheckInId(taskId: String): String?

    @Query("SELECT * FROM attendance_queue WHERE taskId = :taskId AND type = 'CHECK_IN' ORDER BY localId DESC LIMIT 1")
    suspend fun getLatestCheckInRecord(taskId: String): AttendanceEntity?

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    @Query("UPDATE attendance_queue SET status = :status WHERE localId = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("SELECT COUNT(*) FROM attendance_queue WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>
}
