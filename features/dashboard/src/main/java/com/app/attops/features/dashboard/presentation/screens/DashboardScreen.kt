package com.app.attops.features.dashboard.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Sync
import com.app.attops.core.network.model.UserRole
import com.app.attops.features.dashboard.presentation.components.LogoutDialog
import com.app.attops.features.dashboard.presentation.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onLogoutConfirmed: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val data = uiState.data
    var showLogoutDialog by remember { mutableStateOf(value = false) }
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AttOps Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        data?.let {
                            Text(
                                text = it.organization.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (pendingSyncCount > 0) {
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Syncing",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Dashboard Error", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = uiState.error!!, textAlign = TextAlign.Center)
                        Button(onClick = { viewModel.loadDashboardData() }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Try Again")
                        }
                    }
                }
                data != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Welcome Section
                        Text(
                            text = "Hello, ${data.user.name.split(" ").firstOrNull() ?: "User"}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        // Organization Overview Card (Only for Owners/Admins)
                        if ((data.user.role == UserRole.OWNER) || (data.user.role == UserRole.ADMIN)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                ) {
                                    Column {
                                        Text(text = "Org Code (Share with Staff)", style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            text = data.organization.orgCode,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(data.organization.orgCode))
                                    }) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                                    }
                                }
                            }
                        }

                        // Stats Grid (Phase 4.3 Upgrade)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Today's Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                    StatCard(
                                        title = if (data.user.role == UserRole.EMPLOYEE) "My Pending" else "Total Pending",
                                        value = data.taskStats["PENDING"]?.toString() ?: "0",
                                        icon = Icons.AutoMirrored.Filled.Assignment,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.onTasksClick() }
                                    )
                                StatCard(
                                    title = if (data.user.role == UserRole.EMPLOYEE) "My Completed" else "Total Completed",
                                    value = data.taskStats["COMPLETED"]?.toString() ?: "0",
                                    icon = Icons.Default.TaskAlt,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.onTasksClick() }
                                )
                            }
                            
                            if ((data.user.role == UserRole.OWNER) || (data.user.role == UserRole.ADMIN)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatCard(
                                        title = "Active Staff",
                                        value = data.employeeCount.toString(),
                                        icon = Icons.Default.Group,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.onEmployeesClick() }
                                    )
                                    StatCard(
                                        title = "Checked In",
                                        value = data.taskStats["IN_PROGRESS"]?.toString() ?: "0",
                                        icon = Icons.Default.Person,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.onTasksClick() }
                                    )
                                }
                            }
                        }

                        // Workforce Operations (Phase 4.1 Actions)
                        Text(
                            text = "Workforce Operations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        if ((data.user.role == UserRole.OWNER) || (data.user.role == UserRole.ADMIN)) {
                            ActionItem(
                                title = "Manage All Tasks",
                                subtitle = "Create, assign and monitor field work",
                                icon = Icons.AutoMirrored.Filled.Assignment,
                                onClick = { viewModel.onTasksClick() },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            )

                            ActionItem(
                                title = "Add New Employee",
                                subtitle = "Onboard staff to your organization",
                                icon = Icons.Default.Person,
                                onClick = { viewModel.onEmployeesClick() }
                            )
                        } else {
                            ActionItem(
                                title = "My Tasks & Attendance",
                                subtitle = "View assignments and check-in to sites",
                                icon = Icons.AutoMirrored.Filled.Assignment,
                                onClick = { viewModel.onTasksClick() },
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                        }

                        // Directory Section
                        Text(
                            text = "Directory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        if ((data.user.role == UserRole.OWNER) || (data.user.role == UserRole.ADMIN)) {
                            ActionItem(
                                title = "Employee Directory",
                                subtitle = "View and manage all staff members",
                                icon = Icons.Default.Group,
                                onClick = { viewModel.onEmployeesClick() }
                            )
                        }

                        ActionItem(
                            title = "My Profile",
                            subtitle = "Manage your account settings",
                            icon = Icons.Default.Person,
                            onClick = { /* Future */ }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Initializing Dashboard...")
                            Button(onClick = { viewModel.loadDashboardData() }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("Retry Sync")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
                onLogoutConfirmed()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ActionItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
