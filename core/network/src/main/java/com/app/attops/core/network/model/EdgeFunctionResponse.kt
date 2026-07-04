package com.app.attops.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EdgeFunctionResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("org_code") val orgCode: String? = null
)

@Serializable
data class CreateOrganizationRequest(
    @SerialName("name") val name: String,
    @SerialName("business_type") val businessType: String,
    @SerialName("address") val address: String
)

@Serializable
data class CreateEmployeeRequest(
    @SerialName("employee_id") val employeeId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("role") val role: String,
    @SerialName("password") val password: String,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("designation") val designation: String? = null
)
