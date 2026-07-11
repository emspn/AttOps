package com.app.attops.features.dashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.attops.core.common.result.Result
import com.app.attops.core.common.util.RefreshBus
import com.app.attops.features.dashboard.presentation.state.DashboardUiEvent
import com.app.attops.features.dashboard.presentation.state.DashboardUiState
import com.app.attops.features.dashboard.repository.DashboardRepository
import com.app.attops.features.dashboard.usecase.GetDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val repository: DashboardRepository,
    private val attendanceDao: com.app.attops.core.common.database.AttendanceDao,
    private val refreshBus: RefreshBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<DashboardUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val pendingSyncCount = attendanceDao.getPendingCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        observeDashboardData()
    }

    private fun observeDashboardData() {
        viewModelScope.launch {
            getDashboardDataUseCase().collect { result ->
                when (result) {
                    is Result.Loading -> {
                        // Only show global loading if we don't have any cached data yet
                        if (_uiState.value.data == null) {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, data = result.data, error = null)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun loadDashboardData() {
        refreshBus.trigger()
    }

    fun logout() {
        viewModelScope.launch {
            repository.signOut()
            _uiEvent.emit(DashboardUiEvent.NavigateToAuth)
        }
    }

    fun onEmployeesClick() {
        viewModelScope.launch { _uiEvent.emit(DashboardUiEvent.NavigateToEmployees) }
    }

    fun onTasksClick() {
        viewModelScope.launch { _uiEvent.emit(DashboardUiEvent.NavigateToTasks) }
    }
}
