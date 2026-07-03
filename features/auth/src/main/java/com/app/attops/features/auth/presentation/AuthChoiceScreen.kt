package com.app.attops.features.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.attops.core.designsystem.components.AttOpsPrimaryButton
import com.app.attops.core.designsystem.components.AttOpsSecondaryButton
import com.app.attops.features.auth.presentation.util.GoogleSignInHandler
import kotlinx.coroutines.launch

@Composable
fun AuthChoiceScreen(
    onGoogleSignInSuccess: (String) -> Unit,
    onEmployeeLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleSignInHandler = GoogleSignInHandler(context)

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
            text = "Continue with Google",
            onClick = {
                scope.launch {
                    val idToken = googleSignInHandler.signIn()
                    if (idToken != null) {
                        onGoogleSignInSuccess(idToken)
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AttOpsSecondaryButton(
            text = "Employee Login",
            onClick = onEmployeeLoginClick
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Organization Heads use Google Sign-In to manage their workspace.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
