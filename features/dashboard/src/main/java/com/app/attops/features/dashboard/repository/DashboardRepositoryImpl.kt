package com.app.attops.features.dashboard.repository

import android.util.Log
import com.app.attops.core.common.result.Result
import com.app.attops.core.common.util.RefreshBus
import com.app.attops.core.common.util.SessionBus
import com.app.attops.core.network.model.Organization
import com.app.attops.core.network.model.User
import com.app.attops.core.network.model.UserRole
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DashboardRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val refreshBus: RefreshBus,
    private val sessionBus: SessionBus
) : DashboardRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Modern Standard: Memory Caching with immediate emission
    private val _currentUserCache = MutableStateFlow<User?>(null)
    private val _orgDetailsCache = MutableStateFlow<Result<Organization>?>(null)
    private val _empCountCache = MutableStateFlow<Result<Int>>(Result.Success(0))
    private val _adminCountCache = MutableStateFlow<Result<Int>>(Result.Success(0))
    private val _taskStatsCache = MutableStateFlow<Result<Map<String, Int>>>(Result.Success(emptyMap()))

    init {
        // PRODUCTION-GRADE SECURITY: 
        // We listen directly to the Auth Session. The moment it drops or changes, 
        // we wipe EVERYTHING in memory before the UI can even recompose.
        scope.launch {
            auth.sessionStatus.collect { status ->
                if (status !is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
                    clearCache()
                } else {
                    refreshAll()
                }
            }
        }

        scope.launch {
            refreshBus.refreshEvent.collect {
                refreshAll()
            }
        }
    }

    private fun clearCache() {
        Log.d("DashboardRepo", "Auth session changed. Purging dashboard cache.")
        _currentUserCache.value = null
        _orgDetailsCache.value = null
        _empCountCache.value = Result.Success(0)
        _adminCountCache.value = Result.Success(0)
        _taskStatsCache.value = Result.Success(emptyMap())
    }

    private suspend fun refreshAll() {
        val userId = auth.currentUserOrNull()?.id ?: return
        val profile = getFullProfile(userId)
        _currentUserCache.value = profile
        
        val orgId = profile?.organizationId
        if (!orgId.isNullOrEmpty()) {
            fetchOrgDetails(orgId)
            fetchCountsAndStats(orgId)
        }
    }

    private suspend fun fetchOrgDetails(orgId: String) {
        try {
            val org = postgrest.from("organizations")
                .select(columns = Columns.ALL) { filter { eq("id", orgId) } }
                .decodeSingleOrNull<Organization>()
            _orgDetailsCache.value = if (org != null) Result.Success(org) else Result.Error(message = "Organization not found")
        } catch (e: Exception) {
            if (_orgDetailsCache.value !is Result.Success) {
                _orgDetailsCache.value = Result.Error(e)
            }
        }
    }

    private suspend fun fetchCountsAndStats(orgId: String) {
        try {
            val profile = _currentUserCache.value
            val usersResponse = postgrest.from("users")
                .select(columns = Columns.list("id", "role")) { filter { eq("organization_id", orgId) } }
            val users = usersResponse.decodeList<Map<String, String>>()
            _empCountCache.value = Result.Success(users.count { it["role"] == "EMPLOYEE" })
            _adminCountCache.value = Result.Success(users.count { it["role"] == "ADMIN" })

            val tasksResponse = postgrest.from("tasks")
                .select(columns = Columns.ALL) { 
                    filter { 
                        eq("organization_id", orgId)
                        if (profile?.role == UserRole.EMPLOYEE) {
                            eq("assigned_to", profile.id)
                        }
                    } 
                }
            val tasks = tasksResponse.decodeList<com.app.attops.core.network.model.Task>()
            
            val stats = if (profile?.role == UserRole.EMPLOYEE) {
                mapOf(
                    "TOTAL" to tasks.size,
                    "PENDING" to tasks.count { it.status == com.app.attops.core.network.model.TaskStatus.PENDING },
                    "IN_PROGRESS" to tasks.count { it.status == com.app.attops.core.network.model.TaskStatus.IN_PROGRESS },
                    "IN_REVIEW" to tasks.count { it.status == com.app.attops.core.network.model.TaskStatus.COMPLETED },
                    "DONE" to tasks.count { it.status == com.app.attops.core.network.model.TaskStatus.APPROVED }
                )
            } else {
                mapOf(
                    "TOTAL" to tasks.size,
                    "PENDING" to tasks.count { it.status == com.app.attops.core.network.model.TaskStatus.PENDING },
                    "FOR_REVIEW" to tasks.count { it.status == com.app.attops.core.network.model.TaskStatus.COMPLETED },
                    "DONE" to tasks.count { it.status == com.app.attops.core.network.model.TaskStatus.APPROVED }
                )
            }
            _taskStatsCache.value = Result.Success(stats)
        } catch (e: Exception) {
            Log.e("DashboardRepo", "Refresh failed", e)
        }
    }

    private suspend fun getFullProfile(userId: String): User? {
        return try {
            postgrest.from("users")
                .select(columns = Columns.ALL) { filter { eq("id", userId) } }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            null
        }
    }

    override fun getCurrentUser(): Flow<User?> = _currentUserCache.asStateFlow()

    override fun getOrganizationDetails(): Flow<Result<Organization>> = _orgDetailsCache.map { 
        it ?: Result.Loading 
    }

    override fun getEmployeeCount(): Flow<Result<Int>> = _empCountCache.asStateFlow()

    override fun getAdminCount(): Flow<Result<Int>> = _adminCountCache.asStateFlow()

    override fun getTaskStats(): Flow<Result<Map<String, Int>>> = _taskStatsCache.asStateFlow()

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            sessionBus.triggerSignOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
