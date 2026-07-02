package com.app.attops.features.auth.usecase

import com.app.attops.core.network.model.User
import com.app.attops.features.auth.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckSessionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Flow<User?> {
        return repository.getCurrentUser()
    }
}
