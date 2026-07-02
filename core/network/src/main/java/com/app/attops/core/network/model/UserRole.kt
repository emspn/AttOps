package com.app.attops.core.network.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    HEAD, ADMIN, EMPLOYEE
}
