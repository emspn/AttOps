package com.app.attops.features.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun GoogleLoginScreen(
    onLoginSuccess: (Boolean) -> Unit // Boolean: true if first login (needs org creation)
) {
    // This is a placeholder for the actual Google Sign-In Intent/Flow logic
    LaunchedEffect(Unit) {
        delay(1500)
        // Simulating logic: if email not found in DB, return true
        onLoginSuccess(true) 
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Connecting to Google...",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please wait while we verify your account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
