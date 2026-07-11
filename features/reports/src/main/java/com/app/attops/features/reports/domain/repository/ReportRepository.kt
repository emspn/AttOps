package com.app.attops.features.reports.domain.repository

import com.app.attops.core.common.result.Result
import com.app.attops.features.reports.domain.model.IntegrityScorecard
import kotlinx.coroutines.flow.Flow

enum class ReportFilter {
    LAST_WEEK,
    LAST_MONTH,
    LAST_6_MONTHS,
    LAST_YEAR,
    ALL_TIME
}

interface ReportRepository {
    fun getIntegrityScorecards(filter: ReportFilter = ReportFilter.ALL_TIME): Flow<Result<List<IntegrityScorecard>>>
    fun getEmployeeTimesheet(employeeId: String, month: Int, year: Int): Flow<Result<List<com.app.attops.core.network.model.TaskAttendance>>>
}
