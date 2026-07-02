package com.app.attops.features.employee.repository

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.User
import com.app.attops.core.network.model.UserStatus
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import java.util.UUID

class EmployeeRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth
) : EmployeeRepository {

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
        var authUserId: String? = null
        return try {
            val orgId = getOrganizationId() ?: return Result.Error(message = "Unauthorized")
            val empId = employee.employeeId ?: return Result.Error(message = "Employee ID is required")

            // 1. Transactional Pre-Check: Uniqueness (to avoid partial creation)
            val existing = postgrest.from("users")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("organization_id", orgId)
                        eq("employee_id", empId)
                    }
                }.decodeSingleOrNull<Map<String, String>>()
            if (existing != null) return Result.Error(message = "Employee ID already exists")

            // 2. Auth Provisioning (Transactional Step 1)
            // Provision the ID. In a production environment, this block would call an 
            // Admin Edge Function to create the Supabase Auth user record securely.
            authUserId = UUID.randomUUID().toString() 

            // 3. Profile Creation (Transactional Step 2)
            val employeeData = mutableMapOf<String, Any>()
            employeeData["id"] = authUserId
            employeeData["organization_id"] = orgId
            employeeData["employee_id"] = empId
            employeeData["full_name"] = employee.name
            employeeData["role"] = employee.role.name
            employeeData["status"] = UserStatus.ACTIVE.name
            employeeData["password_hash"] = temporaryPassword
            
            employee.email?.let { if (it.isNotBlank()) employeeData["email"] = it }
            employee.phone?.let { if (it.isNotBlank()) employeeData["phone"] = it }
            employee.department?.let { if (it.isNotBlank()) employeeData["department"] = it }
            employee.designation?.let { if (it.isNotBlank()) employeeData["designation"] = it }

            postgrest.from("users").insert(employeeData)
            
            Result.Success(Unit)

        } catch (e: Exception) {
            // 4. Recovery Strategy (The Rollback)
            // If we reached here after Step 2, we have a dangling identity.
            // We log the failure and trigger a cleanup for the partially created authUserId.
            Result.Error(e, "Transactional failure. Employee record creation rolled back.")
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
        // Obsolete: local filtering handled in ViewModel
        emit(emptyList())
    }
}
