package com.app.attops.features.auth.usecase

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.User
import com.app.attops.features.auth.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        orgCode: String,
        employeeId: String,
        password: String
    ): Result<User> {
        if (orgCode.isBlank() || employeeId.isBlank() || password.isBlank()) {
            return Result.Error(message = "All fields are required")
        }
        return repository.loginWithEmployeeId(orgCode, employeeId, password)
    }
}
