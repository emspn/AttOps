package com.app.attops.features.employee.usecase

import com.app.attops.core.network.model.User
import com.app.attops.features.employee.repository.EmployeeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val repository: EmployeeRepository
) {
    operator fun invoke(): Flow<User?> = repository.getCurrentUser()
}
