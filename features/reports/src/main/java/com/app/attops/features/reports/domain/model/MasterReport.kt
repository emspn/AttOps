package com.app.attops.features.reports.domain.model

import com.app.attops.core.network.model.TaskPriority
import com.app.attops.core.network.model.TaskStatus

data class MasterReport(
    val taskId: String,
    val title: String,
    val description: String?,
    val assignedToName: String,
    val priority: TaskPriority,
    val status: TaskStatus,
    val deadline: String?,
    val checkInTime: String?,
    val checkInLat: Double?,
    val checkInLng: Double?,
    val checkOutTime: String?,
    val checkOutLat: Double?,
    val checkOutLng: Double?
)
