package com.app.attops.features.employee.usecase

import com.app.attops.core.common.result.Result
import com.app.attops.features.employee.repository.EmployeeRepository
import javax.inject.Inject

class DeleteEmployeeUseCase @Inject constructor(
    private val repository: EmployeeRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return repository.deleteEmployee(id)
    }
}
