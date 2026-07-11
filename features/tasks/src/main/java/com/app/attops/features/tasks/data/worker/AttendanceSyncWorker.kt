package com.app.attops.features.tasks.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import com.app.attops.core.common.database.AttendanceDao
import com.app.attops.features.tasks.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class AttendanceSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val attendanceDao: AttendanceDao,
    private val repository: TaskRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        val pending = attendanceDao.getPendingAttendance()
        if (pending.isEmpty()) return@withContext ListenableWorker.Result.success()

        Log.d("SyncWorker", "Starting sync for ${pending.size} records")

        var allSuccess = true
        var anySuccess = false
        pending.forEach { record ->
            try {
                val res = repository.syncRecord(record, shouldTriggerRefresh = false)
                if (res is com.app.attops.core.common.result.Result.Success) {
                    anySuccess = true
                } else {
                    allSuccess = false
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync record ${record.localId}", e)
                allSuccess = false
            }
        }

        if (anySuccess) {
            // Trigger refresh once for all successful syncs
            repository.loadTasks() // This triggers refreshBus
        }

        if (allSuccess) ListenableWorker.Result.success() else ListenableWorker.Result.retry()
    }
}
