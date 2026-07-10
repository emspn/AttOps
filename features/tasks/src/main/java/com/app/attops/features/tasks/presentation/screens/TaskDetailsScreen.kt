package com.app.attops.features.tasks.presentation.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.app.attops.core.network.model.TaskStatus
import com.app.attops.core.network.model.UserRole
import com.app.attops.features.tasks.presentation.components.PriorityBadge
import com.app.attops.features.tasks.presentation.components.StatusBadge
import com.app.attops.features.tasks.presentation.viewmodel.TaskUiEvent
import com.app.attops.features.tasks.presentation.viewmodel.TaskViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsScreen(
    taskId: String,
    viewModel: TaskViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val userRole = uiState.userRole
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val task = uiState.tasks.find { it.id == taskId }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Deadline check
    val isExpired = remember(task) {
        task?.dueDate?.let {
            try {
                val deadline = OffsetDateTime.parse(it)
                deadline.isBefore(OffsetDateTime.now())
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    val deadlineDisplay = remember(task) {
        task?.dueDate?.let {
            try {
                val deadline = OffsetDateTime.parse(it)
                deadline.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
            } catch (e: Exception) {
                "No deadline set"
            }
        } ?: "No deadline set"
    }

    LaunchedEffect(taskId) {
        viewModel.loadTaskDetails(taskId)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        
        if (!cameraGranted || !locationGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Camera and Location permissions are required.")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            if (event is TaskUiEvent.ShowError) {
                snackbarHostState.showSnackbar(event.message)
            } else if (event is TaskUiEvent.NavigateBack) {
                onBackClick()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Task Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (viewModel.canDeleteTask(task)) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Task", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (task == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Header Status
                HeaderSection(task, isExpired, deadlineDisplay)

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Description
                    InfoItem(
                        title = "Work Description",
                        icon = Icons.Default.Description,
                        value = task.description ?: "No description provided."
                    )

                    // Location
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = task.locationName ?: "General Location",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (task.latitude != null && task.longitude != null) {
                            OutlinedButton(
                                onClick = {
                                    val gmmIntentUri = "geo:${task.latitude},${task.longitude}?q=${task.latitude},${task.longitude}(${task.locationName})".toUri()
                                    context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Map, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Open Map")
                            }
                        }
                    }

                    // Attendance Proof
                    uiState.attendance?.let { att ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text("Attendance Proof", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        
                        att.integrityDistance?.let { dist ->
                            IntegrityBadge(dist)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProofCard("Check In", att.checkInTime, att.checkInImageUrl, att.checkInLat, att.checkInLng, context)
                            if (att.status == "CHECKED_OUT") {
                                ProofCard("Check Out", att.checkOutTime, att.checkOutImageUrl, att.checkOutLat, att.checkOutLng, context)
                            }
                        }
                    }

                    Spacer(Modifier.height(100.dp))
                }
            }

            // Fixed Bottom Actions
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (userRole == UserRole.EMPLOYEE || userRole == UserRole.ADMIN) {
                    EmployeeActions(
                        task = task, 
                        attendance = uiState.attendance,
                        isExpired = isExpired, 
                        isLoading = uiState.isLoading || uiState.isPerformingAttendance,
                        permissionLauncher = permissionLauncher, 
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showDeleteDialog && task != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task?") },
            text = { Text("This action cannot be undone. Are you sure you want to remove this task?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTask(task.id!!)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

    if (uiState.isCameraOpen) {
        CameraScreen(
            onPhotoCaptured = { path -> viewModel.handlePhotoCaptured(path) },
            onCancel = { viewModel.closeCamera() }
        )
    }
}

@Composable
fun HeaderSection(task: com.app.attops.core.network.model.Task, isExpired: Boolean, deadlineDisplay: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityBadge(priority = task.priority)
                StatusBadge(status = task.status)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Deadline: $deadlineDisplay",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isExpired && task.status != TaskStatus.COMPLETED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun InfoItem(title: String, icon: ImageVector, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProofCard(label: String, time: String?, imageUrl: String?, lat: Double?, lng: Double?, context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(text = time?.split("T")?.lastOrNull()?.take(5) ?: "", style = MaterialTheme.typography.labelSmall)
            }
            if (!imageUrl.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .wrapContentHeight()
                            .clickable {
                                if (lat != null && lng != null) {
                                    val uri = "geo:$lat,$lng?q=$lat,$lng($label Spot)".toUri()
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }
                            },
                        contentScale = ContentScale.FillWidth
                    )
                    
                    if (lat != null && lng != null) {
                        Surface(
                            onClick = {
                                val uri = "geo:$lat,$lng?q=$lat,$lng($label Spot)".toUri()
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Location Link",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            if (lat != null && lng != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Location: $lat, $lng",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun IntegrityBadge(distance: Double) {
    val isReliable = distance < 200
    Surface(
        color = (if (isReliable) Color(0xFF22C55E) else Color(0xFFEF4444)).copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isReliable) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                contentDescription = null,
                tint = if (isReliable) Color(0xFF22C55E) else Color(0xFFEF4444),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Integrity: Worker moved ${String.format(Locale.getDefault(), "%.1f", distance)}m",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isReliable) Color(0xFF22C55E) else Color(0xFFEF4444)
            )
        }
    }
}

@Composable
fun EmployeeActions(
    task: com.app.attops.core.network.model.Task,
    attendance: com.app.attops.core.network.model.TaskAttendance?,
    isExpired: Boolean,
    isLoading: Boolean,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    viewModel: TaskViewModel
) {
    // ATOMIC BUTTON SWAP: Use attendance status for better reliability
    val currentStatus = attendance?.status ?: "PENDING"
    
    AnimatedContent(
        targetState = currentStatus,
        transitionSpec = {
            fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
        },
        label = "ButtonTransition"
    ) { status ->
        if (!isExpired || status == "CHECKED_OUT") {
            val (btnText, btnColor) = when (status) {
                "PENDING" -> "Verify & Check In" to MaterialTheme.colorScheme.primary
                "CHECKED_IN" -> "Complete & Check Out" to MaterialTheme.colorScheme.error
                else -> "" to MaterialTheme.colorScheme.primary
            }

            if (btnText.isNotEmpty()) {
                Button(
                    onClick = { 
                        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION))
                        if (status == "PENDING") {
                            viewModel.requestCheckIn(task.id!!, task.latitude ?: 0.0, task.longitude ?: 0.0)
                        } else {
                            viewModel.requestCheckOut(task.id!!)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(btnText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Expired", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}
