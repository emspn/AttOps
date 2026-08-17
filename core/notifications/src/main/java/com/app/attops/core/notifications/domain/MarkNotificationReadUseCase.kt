package com.app.attops.core.notifications.domain

import com.app.attops.core.notifications.repository.NotificationRepository
import javax.inject.Inject

class MarkNotificationReadUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(id: String) = repository.markAsRead(id)
}
