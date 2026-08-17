package com.app.attops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.attops.features.auth.repository.AuthRepository
import com.app.attops.core.network.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import com.app.attops.core.notifications.domain.SyncFcmTokenUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncFcmTokenUseCase: SyncFcmTokenUseCase
) : ViewModel() {
    
    // Global user state that MainActivity and other components can observe
    val currentUser: StateFlow<User?> = authRepository.getCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        observeUserForTokenSync()
    }

    private fun observeUserForTokenSync() {
        viewModelScope.launch {
            currentUser.collectLatest { user ->
                if (user != null) {
                    syncFcmTokenUseCase()
                }
            }
        }
    }
}
