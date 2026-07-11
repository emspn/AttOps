package com.app.attops.features.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.attops.core.designsystem.components.AttOpsPrimaryButton
import com.app.attops.core.designsystem.components.AttOpsTextField
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgCreationScreen(
    viewModel: AuthViewModel,
    onOrgCreated: (String, String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var orgName by remember { mutableStateOf("") }
    var orgType by remember { mutableStateOf("") }
    var orgAddress by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val businessTypes = listOf(
        "Construction",
        "Manufacturing",
        "Retail",
        "Healthcare",
        "Education",
        "Logistics & Transport",
        "IT & Software",
        "Hospitality",
        "Agriculture",
        "Real Estate",
        "Sales & Marketing",
        "Security Services",
        "Other"
    )

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
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }

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
                    placeholder = "e.g. Acme Industries",
                    enabled = !uiState.isLoading
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Business Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!uiState.isLoading) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = orgType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Business Type") },
                        placeholder = { Text("Select your industry") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        businessTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    orgType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AttOpsTextField(
                    value = orgAddress,
                    onValueChange = { orgAddress = it },
                    label = "Organization Address",
                    placeholder = "Enter full office address",
                    singleLine = false,
                    enabled = !uiState.isLoading
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                AttOpsPrimaryButton(
                    text = "Create Workspace",
                    onClick = { 
                        if (orgName.isNotBlank() && orgType.isNotBlank() && orgAddress.isNotBlank()) {
                            onOrgCreated(orgName.trim(), orgType, orgAddress.trim())
                        }
                    },
                    enabled = !uiState.isLoading && orgName.isNotBlank() && orgType.isNotBlank() && orgAddress.isNotBlank()
                )
            }
        }
    }
}
