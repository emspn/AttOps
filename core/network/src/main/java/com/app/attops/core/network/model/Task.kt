package com.app.attops.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class Task(
    @SerialName("id") val id: String? = null,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("created_by") val createdBy: String,
    @SerialName("assigned_to") val assignedTo: String? = null,
    @SerialName("priority") val priority: TaskPriority = TaskPriority.MEDIUM,
    @SerialName("status") val status: TaskStatus = TaskStatus.PENDING,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
