package com.app.attops.features.employee.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.attops.core.designsystem.components.AttOpsTextField
import com.app.attops.core.network.model.UserRole
import com.app.attops.features.employee.presentation.components.EmployeeCard
import com.app.attops.features.employee.presentation.viewmodel.EmployeeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    viewModel: EmployeeViewModel,
    onEmployeeClick: (String) -> Unit,
    onAddEmployeeClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserRole = uiState.currentUserRole

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employees") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            // Only Owner or Admin can add employees
            if (currentUserRole == UserRole.OWNER || currentUserRole == UserRole.ADMIN) {
                FloatingActionButton(
                    onClick = onAddEmployeeClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Employee")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            AttOpsTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = "Search Employees",
                placeholder = "Search by name or ID",
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // State Handling
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                    }
                }
                uiState.employees.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No employees found", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.employees) { employee ->
                            EmployeeCard(
                                employee = employee,
                                onClick = { onEmployeeClick(employee.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
