package com.app.attops.features.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.app.attops.core.designsystem.components.AttOpsPrimaryButton
import com.app.attops.core.designsystem.components.AttOpsTextField

@Composable
fun EmployeeLoginScreen(
    onLoginClick: (String, String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var orgCode by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Employee Login",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Enter your organization details and credentials to login.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        AttOpsTextField(
            value = orgCode,
            onValueChange = { orgCode = it },
            label = "Organization Code",
            placeholder = "e.g. ATT123"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AttOpsTextField(
            value = employeeId,
            onValueChange = { employeeId = it },
            label = "Employee ID",
            placeholder = "e.g. EMP001"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AttOpsTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        AttOpsPrimaryButton(
            text = "Login",
            onClick = { onLoginClick(orgCode, employeeId, password) },
            enabled = orgCode.isNotBlank() && employeeId.isNotBlank() && password.isNotBlank()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Go Back")
        }
    }
}
