package com.app.attops.features.auth.repository

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.Organization
import com.app.attops.core.network.model.User
import com.app.attops.core.network.model.UserRole
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.random.Random
import java.util.UUID

class AuthRepositoryImpl @Inject constructor(
    private val supabaseAuth: Auth,
    private val postgrest: Postgrest
) : AuthRepository {

    override fun getCurrentUser(): Flow<User?> = flow {
        supabaseAuth.sessionStatus.collect { status ->
            if (status is SessionStatus.Authenticated) {
                val userId = status.session.user?.id ?: ""
                try {
                    val user = postgrest.from("users")
                        .select(columns = Columns.ALL) {
                            filter {
                                eq("id", userId)
                            }
                        }
                        .decodeSingleOrNull<User>()
                    emit(user)
                } catch (e: Exception) {
                    emit(null)
                }
            } else {
                emit(null)
            }
        }
    }

    override suspend fun signInWithGoogle(): Result<User> {
        return try {
            val authUser = supabaseAuth.currentUserOrNull() ?: return Result.Error(message = "Google Sign-In cancelled")
            
            val existingUser = postgrest.from("users")
                .select(columns = Columns.ALL) {
                    filter { eq("id", authUser.id) }
                }
                .decodeSingleOrNull<User>()

            if (existingUser != null) {
                Result.Success(existingUser)
            } else {
                Result.Success(User(id = authUser.id, organizationId = "", name = authUser.userMetadata?.get("full_name")?.toString() ?: "Head", role = UserRole.HEAD))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun loginWithEmployeeId(
        orgCode: String,
        employeeId: String,
        password: String
    ): Result<User> {
        return try {
            val organization = postgrest.from("organizations")
                .select(columns = Columns.ALL) {
                    filter { eq("org_code", orgCode) }
                }
                .decodeSingleOrNull<Organization>() ?: return Result.Error(message = "Invalid Organization Code")

            val user = postgrest.from("users")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("organization_id", organization.id)
                        eq("employee_id", employeeId)
                        eq("password_hash", password)
                    }
                }
                .decodeSingleOrNull<User>() ?: return Result.Error(message = "Invalid Credentials")

            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createOrganization(
        name: String,
        businessType: String,
        phone: String,
        address: String?
    ): Result<Organization> {
        return try {
            val authUser = supabaseAuth.currentUserOrNull() ?: return Result.Error(message = "Session Expired")
            
            val orgCode = generateOrgCode()
            val newOrg = Organization(
                id = UUID.randomUUID().toString(),
                name = name,
                businessType = businessType,
                orgCode = orgCode,
                ownerId = authUser.id,
                phone = phone,
                address = address
            )

            postgrest.from("organizations").insert(newOrg)
            
            val ownerUser = User(
                id = authUser.id,
                organizationId = newOrg.id,
                name = authUser.userMetadata?.get("full_name")?.toString() ?: "Head",
                email = authUser.email,
                role = UserRole.HEAD
            )
            postgrest.from("users").insert(ownerUser)

            Result.Success(newOrg)
        } catch (e: Exception) {
            Result.Error(e)
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

    private fun generateOrgCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return "ATT-" + (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
