package com.app.attops.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.attops.core.common.result.Result
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

    // Critical: Prevents background session checks from navigating while a user is manually logging in
    private var isManualFlowActive = false

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            checkSessionUseCase().collect { user ->
                _uiState.update { it.copy(isLoading = false, isAuthenticated = user != null, user = user) }
                
                // Only auto-navigate if we AREN'T in a manual login flow (like clicking Google button)
                if (!isManualFlowActive && user != null) {
                    if (user.organizationId.isEmpty()) {
                        _uiEvent.emit(AuthUiEvent.NavigateToOrgCreation)
                    } else {
                        _uiEvent.emit(AuthUiEvent.NavigateToDashboard)
                    }
                }
            }
        }
    }

    fun startManualFlow() {
        isManualFlowActive = true
    }

    fun signInWithGoogle(idToken: String, nonce: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = signInWithGoogleUseCase(idToken, nonce)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true, user = result.data) }
                    if (result.data.organizationId.isEmpty()) {
                        _uiEvent.emit(AuthUiEvent.NavigateToOrgCreation)
                    } else {
                        _uiEvent.emit(AuthUiEvent.NavigateToDashboard)
                    }
                }
                is Result.Error -> {
                    isManualFlowActive = false
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    viewModelScope.launch {
                        _uiEvent.emit(AuthUiEvent.ShowError(result.message ?: "Google Sign-In failed"))
                    }
                }
                else -> {}
            }
        }
    }

    fun login(orgCode: String, employeeId: String, pass: String) {
        viewModelScope.launch {
            isManualFlowActive = true
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = loginUseCase(orgCode, employeeId, pass)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true, user = result.data) }
                    _uiEvent.emit(AuthUiEvent.NavigateToDashboard)
                }
                is Result.Error -> {
                    isManualFlowActive = false
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    viewModelScope.launch {
                        _uiEvent.emit(AuthUiEvent.ShowError(result.message ?: "Login failed"))
                    }
                }
                else -> {}
            }
        }
    }

    fun createOrganization(name: String, type: String, address: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = createOrganizationUseCase(name, type, address)) {
                is Result.Success -> {
                    val orgCode = result.data.orgCode
                    _uiState.update { it.copy(isLoading = false, createdOrgCode = orgCode) }
                    _uiEvent.emit(AuthUiEvent.NavigateToOrgCreationSuccess(orgCode))
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    viewModelScope.launch {
                        _uiEvent.emit(AuthUiEvent.ShowError(result.message ?: "Organization setup failed. Please try again."))
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Stable entry point for Google Sign-In. 
     * Uses viewModelScope to prevent cancellation on recomposition.
     */
    fun performGoogleSignIn(
        activity: android.app.Activity,
        handler: com.app.attops.features.auth.presentation.util.GoogleSignInHandler,
        onStateChanged: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            onStateChanged(true)
            startManualFlow()
            
            try {
                when (val result = handler.signIn(activity)) {
                    is com.app.attops.features.auth.presentation.util.SignInResult.Success -> {
                        signInWithGoogle(result.idToken, result.nonce)
                    }
                    is com.app.attops.features.auth.presentation.util.SignInResult.Error -> {
                        onGoogleSignInError(result.message)
                    }
                    is com.app.attops.features.auth.presentation.util.SignInResult.Cancelled -> {
                        onGoogleSignInCancelled()
                    }
                }
            } catch (e: Exception) {
                onGoogleSignInError(e.localizedMessage ?: "Unknown Error")
            } finally {
                onStateChanged(false)
            }
        }
    }

    private fun onGoogleSignInError(message: String) {
        isManualFlowActive = false
        _uiState.update { it.copy(isLoading = false, error = message) }
        viewModelScope.launch {
            _uiEvent.emit(AuthUiEvent.ShowError(message))
        }
    }

    private fun onGoogleSignInCancelled() {
        isManualFlowActive = false
        _uiState.update { it.copy(isLoading = false) }
    }
    
    fun logout() {
        viewModelScope.launch {
            // Logic to clear session and local state
            signOutUseCase()
            isManualFlowActive = false
            _uiState.update { AuthUiState() }
        }
    }
}
