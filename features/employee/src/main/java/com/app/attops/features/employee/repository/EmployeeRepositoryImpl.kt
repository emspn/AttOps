package com.app.attops.features.employee.repository

import android.util.Log
import com.app.attops.core.common.result.Result
import com.app.attops.core.common.util.RefreshBus
import com.app.attops.core.network.model.CreateEmployeeRequest
import com.app.attops.core.network.model.EdgeFunctionResponse
import com.app.attops.core.network.model.User
import com.app.attops.core.network.model.UserRole
import com.app.attops.core.network.model.UserStatus
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EmployeeRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val supabaseFunctions: Functions,
    private val refreshBus: RefreshBus
) : EmployeeRepository {

    private suspend fun getFullProfile(userId: String): User? {
        return try {
            postgrest.from("users")
                .select(columns = Columns.ALL) {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            Log.e("EmployeeRepo", "Error getting full profile", e)
            null
        }
    }

    override fun getCurrentUser(): Flow<User?> = refreshBus.refreshEvent.flatMapLatest {
        flow {
            val sessionStatus = auth.sessionStatus.value
            if (sessionStatus is SessionStatus.Authenticated) {
                val userId = sessionStatus.session.user?.id ?: ""
                val user = getFullProfile(userId)
                emit(user)
            } else {
                emit(null)
            }
        }
    }

    override fun getEmployees(): Flow<List<User>> = refreshBus.refreshEvent.flatMapLatest {
        flow {
            val userId = auth.currentUserOrNull()?.id ?: ""
            if (userId.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            val profile = getFullProfile(userId)
            val orgId = profile?.organizationId
            
            if (!orgId.isNullOrEmpty()) {
                val role = profile.role
                
                val employees = postgrest.from("users")
                    .select(columns = Columns.ALL) {
                        filter { 
                            eq("organization_id", orgId)
                            neq("id", userId) // Don't show self
                        }
                    }
                    .decodeList<User>()

                // Filter by role locally to avoid PostgREST Enum mapping issues
                val filteredEmployees = if (role == UserRole.ADMIN) {
                    employees.filter { it.role == UserRole.EMPLOYEE }
                } else {
                    employees // Owner sees everyone
                }
                
                emit(filteredEmployees)
            } else {
                emit(emptyList())
            }
        }
    }

    override fun getEmployee(id: String): Flow<User?> = refreshBus.refreshEvent.flatMapLatest {
        flow {
            val userId = auth.currentUserOrNull()?.id ?: ""
            val profile = if (userId.isNotEmpty()) getFullProfile(userId) else null
            val orgId = profile?.organizationId
            
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
    }

    override suspend fun addEmployee(employee: User, temporaryPassword: String): Result<Unit> {
        return try {
            Log.d("EmployeeRepo", "Initiating secure employee provisioning for: ${employee.employeeId}")
            
            val requestBody = CreateEmployeeRequest(
                employeeId = employee.employeeId!!,
                fullName = employee.name,
                role = employee.role.name,
                password = temporaryPassword,
                email = employee.email?.ifBlank { null },
                phone = employee.phone?.ifBlank { null },
                department = employee.department?.ifBlank { null },
                designation = employee.designation?.ifBlank { null }
            )

            val response = try {
                supabaseFunctions.invoke("create-employee", requestBody)
            } catch (invokeError: Exception) {
                Log.e("EmployeeRepo", "Edge function invocation failed", invokeError)
                return Result.Error(invokeError, "Backend unreachable: ${invokeError.localizedMessage}")
            }

            val result = try {
                response.body<EdgeFunctionResponse>()
            } catch (parseError: Exception) {
                Log.e("EmployeeRepo", "Response parsing failed", parseError)
                return Result.Error(parseError, "Server data mismatch: Invalid response format.")
            }

            if (result.success) {
                Log.d("EmployeeRepo", "Provisioning SUCCESS for userId: ${result.userId}")
                refreshBus.trigger()
                Result.Success(Unit)
            } else {
                val errorMsg = result.message ?: "Failed to add employee. Please try again."
                Log.e("EmployeeRepo", "Provisioning FAILED: $errorMsg")
                Result.Error(message = errorMsg)
            }
        } catch (e: Exception) {
            Log.e("EmployeeRepo", "Unexpected error during provisioning", e)
            val friendlyMsg = when {
                e.message?.contains("timeout", ignoreCase = true) == true -> "Server timed out. Please check your internet."
                else -> "An unexpected error occurred while adding the employee."
            }
            Result.Error(e, friendlyMsg)
        }
    }

    override suspend fun updateEmployee(employee: User): Result<Unit> {
        return try {
            val userId = auth.currentUserOrNull()?.id ?: return Result.Error(message = "Unauthorized")
            val profile = getFullProfile(userId)
            val orgId = profile?.organizationId ?: return Result.Error(message = "Unauthorized")
            
            postgrest.from("users").update(employee) {
                filter {
                    eq("id", employee.id)
                    eq("organization_id", orgId)
                }
            }
            refreshBus.trigger()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteEmployee(id: String): Result<Unit> {
        return try {
            val userId = auth.currentUserOrNull()?.id ?: return Result.Error(message = "Unauthorized")
            val profile = getFullProfile(userId)
            val orgId = profile?.organizationId ?: return Result.Error(message = "Unauthorized")

            postgrest.from("users").update(buildJsonObject { put("status", UserStatus.INACTIVE.name) }) {
                filter {
                    eq("id", id)
                    eq("organization_id", orgId)
                }
            }
            refreshBus.trigger()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    @Deprecated("Use local filtering in ViewModel")
    override fun searchEmployees(query: String): Flow<List<User>> = flow {
        emit(emptyList())
    }
}
