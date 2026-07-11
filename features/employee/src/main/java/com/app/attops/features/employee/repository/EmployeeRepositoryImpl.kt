package com.app.attops.features.employee.repository

import android.util.Log
import com.app.attops.core.common.result.Result
import com.app.attops.core.common.util.RefreshBus
import com.app.attops.core.common.util.SessionBus
import com.app.attops.core.network.model.*
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.call.body
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EmployeeRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val supabaseFunctions: Functions,
    private val refreshBus: RefreshBus,
    private val sessionBus: SessionBus
) : EmployeeRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _employeesCache = MutableStateFlow<List<User>>(emptyList())
    private val _currentUserCache = MutableStateFlow<User?>(null)

    init {
        // PRODUCTION-GRADE SECURITY: Reactive Session Purging
        scope.launch {
            auth.sessionStatus.collect { status ->
                if (status !is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
                    clearCache()
                } else {
                    refreshData()
                }
            }
        }

        scope.launch {
            refreshBus.refreshEvent.collect {
                refreshData()
            }
        }
    }

    private fun clearCache() {
        Log.d("EmployeeRepo", "Auth session changed. Purging employee cache.")
        _employeesCache.value = emptyList()
        _currentUserCache.value = null
    }

    private suspend fun refreshData() {
        val userId = (auth.sessionStatus.value as? SessionStatus.Authenticated)?.session?.user?.id ?: ""
        if (userId.isEmpty()) return
        
        val profile = getFullProfile(userId)
        _currentUserCache.value = profile
        
        val orgId = profile?.organizationId
        if (!orgId.isNullOrEmpty()) {
            try {
                // Explicitly naming columns for absolute mapping safety
                val employees = postgrest.from("users")
                    .select(columns = Columns.list(
                        "id", "organization_id", "employee_id", "full_name", 
                        "email", "phone", "department", "designation", 
                        "role", "status", "profile_photo", "login_password", "created_at"
                    )) {
                        filter { eq("organization_id", orgId); neq("id", userId) }
                    }
                    .decodeList<User>()

                val filtered = if (profile.role == UserRole.ADMIN) {
                    employees.filter { it.role == UserRole.EMPLOYEE }
                } else {
                    employees
                }
                _employeesCache.value = filtered
            } catch (e: Exception) {
                Log.e("EmployeeRepo", "Refresh failed", e)
            }
        }
    }

    private suspend fun getFullProfile(userId: String): User? {
        return try {
            postgrest.from("users").select(columns = Columns.ALL) { filter { eq("id", userId) } }.decodeSingleOrNull<User>()
        } catch (e: Exception) {
            null
        }
    }

    override fun getCurrentUser(): Flow<User?> = _currentUserCache.asStateFlow()

    override fun getEmployees(): Flow<List<User>> = _employeesCache.asStateFlow()

    override fun getEmployee(id: String): Flow<User?> = flow {
        // For individual employee, we fetch fresh but could also check cache
        val cached = _employeesCache.value.find { it.id == id }
        if (cached != null) emit(cached)
        
        val userId = auth.currentUserOrNull()?.id ?: ""
        val profile = if (userId.isNotEmpty()) getFullProfile(userId) else null
        val orgId = profile?.organizationId
        
        if (!orgId.isNullOrEmpty()) {
            try {
                val employee = postgrest.from("users")
                    .select(columns = Columns.list(
                        "id", "organization_id", "employee_id", "full_name", 
                        "email", "phone", "department", "designation", 
                        "role", "status", "profile_photo", "login_password", "created_at"
                    )) {
                        filter { eq("organization_id", orgId); eq("id", id) }
                    }.decodeSingleOrNull<User>()
                emit(employee)
            } catch (e: Exception) {
                emit(cached)
            }
        }
    }

    override suspend fun addEmployee(employee: User, temporaryPassword: String): Result<Unit> {
        return try {
            val requestBody = CreateEmployeeRequest(
                employeeId = employee.employeeId!!, fullName = employee.name, role = employee.role.name,
                password = temporaryPassword, email = employee.email?.ifBlank { null }, phone = employee.phone?.ifBlank { null },
                department = employee.department?.ifBlank { null }, designation = employee.designation?.ifBlank { null }
            )
            val response = supabaseFunctions.invoke("create-employee", requestBody)
            val result = response.body<EdgeFunctionResponse>()
            if (result.success) {
                refreshBus.trigger()
                Result.Success(Unit)
            } else {
                Result.Error(message = result.message ?: "Failed to add employee.")
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateEmployee(employee: User): Result<Unit> {
        return try {
            val profile = _currentUserCache.value ?: return Result.Error(message = "Unauthorized")
            postgrest.from("users").update(employee) { filter { eq("id", employee.id); eq("organization_id", profile.organizationId) } }
            refreshBus.trigger()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteEmployee(id: String): Result<Unit> {
        return try {
            val profile = _currentUserCache.value ?: return Result.Error(message = "Unauthorized")
            postgrest.from("users").update(buildJsonObject { put("status", UserStatus.INACTIVE.name) }) {
                filter { eq("id", id); eq("organization_id", profile.organizationId) }
            }
            refreshBus.trigger()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    @Deprecated("Use local filtering in ViewModel")
    override fun searchEmployees(query: String): Flow<List<User>> = flow { emit(emptyList()) }
}
