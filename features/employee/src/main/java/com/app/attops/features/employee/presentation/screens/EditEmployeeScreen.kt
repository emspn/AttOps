package com.app.attops.features.employee.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.attops.core.designsystem.components.AttOpsPrimaryButton
import com.app.attops.core.designsystem.components.AttOpsTextField
import com.app.attops.core.network.model.UserRole
import com.app.attops.core.network.model.UserStatus
import com.app.attops.features.employee.presentation.viewmodel.EmployeeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEmployeeScreen(
    employeeId: String,
    viewModel: EmployeeViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val employee = uiState.selectedEmployee

    LaunchedEffect(employeeId) {
        viewModel.loadEmployee(employeeId)
    }

    var name by remember(employee) { mutableStateOf(employee?.name ?: "") }
    var email by remember(employee) { mutableStateOf(employee?.email ?: "") }
    var phone by remember(employee) { mutableStateOf(employee?.phone ?: "") }
    var department by remember(employee) { mutableStateOf(employee?.department ?: "") }
    var designation by remember(employee) { mutableStateOf(employee?.designation ?: "") }
    var role by remember(employee) { mutableStateOf(employee?.role ?: UserRole.EMPLOYEE) }
    var status by remember(employee) { mutableStateOf(employee?.status ?: UserStatus.ACTIVE) }
    var roleExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Employee") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (employee != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AttOpsTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name"
                )

                AttOpsTextField(
                    value = employee.employeeId ?: "",
                    onValueChange = {},
                    label = "Employee ID",
                    enabled = false // ID shouldn't be editable
                )

                AttOpsTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = "Designation"
                )

                AttOpsTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = "Department"
                )

                AttOpsTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email"
                )

                AttOpsTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone Number"
                )

                // Role Selection
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    OutlinedTextField(
                        value = role.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        UserRole.entries.forEach { roleOption ->
                            DropdownMenuItem(
                                text = { Text(roleOption.name) },
                                onClick = {
                                    role = roleOption
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }

                // Status Selection
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    OutlinedTextField(
                        value = status.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        UserStatus.entries.forEach { statusOption ->
                            DropdownMenuItem(
                                text = { Text(statusOption.name) },
                                onClick = {
                                    status = statusOption
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AttOpsPrimaryButton(
                    text = "Update Employee",
                    onClick = {
                        viewModel.updateEmployee(
                            employee.copy(
                                name = name,
                                email = email,
                                phone = phone,
                                department = department,
                                designation = designation,
                                role = role,
                                status = status
                            )
                        )
                    },
                    enabled = name.isNotBlank() && !uiState.isSaving
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Employee") },
            text = { Text("Are you sure you want to delete this employee? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEmployee(employeeId)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
