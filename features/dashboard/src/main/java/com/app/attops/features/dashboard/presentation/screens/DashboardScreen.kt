package com.app.attops.features.dashboard.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.attops.core.network.model.UserRole
import com.app.attops.features.dashboard.presentation.components.LogoutDialog
import com.app.attops.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.app.attops.features.dashboard.usecase.DashboardData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val data = uiState.data
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text("AttOps", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp) },
                actions = {
                    if (pendingSyncCount > 0) { SyncIndicator() }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            when {
                uiState.isLoading -> LoadingShimmer()
                uiState.error != null -> ErrorState(uiState.error!!) { viewModel.loadDashboardData() }
                data != null -> {
                    AnimatedContent(targetState = data, label = "DashboardContent") { targetData ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Spacer(Modifier.height(4.dp))
                            HeaderSection(targetData.user.name, targetData.organization.name)

                            if (targetData.user.role != UserRole.EMPLOYEE) {
                                OrgCodeCard(targetData.organization.orgCode) {
                                    clipboardManager.setText(AnnotatedString(targetData.organization.orgCode))
                                }
                            }

                            StatsSection(targetData, viewModel)
                            QuickActionsSection(targetData, viewModel)
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncIndicator() {
    val rotation by rememberInfiniteTransition(label = "SyncRotate").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing)), label = "rotation"
    )
    Icon(imageVector = Icons.Default.Sync, contentDescription = "Syncing", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rotation })
}

@Composable
fun HeaderSection(userName: String, orgName: String) {
    Column {
        Text(text = "Hello, ${userName.split(" ").firstOrNull() ?: "User"}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(text = orgName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun OrgCodeCard(orgCode: String, onCopy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column {
                Text(text = "Organization Code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = orgCode, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
            }
            IconButton(onClick = onCopy, modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun StatsSection(data: DashboardData, viewModel: DashboardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Pending", data.taskStats["PENDING"]?.toString() ?: "0", Icons.AutoMirrored.Filled.Assignment, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f)) { viewModel.onTasksClick() }
            StatCard("Done", data.taskStats["COMPLETED"]?.toString() ?: "0", Icons.Default.CheckCircle, MaterialTheme.colorScheme.secondary, Modifier.weight(1f)) { viewModel.onTasksClick() }
        }
        if (data.user.role != UserRole.EMPLOYEE) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Staff", data.employeeCount.toString(), Icons.Default.Group, MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { viewModel.onEmployeesClick() }
                StatCard("Active", data.taskStats["IN_PROGRESS"]?.toString() ?: "0", Icons.Default.Bolt, MaterialTheme.colorScheme.error, Modifier.weight(1f)) { viewModel.onTasksClick() }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickActionsSection(data: DashboardData, viewModel: DashboardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Operations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (data.user.role != UserRole.EMPLOYEE) {
            ActionCard("Task Control", "Manage organization tasks", Icons.Default.AssignmentInd, MaterialTheme.colorScheme.primary) { viewModel.onTasksClick() }
            ActionCard("Staff List", "Manage your workforce", Icons.Default.PeopleAlt, MaterialTheme.colorScheme.secondary) { viewModel.onEmployeesClick() }
        } else {
            ActionCard("My Work", "Assignments & Attendance", Icons.Default.Task, MaterialTheme.colorScheme.primary) { viewModel.onTasksClick() }
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun LoadingShimmer() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth(0.3f).height(20.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = 0.1f)))
        Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(20.dp)).background(Color.Gray.copy(alpha = 0.1f)))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(16.dp)).background(Color.Gray.copy(alpha = 0.1f)))
            Box(modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(16.dp)).background(Color.Gray.copy(alpha = 0.1f)))
        }
    }
}

@Composable
fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(text = "Connection Error", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = error, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp), shape = RoundedCornerShape(10.dp)) { Text("Retry") }
    }
}
