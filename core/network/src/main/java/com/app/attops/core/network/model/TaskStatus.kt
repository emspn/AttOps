package com.app.attops.core.network.model

import kotlinx.serialization.Serializable

@Serializable
enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED
}
