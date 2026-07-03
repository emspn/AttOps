package com.app.attops.features.employee.repository

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.User
import com.app.attops.core.network.model.UserStatus
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import javax.inject.Inject

class EmployeeRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val functions: Functions
) : EmployeeRepository {

    @Serializable
    private data class EdgeFunctionResponse(
        val success: Boolean,
        val message: String? = null,
        val error: String? = null
    )

    private suspend fun getOrganizationId(): String? {
        val userId = auth.currentUserOrNull()?.id ?: return null
        return try {
            val userProfile = postgrest.from("users")
                .select(columns = Columns.list("organization_id")) {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<User>()
            userProfile?.organizationId
        } catch (e: Exception) {
            null
        }
    }

    override fun getCurrentUser(): Flow<User?> = flow {
        val sessionStatus = auth.sessionStatus.value
        if (sessionStatus is SessionStatus.Authenticated) {
            val userId = sessionStatus.session.user?.id ?: ""
            try {
                val user = postgrest.from("users")
                    .select(columns = Columns.ALL) {
                        filter { eq("id", userId) }
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

    override fun getEmployees(): Flow<List<User>> = flow {
        val orgId = getOrganizationId()
        if (!orgId.isNullOrEmpty()) {
            val employees = postgrest.from("users")
                .select(columns = Columns.ALL) {
                    filter { eq("organization_id", orgId) }
                }
                .decodeList<User>()
            emit(employees)
        } else {
            emit(emptyList())
        }
    }

    override fun getEmployee(id: String): Flow<User?> = flow {
        val orgId = getOrganizationId()
        if (!orgId.isNullOrEmpty()) {
            val employee = postgrest.from("users")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("organization_id", orgId)
                        eq("id", id)
                    }
                }
                .decodeSingleOrNull<User>()
            emit(employee)
        } else {
            emit(null)
        }
    }

    override suspend fun addEmployee(employee: User, temporaryPassword: String): Result<Unit> {
        return try {
            val params = mutableMapOf<String, Any?>(
                "employee_id" to employee.employeeId,
                "full_name" to employee.name,
                "role" to employee.role.name,
                "password" to temporaryPassword
            )
            
            employee.email?.let { if (it.isNotBlank()) params["email"] = it }
            employee.phone?.let { if (it.isNotBlank()) params["phone"] = it }
            employee.department?.let { if (it.isNotBlank()) params["department"] = it }
            employee.designation?.let { if (it.isNotBlank()) params["designation"] = it }

            val response = functions.invoke("create-employee", params)
            val result = response.body<EdgeFunctionResponse>()

            if (result.success) {
                Result.Success(Unit)
            } else {
                Result.Error(message = result.message ?: result.error ?: "Provisioning failed")
            }
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to provision employee via Edge Function")
        }
    }

    override suspend fun updateEmployee(employee: User): Result<Unit> {
        return try {
            val orgId = getOrganizationId() ?: return Result.Error(message = "Unauthorized")
            postgrest.from("users").update(employee) {
                filter {
                    eq("id", employee.id)
                    eq("organization_id", orgId)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteEmployee(id: String): Result<Unit> {
        return try {
            val orgId = getOrganizationId() ?: return Result.Error(message = "Unauthorized")
            // Soft delete (Preserves audit history)
            postgrest.from("users").update(mapOf("status" to UserStatus.INACTIVE.name)) {
                filter {
                    eq("id", id)
                    eq("organization_id", orgId)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun searchEmployees(query: String): Flow<List<User>> = flow {
        emit(emptyList())
    }
}
