package com.app.attops.features.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.attops.core.designsystem.components.AttOpsPrimaryButton
import com.app.attops.core.designsystem.components.AttOpsTextField

@Composable
fun OrgCreationScreen(
    onOrgCreated: (String, String, String, String?) -> Unit
) {
    var orgName by remember { mutableStateOf("") }
    var orgType by remember { mutableStateOf("") }
    var orgAddress by remember { mutableStateOf("") }
    var orgPhone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Create Organization",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Set up your workspace to start managing your field workforce.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        AttOpsTextField(
            value = orgName,
            onValueChange = { orgName = it },
            label = "Organization Name",
            placeholder = "e.g. Acme Construction"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AttOpsTextField(
            value = orgType,
            onValueChange = { orgType = it },
            label = "Business Type",
            placeholder = "e.g. Construction, Solar, Sales"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AttOpsTextField(
            value = orgPhone,
            onValueChange = { orgPhone = it },
            label = "Mobile Number"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AttOpsTextField(
            value = orgAddress,
            onValueChange = { orgAddress = it },
            label = "Organization Address (Optional)",
            singleLine = false
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        AttOpsPrimaryButton(
            text = "Create Workspace",
            onClick = { onOrgCreated(orgName, orgType, orgPhone, orgAddress.ifBlank { null }) },
            enabled = orgName.isNotBlank() && orgType.isNotBlank() && orgPhone.isNotBlank()
        )
    }
}
