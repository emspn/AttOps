package com.app.attops.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id") val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("employee_id") val employeeId: String? = null,
    @SerialName("full_name") val name: String,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("designation") val designation: String? = null,
    @SerialName("role") val role: UserRole,
    @SerialName("status") val status: UserStatus = UserStatus.ACTIVE,
    @SerialName("profile_photo") val profilePhoto: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
