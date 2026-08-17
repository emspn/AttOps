package com.app.attops.core.notifications.domain

import com.app.attops.core.notifications.repository.NotificationRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SyncFcmTokenUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            repository.uploadToken(token)
        } catch (e: Exception) {
            // Log or handle error
        }
    }
}
