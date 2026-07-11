package com.app.attops.features.reports.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.attops.core.common.result.Result
import com.app.attops.features.reports.domain.model.IntegrityScorecard
import com.app.attops.features.reports.domain.repository.ReportFilter
import com.app.attops.features.reports.domain.repository.ReportRepository
import com.app.attops.features.reports.util.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ReportsUiState(
    val isLoading: Boolean = false, // Modern: Default to false, let first emission handle it
    val scorecards: List<IntegrityScorecard> = emptyList(),
    val currentFilter: ReportFilter = ReportFilter.ALL_TIME,
    val exportFile: File? = null,
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadScorecards(ReportFilter.ALL_TIME)
    }

    fun loadScorecards(filter: ReportFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
        viewModelScope.launch {
            repository.getIntegrityScorecards(filter).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update { 
                        it.copy(isLoading = false, scorecards = result.data, error = null) 
                    }
                    is Result.Error -> _uiState.update { 
                        it.copy(isLoading = false, error = result.message) 
                    }
                }
            }
        }
    }

    fun exportToPdf(context: Context) {
        val scorecards = uiState.value.scorecards
        if (scorecards.isNotEmpty()) {
            val file = PdfGenerator.generateIntegrityReport(context, scorecards, uiState.value.currentFilter)
            _uiState.update { it.copy(exportFile = file) }
        }
    }

    fun exportToCsv(context: Context) {
        val scorecards = uiState.value.scorecards
        if (scorecards.isNotEmpty()) {
            val file = PdfGenerator.generateIntegrityCsv(context, scorecards, uiState.value.currentFilter)
            _uiState.update { it.copy(exportFile = file) }
        }
    }

    fun clearExportFile() {
        _uiState.update { it.copy(exportFile = null) }
    }

    fun exportTimesheetToPdf(context: Context, fullName: String, attendances: List<com.app.attops.core.network.model.TaskAttendance>) {
        if (attendances.isNotEmpty()) {
            val file = PdfGenerator.generateEmployeeTimesheetPdf(context, fullName, attendances)
            _uiState.update { it.copy(exportFile = file) }
        }
    }

    fun loadTimesheet(
        employeeId: String,
        month: Int,
        year: Int,
        onResult: (List<com.app.attops.core.network.model.TaskAttendance>) -> Unit
    ) {
        viewModelScope.launch {
            repository.getEmployeeTimesheet(employeeId, month, year).collect { result ->
                if (result is Result.Success) {
                    onResult(result.data)
                }
            }
        }
    }
}
