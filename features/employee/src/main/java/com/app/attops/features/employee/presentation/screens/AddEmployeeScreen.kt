package com.app.attops.features.employee.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.attops.core.common.util.PasswordGenerator
import com.app.attops.core.designsystem.components.AttOpsPrimaryButton
import com.app.attops.core.designsystem.components.AttOpsTextField
import com.app.attops.core.network.model.UserRole
import com.app.attops.features.employee.presentation.state.EmployeeUiEvent
import com.app.attops.features.employee.presentation.viewmodel.EmployeeViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeScreen(
    viewModel: EmployeeViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var employeeId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var password by remember { mutableStateOf(PasswordGenerator.generate()) }
    var role by remember { mutableStateOf(UserRole.EMPLOYEE) }
    var expanded by remember { mutableStateOf(false) }

    // Validation States
    var nameError by remember { mutableStateOf<String?>(null) }
    var employeeIdError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is EmployeeUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is EmployeeUiEvent.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add Employee") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            AttOpsTextField(
                value = name,
                onValueChange = { 
                    name = it
                    nameError = if (it.isBlank()) "Name is required" else null
                },
                label = "Full Name",
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } }
            )

            AttOpsTextField(
                value = employeeId,
                onValueChange = { 
                    employeeId = it
                    employeeIdError = if (it.isBlank()) "Employee ID is required" else null
                },
                label = "Employee ID",
                isError = employeeIdError != null,
                supportingText = employeeIdError?.let { { Text(it) } }
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
                label = "Email (Optional)"
            )

            AttOpsTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Phone Number (Optional)"
            )

            AttOpsTextField(
                value = password,
                onValueChange = { 
                    password = it
                    passwordError = if (it.length < 8) "Password must be at least 8 characters" else null
                },
                label = "Temporary Password",
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it) } },
                trailingIcon = {
                    IconButton(onClick = { password = PasswordGenerator.generate() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate")
                    }
                }
            )

            // Role Selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = role.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Role") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // Determine available roles based on current user's role
                    val availableRoles = if (uiState.currentUserRole == UserRole.OWNER) {
                        listOf(UserRole.EMPLOYEE, UserRole.ADMIN)
                    } else {
                        listOf(UserRole.EMPLOYEE)
                    }

                    availableRoles.forEach { roleOption ->
                        DropdownMenuItem(
                            text = { Text(roleOption.name) },
                            onClick = {
                                role = roleOption
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AttOpsPrimaryButton(
                text = "Create Employee",
                onClick = {
                    if (name.isBlank()) nameError = "Name is required"
                    if (employeeId.isBlank()) employeeIdError = "Employee ID is required"
                    if (password.length < 8) passwordError = "Password too short"
                    
                    if (nameError == null && employeeIdError == null && passwordError == null) {
                        viewModel.addEmployee(
                            employeeId = employeeId.trim(),
                            name = name.trim(),
                            email = email.trim().ifBlank { null },
                            phone = phone.trim().ifBlank { null },
                            department = department.trim().ifBlank { null },
                            designation = designation.trim().ifBlank { null },
                            role = role,
                            password = password
                        )
                    }
                },
                enabled = !uiState.isSaving
            )
        }
    }
}
