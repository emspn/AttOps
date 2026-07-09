package com.app.attops.core.common.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AttendanceEntity::class], version = 1, exportSchema = false)
abstract class AttOpsDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
}
