package com.app.attops.features.auth.usecase

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.Organization
import com.app.attops.features.auth.repository.AuthRepository
import javax.inject.Inject

class CreateOrganizationUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        businessType: String,
        address: String
    ): Result<Organization> {
        if (name.isBlank() || businessType.isBlank() || address.isBlank()) {
            return Result.Error(message = "Required fields are missing")
        }
        return repository.createOrganization(name, businessType, address)
    }
}
