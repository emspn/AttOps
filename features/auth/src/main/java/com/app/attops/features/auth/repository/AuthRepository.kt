package com.app.attops.features.auth.repository

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.User
import com.app.attops.core.network.model.Organization
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): Flow<User?>
    
    suspend fun signInWithGoogle(idToken: String): Result<User>
    
    suspend fun loginWithEmployeeId(
        orgCode: String,
        employeeId: String,
        password: String
    ): Result<User>
    
    suspend fun createOrganization(
        name: String,
        businessType: String,
        phone: String,
        address: String?
    ): Result<Organization>
    
    suspend fun signOut(): Result<Unit>
}
