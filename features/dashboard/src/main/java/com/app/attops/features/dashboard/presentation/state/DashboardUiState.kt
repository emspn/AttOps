package com.app.attops.features.dashboard.presentation.state

import com.app.attops.features.dashboard.usecase.DashboardData

data class DashboardUiState(
    val isLoading: Boolean = false,
    val data: DashboardData? = null,
    val error: String? = null
)

sealed interface DashboardUiEvent {
    data class ShowError(val message: String) : DashboardUiEvent
    data object NavigateToEmployees : DashboardUiEvent
    data object NavigateToTasks : DashboardUiEvent
    data object NavigateToProfile : DashboardUiEvent
}
