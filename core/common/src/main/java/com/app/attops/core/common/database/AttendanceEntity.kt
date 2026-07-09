package com.app.attops.core.common.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_queue")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    val clientSideId: String, // Shared UUID between in/out
    val taskId: String,
    val organizationId: String,
    val employeeId: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
    val localImagePath: String,
    val status: String = "PENDING"
)
