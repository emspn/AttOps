package com.app.attops.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    @SerialName("HEAD") OWNER,
    @SerialName("ADMIN") ADMIN,
    @SerialName("EMPLOYEE") EMPLOYEE
}
