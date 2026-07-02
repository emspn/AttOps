package com.app.attops.features.employee.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.attops.core.common.util.PasswordGenerator
import com.app.attops.core.designsystem.components.AttOpsPrimaryButton
import com.app.attops.core.designsystem.components.AttOpsTextField
import com.app.attops.core.network.model.UserRole
import com.app.attops.features.employee.presentation.viewmodel.EmployeeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeScreen(
    viewModel: EmployeeViewModel,
    onBackClick: () -> Unit
) {
    var employeeId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var password by remember { mutableStateOf(PasswordGenerator.generate()) }
    var role by remember { mutableStateOf(UserRole.EMPLOYEE) }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
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
            AttOpsTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name"
            )

            AttOpsTextField(
                value = employeeId,
                onValueChange = { employeeId = it },
                label = "Employee ID"
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

            AttOpsTextField(
                value = password,
                onValueChange = { password = it },
                label = "Temporary Password",
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
                    UserRole.entries.forEach { roleOption ->
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
                    viewModel.addEmployee(
                        employeeId = employeeId,
                        name = name,
                        email = email.ifBlank { null },
                        phone = phone.ifBlank { null },
                        department = department.ifBlank { null },
                        designation = designation.ifBlank { null },
                        role = role,
                        password = password
                    )
                },
                enabled = name.isNotBlank() && employeeId.isNotBlank() && password.isNotBlank()
            )
        }
    }
}
