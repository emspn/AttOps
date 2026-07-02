package com.app.attops.features.auth.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    HEAD, ADMIN, EMPLOYEE
}
