package com.app.attops.features.employee.usecase

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.User
import com.app.attops.features.employee.repository.EmployeeRepository
import javax.inject.Inject

class UpdateEmployeeUseCase @Inject constructor(
    private val repository: EmployeeRepository
) {
    suspend operator fun invoke(employee: User): Result<Unit> {
        if (employee.name.isBlank()) {
            return Result.Error(message = "Name is required")
        }
        return repository.updateEmployee(employee)
    }
}
