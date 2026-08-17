package com.app.attops.core.notifications.domain

import com.app.attops.core.notifications.repository.NotificationRepository
import javax.inject.Inject

class GetNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    operator fun invoke() = repository.getNotifications()
}
