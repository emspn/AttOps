package com.app.attops.features.auth.usecase

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.User
import com.app.attops.features.auth.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(idToken: String, nonce: String? = null): Result<User> {
        return repository.signInWithGoogle(idToken, nonce)
    }
}
