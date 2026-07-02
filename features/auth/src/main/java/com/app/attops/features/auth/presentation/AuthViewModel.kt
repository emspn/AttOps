package com.app.attops.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.User
import com.app.attops.features.auth.usecase.CheckSessionUseCase
import com.app.attops.features.auth.usecase.CreateOrganizationUseCase
import com.app.attops.features.auth.usecase.LoginUseCase
import com.app.attops.features.auth.usecase.SignInWithGoogleUseCase
import com.app.attops.features.auth.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val checkSessionUseCase: CheckSessionUseCase,
    private val loginUseCase: LoginUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val createOrganizationUseCase: CreateOrganizationUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AuthUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            checkSessionUseCase().collect { user ->
                if (user != null) {
                    _uiState.update { it.copy(isAuthenticated = true, user = user) }
                    _uiEvent.emit(AuthUiEvent.NavigateToDashboard)
                }
            }
        }
    }

    fun login(orgCode: String, employeeId: String, pass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = loginUseCase(orgCode, employeeId, pass)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true, user = result.data) }
                    _uiEvent.emit(AuthUiEvent.NavigateToDashboard)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    _uiEvent.emit(AuthUiEvent.ShowError(result.message ?: "Login failed"))
                }
                is Result.Loading -> { /* Handled by initial state update */ }
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = signInWithGoogleUseCase()) {
                is Result.Success -> {
                    if (result.data.organizationId.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, isFirstLogin = true, user = result.data) }
                        _uiEvent.emit(AuthUiEvent.NavigateToOrgCreation)
                    } else {
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = true, user = result.data) }
                        _uiEvent.emit(AuthUiEvent.NavigateToDashboard)
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    _uiEvent.emit(AuthUiEvent.ShowError(result.message ?: "Google Sign-In failed"))
                }
                is Result.Loading -> {}
            }
        }
    }

    fun createOrganization(name: String, type: String, phone: String, address: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = createOrganizationUseCase(name, type, phone, address)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                    _uiEvent.emit(AuthUiEvent.NavigateToDashboard)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    _uiEvent.emit(AuthUiEvent.ShowError(result.message ?: "Failed to create organization"))
                }
                is Result.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            signOutUseCase()
            _uiState.update { AuthUiState() }
        }
    }
}
