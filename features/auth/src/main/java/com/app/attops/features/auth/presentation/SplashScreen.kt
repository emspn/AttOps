package com.app.attops.features.auth.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    onNavigateToAuth: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToOrgCreation: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is AuthUiEvent.NavigateToDashboard -> onNavigateToDashboard()
                is AuthUiEvent.NavigateToOrgCreation -> onNavigateToOrgCreation()
                else -> {}
            }
        }
    }

    // Handle initial navigation if session check completes without events
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            if (uiState.isAuthenticated) {
                uiState.user?.let { user ->
                    if (user.organizationId.isEmpty()) {
                        onNavigateToOrgCreation()
                    } else {
                        onNavigateToDashboard()
                    }
                }
            } else {
                onNavigateToAuth()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "AttOps",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
