package com.app.attops.features.employee.usecase

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.User
import com.app.attops.features.employee.repository.EmployeeRepository
import javax.inject.Inject

class AddEmployeeUseCase @Inject constructor(
    private val repository: EmployeeRepository
) {
    suspend operator fun invoke(employee: User, temporaryPassword: String): Result<Unit> {
        if (employee.name.isBlank() || employee.employeeId.isNullOrBlank() || temporaryPassword.isBlank()) {
            return Result.Error(message = "Name, Employee ID and Password are required")
        }
        return repository.addEmployee(employee, temporaryPassword)
    }
}
