package com.app.attops.features.auth.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.attops.core.designsystem.components.AttOpsPrimaryButton
import com.app.attops.core.designsystem.components.AttOpsSecondaryButton
import com.app.attops.features.auth.presentation.util.GoogleSignInHandler
import com.app.attops.features.auth.presentation.util.SignInResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AuthChoiceScreen(
    viewModel: AuthViewModel,
    onEmployeeLoginClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val googleSignInHandler = remember(context) { GoogleSignInHandler(context) }
    
    // Immediate local feedback state to disable button instantly
    var isTriggeringPicker by remember { mutableStateOf(false) }
    
    val TAG = "AUTH_CHOICE"

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            if (event is AuthUiEvent.ShowError) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Show loading if Supabase is working OR if we are waiting for the system picker
            if (uiState.isLoading || isTriggeringPicker) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Welcome to AttOps",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Modern Field Workforce Management Platform",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                AttOpsPrimaryButton(
                    text = if (isTriggeringPicker) "Opening..." else "Continue with Google",
                    onClick = {
                        val activity = context.findActivity()
                        if (activity != null) {
                            viewModel.performGoogleSignIn(
                                activity = activity,
                                handler = googleSignInHandler,
                                onStateChanged = { isTriggeringPicker = it }
                            )
                        }
                    },
                    enabled = !uiState.isLoading && !isTriggeringPicker
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AttOpsSecondaryButton(
                    text = "Employee Login",
                    onClick = {
                        if (!isTriggeringPicker && !uiState.isLoading) {
                            onEmployeeLoginClick()
                        }
                    },
                    enabled = !uiState.isLoading && !isTriggeringPicker
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Organization Owners use Google Sign-In to manage their workspace.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
