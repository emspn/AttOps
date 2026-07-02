package com.app.attops.features.auth.usecase

import com.app.attops.core.common.result.Result
import com.app.attops.features.auth.repository.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.signOut()
    }
}
