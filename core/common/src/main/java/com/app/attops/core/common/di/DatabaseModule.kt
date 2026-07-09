package com.app.attops.core.common.di

import android.content.Context
import androidx.room.Room
import com.app.attops.core.common.database.AttendanceDao
import com.app.attops.core.common.database.AttOpsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AttOpsDatabase {
        return Room.databaseBuilder(
            context,
            AttOpsDatabase::class.java,
            "attops_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideAttendanceDao(db: AttOpsDatabase): AttendanceDao {
        return db.attendanceDao()
    }
}
