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
    val employeeCount: Int = 0,
    val adminCount: Int = 0,
    val taskStats: Map<String, Int> = emptyMap()
)

class GetDashboardDataUseCase @Inject constructor(
    private val repository: DashboardRepository,
) {
    operator fun invoke(): Flow<Result<DashboardData>> = combine(
        repository.getCurrentUser(),
        repository.getOrganizationDetails(),
        repository.getEmployeeCount(),
        repository.getAdminCount(),
        repository.getTaskStats(),
    ) { user, orgResult, countResult, adminResult, taskResult ->
        if (user == null) return@combine Result.Error(message = "Session expired. Please login again.")
        
        // Extract data safely, falling back to defaults if stats fail
        val org = (orgResult as? Result.Success)?.data
        val empCount = (countResult as? Result.Success)?.data ?: 0
        val adminCount = (adminResult as? Result.Success)?.data ?: 0
        val tasks = (taskResult as? Result.Success)?.data ?: emptyMap()

        if (org != null) {
            Result.Success(
                DashboardData(
                    user = user,
                    organization = org,
                    employeeCount = empCount,
                    adminCount = adminCount,
                    taskStats = tasks
                )
            )
        } else if (orgResult is Result.Error) {
            Result.Error(orgResult.exception, orgResult.message ?: "Failed to load organization")
        } else {
            Result.Loading
        }
    }
}
