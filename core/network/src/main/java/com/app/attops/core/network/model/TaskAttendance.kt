package com.app.attops.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class TaskAttendance(
    @SerialName("id") val id: String? = null,
    @SerialName("client_side_id") val clientSideId: String? = null,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("employee_id") val employeeId: String,
    @SerialName("check_in_time") val checkInTime: String? = null,
    @SerialName("check_in_lat") val checkInLat: Double? = null,
    @SerialName("check_in_lng") val checkInLng: Double? = null,
    @SerialName("check_out_time") val checkOutTime: String? = null,
    @SerialName("check_out_lat") val checkOutLat: Double? = null,
    @SerialName("check_out_lng") val checkOutLng: Double? = null,
    @SerialName("integrity_distance") val integrityDistance: Double? = null,
    @SerialName("check_in_image_url") val checkInImageUrl: String? = null,
    @SerialName("check_out_image_url") val checkOutImageUrl: String? = null,
    @SerialName("status") val status: String
)
