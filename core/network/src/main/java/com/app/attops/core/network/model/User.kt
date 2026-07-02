package com.app.attops.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id") val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("employee_id") val employeeId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String? = null,
    @SerialName("role") val role: UserRole,
    @SerialName("created_at") val createdAt: String? = null
)
