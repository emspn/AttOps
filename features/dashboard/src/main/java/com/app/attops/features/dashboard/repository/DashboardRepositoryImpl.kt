package com.app.attops.features.dashboard.repository

import android.util.Log
import com.app.attops.core.common.result.Result
import com.app.attops.core.common.util.RefreshBus
import com.app.attops.core.network.model.Organization
import com.app.attops.core.network.model.User
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DashboardRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val refreshBus: RefreshBus
) : DashboardRepository {

    private suspend fun getFullProfile(userId: String): User? {
        return try {
            Log.d("DashboardRepo", "Fetching full profile for: $userId")
            postgrest.from("users")
                .select(columns = Columns.ALL) {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            Log.e("DashboardRepo", "Profile decode failed", e)
            null
        }
    }

    override fun getCurrentUser(): Flow<User?> = refreshBus.refreshEvent.flatMapLatest {
        flow {
            val userId = auth.currentUserOrNull()?.id
            if (userId != null) {
                emit(getFullProfile(userId))
            } else {
                emit(null)
            }
        }
    }

    override fun getOrganizationDetails(): Flow<Result<Organization>> = refreshBus.refreshEvent.flatMapLatest {
        flow {
            val userId = auth.currentUserOrNull()?.id
            if (userId == null) {
                emit(Result.Error(message = "Session expired"))
                return@flow
            }

            try {
                val profile = getFullProfile(userId)
                val orgId = profile?.organizationId
                
                if (!orgId.isNullOrEmpty()) {
                    val org = postgrest.from("organizations")
                        .select(columns = Columns.ALL) {
                            filter { eq("id", orgId) }
                        }
                        .decodeSingleOrNull<Organization>()
                    
                    if (org != null) {
                        emit(Result.Success(org))
                    } else {
                        emit(Result.Error(message = "Organization not found"))
                    }
                } else {
                    emit(Result.Error(message = "User not linked to an organization"))
                }
            } catch (e: Exception) {
                Log.e("DashboardRepo", "Org details failed", e)
                emit(Result.Error(e))
            }
        }
    }

    override fun getEmployeeCount(): Flow<Result<Int>> = refreshBus.refreshEvent.flatMapLatest {
        flow {
            val userId = auth.currentUserOrNull()?.id ?: return@flow
            try {
                val profile = getFullProfile(userId)
                val orgId = profile?.organizationId
                if (!orgId.isNullOrEmpty()) {
                    val response = postgrest.from("users")
                        .select(columns = Columns.list("id", "role")) {
                            filter { eq("organization_id", orgId) }
                        }
                    val users = response.decodeList<Map<String, String>>()
                    val count = users.count { it["role"] == "EMPLOYEE" }
                    emit(Result.Success(count))
                }
            } catch (e: Exception) {
                Log.e("DashboardRepo", "Emp count failed", e)
                emit(Result.Success(0))
            }
        }
    }

    override fun getAdminCount(): Flow<Result<Int>> = refreshBus.refreshEvent.flatMapLatest {
        flow {
            val userId = auth.currentUserOrNull()?.id ?: return@flow
            try {
                val profile = getFullProfile(userId)
                val orgId = profile?.organizationId
                if (!orgId.isNullOrEmpty()) {
                    val response = postgrest.from("users")
                        .select(columns = Columns.list("id", "role")) {
                            filter { eq("organization_id", orgId) }
                        }
                    val users = response.decodeList<Map<String, String>>()
                    val count = users.count { it["role"] == "ADMIN" }
                    emit(Result.Success(count))
                }
            } catch (e: Exception) {
                Log.e("DashboardRepo", "Admin count failed", e)
                emit(Result.Success(0))
            }
        }
    }

    override fun getTaskStats(): Flow<Result<Map<String, Int>>> = refreshBus.refreshEvent.flatMapLatest {
        flow {
            val userId = auth.currentUserOrNull()?.id ?: return@flow
            try {
                val profile = getFullProfile(userId)
                val orgId = profile?.organizationId
                if (!orgId.isNullOrEmpty()) {
                    val response = postgrest.from("tasks")
                        .select(columns = Columns.ALL) {
                            filter { eq("organization_id", orgId) }
                        }
                    val tasks = response.decodeList<com.app.attops.core.network.model.Task>()
                    
                    val stats = mapOf(
                        "TOTAL" to tasks.size,
                        "PENDING" to tasks.count { it.status == com.app.attops.core.network.model.TaskStatus.PENDING },
                        "IN_PROGRESS" to tasks.count { it.status == com.app.attops.core.network.model.TaskStatus.IN_PROGRESS },
                        "COMPLETED" to tasks.count { it.status == com.app.attops.core.network.model.TaskStatus.COMPLETED }
                    )
                    emit(Result.Success(stats))
                }
            } catch (e: Exception) {
                Log.e("DashboardRepo", "Task stats failed", e)
                emit(Result.Error(e))
            }
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
