package com.app.attops.features.auth.presentation

import com.app.attops.core.network.model.User

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val user: User? = null,
    val isFirstLogin: Boolean = false
)

sealed interface AuthUiEvent {
    data class ShowError(val message: String) : AuthUiEvent
    data object NavigateToDashboard : AuthUiEvent
    data object NavigateToOrgCreation : AuthUiEvent
}
