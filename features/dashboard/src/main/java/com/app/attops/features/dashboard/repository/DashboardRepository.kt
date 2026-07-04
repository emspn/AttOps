package com.app.attops.features.dashboard.repository

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.Organization
import com.app.attops.core.network.model.User
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getOrganizationDetails(): Flow<Result<Organization>>
    fun getEmployeeCount(): Flow<Result<Int>>
    fun getAdminCount(): Flow<Result<Int>>
    fun getCurrentUser(): Flow<User?>
    suspend fun signOut(): Result<Unit>
}
