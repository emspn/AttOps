package com.app.attops.features.dashboard.usecase

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.Organization
import com.app.attops.core.network.model.User
import com.app.attops.features.dashboard.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class DashboardData(
    val user: User,
    val organization: Organization,
    val employeeCount: Int,
    val adminCount: Int,
)

class GetDashboardDataUseCase @Inject constructor(
    private val repository: DashboardRepository,
) {
    operator fun invoke(): Flow<Result<DashboardData>> = combine(
        repository.getCurrentUser(),
        repository.getOrganizationDetails(),
        repository.getEmployeeCount(),
        repository.getAdminCount(),
    ) { user, orgResult, countResult, adminResult ->
        if (user == null) return@combine Result.Error(message = "User profile not found. Please try logging in again.")
        
        when {
            (orgResult is Result.Success) && (countResult is Result.Success) && (adminResult is Result.Success) -> {
                Result.Success(
                    DashboardData(
                        user = user,
                        organization = orgResult.data,
                        employeeCount = countResult.data,
                        adminCount = adminResult.data,
                    )
                )
            }
            orgResult is Result.Error -> Result.Error(orgResult.exception, orgResult.message ?: "Failed to load organization details")
            countResult is Result.Error -> Result.Error(countResult.exception, countResult.message ?: "Failed to load employee count")
            adminResult is Result.Error -> Result.Error(adminResult.exception, adminResult.message ?: "Failed to load admin count")
            else -> Result.Loading
        }
    }
}
