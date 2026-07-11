package com.app.attops.features.reports.data.repository

import android.util.Log
import com.app.attops.core.common.result.Result
import com.app.attops.core.common.util.RefreshBus
import com.app.attops.core.common.util.SessionBus
import com.app.attops.core.network.model.TaskAttendance
import com.app.attops.core.network.model.User
import com.app.attops.core.network.model.UserRole
import com.app.attops.features.reports.domain.model.IntegrityScorecard
import com.app.attops.features.reports.domain.repository.ReportFilter
import com.app.attops.features.reports.domain.repository.ReportRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReportRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val refreshBus: RefreshBus,
    private val sessionBus: SessionBus
) : ReportRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _scorecardCache = MutableStateFlow<Map<ReportFilter, Result<List<IntegrityScorecard>>>>(emptyMap())

    init {
        // PRODUCTION-GRADE SECURITY: Reactive Session Purging
        scope.launch {
            auth.sessionStatus.collect { status ->
                if (status !is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
                    _scorecardCache.value = emptyMap()
                }
            }
        }
    }

    private suspend fun getFullProfile(): User? {
        val userId = auth.currentUserOrNull()?.id ?: return null
        return try {
            postgrest.from("users").select(columns = Columns.ALL) { filter { eq("id", userId) } }.decodeSingleOrNull<User>()
        } catch (e: Exception) {
            null
        }
    }

    override fun getIntegrityScorecards(filter: ReportFilter): Flow<Result<List<IntegrityScorecard>>> = flow {
        // Instant UI: Emit cached value if exists
        val cached = _scorecardCache.value[filter]
        if (cached != null) emit(cached) else emit(Result.Loading)

        try {
            val profile = getFullProfile() ?: throw Exception("Unauthorized")
            val orgId = profile.organizationId
            if (profile.role == UserRole.EMPLOYEE) throw Exception("Access Denied")

            val allEmployees = postgrest.from("users").select(columns = Columns.ALL) {
                filter { eq("organization_id", orgId); eq("role", "EMPLOYEE") }
            }.decodeList<User>()

            val now = LocalDateTime.now()
            val startTime = when (filter) {
                ReportFilter.LAST_WEEK -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).withHour(0).withMinute(0)
                ReportFilter.LAST_MONTH -> now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0)
                ReportFilter.LAST_6_MONTHS -> now.minusMonths(6).withDayOfMonth(1).withHour(0).withMinute(0)
                ReportFilter.LAST_YEAR -> now.minusYears(1).withDayOfYear(1).withHour(0).withMinute(0)
                ReportFilter.ALL_TIME -> null
            }

            val attendances = postgrest.from("attendance_logs").select(columns = Columns.ALL) {
                filter { eq("organization_id", orgId); if (startTime != null) gte("check_in_time", startTime.toString()) }
            }.decodeList<TaskAttendance>()

            val scorecards = allEmployees.map { employee ->
                val empAttendances = attendances.filter { it.employeeId == employee.id }
                val completed = empAttendances.filter { it.status == "CHECKED_OUT" || it.checkOutTime != null }
                val avgDistance = if (completed.isNotEmpty()) completed.mapNotNull { it.integrityDistance }.average() else 0.0
                val flags = completed.count { (it.integrityDistance ?: 0.0) > 200.0 }

                IntegrityScorecard(
                    employeeId = employee.id, fullName = employee.name, totalTasks = empAttendances.size,
                    completedTasks = completed.size, averageIntegrityDistance = avgDistance,
                    flagCount = flags, attendanceRate = if (empAttendances.isNotEmpty()) completed.size.toFloat() / empAttendances.size else 0f
                )
            }.sortedByDescending { it.attendanceRate }

            val result = Result.Success(scorecards)
            _scorecardCache.update { it + (filter to result) }
            emit(result)
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun getEmployeeTimesheet(employeeId: String, month: Int, year: Int): Flow<Result<List<TaskAttendance>>> = flow {
        emit(Result.Loading)
        try {
            val prefix = "${year}-${month.toString().padStart(2, '0')}"
            val attendances = postgrest.from("attendance_logs").select(columns = Columns.ALL) {
                filter { eq("employee_id", employeeId) }
                order("check_in_time", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<TaskAttendance>().filter { it.checkInTime?.startsWith(prefix) == true }
            emit(Result.Success(attendances))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }
}
