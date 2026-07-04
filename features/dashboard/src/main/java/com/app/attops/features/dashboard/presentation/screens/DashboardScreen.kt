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
    val data = uiState.data
    var showLogoutDialog by remember { mutableStateOf(value = false) }
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AttOps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
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
                            text = "Welcome back, ${data.user.name.split(" ").firstOrNull() ?: "User"}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        // Organization Overview Card (Only for Owners/Admins)
                        if (data.user.role == UserRole.OWNER || data.user.role == UserRole.ADMIN) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                ) {
                                    Column {
                                        Text(text = "Organization Code", style = MaterialTheme.typography.labelMedium)
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

                        // Stats Grid
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (data.user.role == UserRole.OWNER || data.user.role == UserRole.ADMIN) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    StatCard(
                                        title = "Employees",
                                        value = data.employeeCount.toString(),
                                        icon = Icons.Default.Group,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.onEmployeesClick() }
                                    )
                                    StatCard(
                                        title = "Admins",
                                        value = data.adminCount.toString(),
                                        icon = Icons.Default.Person,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.onEmployeesClick() }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                StatCard(
                                    title = "Pending Tasks",
                                    value = "Coming Soon",
                                    icon = Icons.AutoMirrored.Filled.Assignment,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.onTasksClick() }
                                )
                                StatCard(
                                    title = "Attendance",
                                    value = "Coming Soon",
                                    icon = Icons.Default.Group,
                                    modifier = Modifier.weight(1f),
                                    onClick = { /* Future */ }
                                )
                            }
                        }

                        // Management Section
                        Text(
                            text = "Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        if (data.user.role == UserRole.OWNER || data.user.role == UserRole.ADMIN) {
                            ActionItem(
                                title = "Add New Employee",
                                subtitle = "Create credentials for your staff",
                                icon = Icons.Default.Person,
                                onClick = { viewModel.onEmployeesClick() }
                            )
                            
                            ActionItem(
                                title = "Employee Directory",
                                subtitle = "View and manage all members",
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

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                else -> {
                    // This handles data == null and error == null
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Dashboard is empty.")
                            Button(onClick = { viewModel.loadDashboardData() }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("Refresh")
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
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ActionItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
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
