package com.app.attops.features.auth.repository

import android.util.Log
import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.EdgeFunctionResponse
import com.app.attops.core.network.model.Organization
import com.app.attops.core.network.model.User
import com.app.attops.core.network.model.UserRole
import com.app.attops.core.network.model.UserStatus
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val supabaseAuth: Auth,
    private val postgrest: Postgrest,
    private val functions: Functions
) : AuthRepository {

    private val tag = "AUTH_REPO"

    override fun getCurrentUser(): Flow<User?> = flow {
        supabaseAuth.sessionStatus.collect { status ->
            if (status is SessionStatus.Authenticated) {
                val authUser = status.session.user
                val userId = authUser?.id ?: ""
                try {
                    val user = postgrest.from("users")
                        .select(columns = Columns.ALL) {
                            filter { eq("id", userId) }
                        }
                        .decodeSingleOrNull<User>()
                    
                    if (user != null) {
                        emit(user)
                    } else if (authUser != null) {
                        emit(createPlaceholderUser(authUser))
                    } else {
                        emit(null)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error fetching current user profile", e)
                    emit(null)
                }
            } else {
                emit(null)
            }
        }
    }

    override suspend fun signInWithGoogle(idToken: String, nonce: String?): Result<User> {
        return try {
            Log.d(tag, "Attempting Supabase signInWith(IDToken)...")
            supabaseAuth.signInWith(IDToken) {
                this.idToken = idToken
                this.nonce = nonce
                this.provider = Google
            }

            val authUser = supabaseAuth.currentUserOrNull() ?: return Result.Error(message = "Supabase session creation failed")
            Log.i(tag, "Supabase Auth Successful for UID: ${authUser.id}")
            
            val existingUser = postgrest.from("users")
                .select(columns = Columns.ALL) {
                    filter { eq("id", authUser.id) }
                }
                .decodeSingleOrNull<User>()

            if (existingUser != null) {
                Log.d(tag, "Existing profile found for user.")
                Result.Success(existingUser)
            } else {
                Log.d(tag, "No profile found. Directing to Org Creation.")
                Result.Success(createPlaceholderUser(authUser))
            }
        } catch (e: Exception) {
            Log.e(tag, "Google-Supabase Auth Bridge Failed", e)
            Result.Error(e, "Server Auth Failed: ${e.localizedMessage}")
        }
    }

    private fun createPlaceholderUser(authUser: io.github.jan.supabase.auth.user.UserInfo): User {
        return User(
            id = authUser.id,
            organizationId = "",
            name = authUser.userMetadata?.get("full_name")?.toString() ?: "Owner",
            email = authUser.email,
            role = UserRole.OWNER,
            status = UserStatus.ACTIVE
        )
    }

    override suspend fun loginWithEmployeeId(
        orgCode: String,
        employeeId: String,
        password: String
    ): Result<User> {
        return try {
            val deterministicEmail = "${employeeId.lowercase().trim()}@${orgCode.lowercase().trim()}.attops.com"
            
            supabaseAuth.signInWith(Email) {
                this.email = deterministicEmail
                this.password = password
            }

            val authUser = supabaseAuth.currentUserOrNull() ?: return Result.Error(message = "Login failed. Please check your credentials.")
            
            val user = postgrest.from("users")
                .select(columns = Columns.ALL) {
                    filter { eq("id", authUser.id) }
                }
                .decodeSingleOrNull<User>() ?: return Result.Error(message = "Your profile was not found. Please contact your administrator.")

            Result.Success(user)
        } catch (e: Exception) {
            Log.e(tag, "Employee Login Failed", e)
            val friendlyMessage = when {
                e.message?.contains("invalid", ignoreCase = true) == true -> "Invalid Employee ID or password."
                e.message?.contains("network", ignoreCase = true) == true -> "Network error. Please check your connection."
                else -> "Unable to login. Please verify your organization code and credentials."
            }
            Result.Error(e, friendlyMessage)
        }
    }

    override suspend fun createOrganization(
        name: String,
        businessType: String,
        address: String
    ): Result<Organization> {
        return try {
            val authUser = supabaseAuth.currentUserOrNull() ?: return Result.Error(message = "Session Expired")
            
            Log.d(tag, "Provisioning organization via Edge Function: $name")
            
            val params = mapOf(
                "name" to name.trim(),
                "business_type" to businessType.trim(),
                "address" to address.trim()
            )

            val response = try {
                functions.invoke("create-organization", params)
            } catch (invokeError: Exception) {
                Log.e(tag, "Invoke call failed", invokeError)
                return Result.Error(invokeError, "Backend unreachable: ${invokeError.localizedMessage}")
            }

            val result = try {
                response.body<EdgeFunctionResponse>()
            } catch (parseError: Exception) {
                Log.e(tag, "Response parsing failed", parseError)
                return Result.Error(parseError, "Data error: Invalid server response")
            }

            if (result.success && result.orgCode != null) {
                // Return a lightweight organization object for navigation
                Result.Success(
                    Organization(
                        id = "", // Not needed for immediate success screen navigation
                        name = name.trim(),
                        businessType = businessType.trim(),
                        orgCode = result.orgCode!!,
                        ownerId = authUser.id,
                        address = address.trim()
                    )
                )
            } else {
                val errorMsg = result.message ?: result.error ?: "Creation failed"
                Log.e(tag, "Edge Function Logic Error: $errorMsg")
                Result.Error(message = errorMsg)
            }
        } catch (e: Exception) {
            Log.e(tag, "Critical Organization Creation Failure", e)
            Result.Error(e, "Unexpected error: ${e.localizedMessage}")
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            supabaseAuth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
