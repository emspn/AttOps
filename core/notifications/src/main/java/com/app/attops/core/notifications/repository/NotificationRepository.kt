package com.app.attops.core.notifications.repository

import com.app.attops.core.common.result.Result
import com.app.attops.core.notifications.model.NotificationLog
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun uploadToken(token: String): Result<Unit>
    suspend fun deleteToken(token: String): Result<Unit>
    fun getNotifications(): Flow<Result<List<NotificationLog>>>
    suspend fun markAsRead(notificationId: String): Result<Unit>
}
