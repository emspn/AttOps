package com.app.attops.features.tasks.presentation.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (task == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Task not found.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityBadge(priority = task.priority)
                    StatusBadge(status = task.status)
                }

                HorizontalDivider()

                // Deadline Section
                Column {
                    Text(text = "Deadline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = deadlineDisplay,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isExpired && task.status != TaskStatus.COMPLETED) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                        if (isExpired && task.status != TaskStatus.COMPLETED) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Text(" EXPIRED", color = Color.Red, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider()

                Text(text = "Work Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = task.description ?: "No description provided.", style = MaterialTheme.typography.bodyLarge)

                HorizontalDivider()

                Text(text = "Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                // Location Display with optional Map Attachment
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = task.locationName ?: "General Location", style = MaterialTheme.typography.bodyLarge)
                    
                    if (task.latitude != null && task.longitude != null) {
                        TextButton(
                            onClick = {
                                val gmmIntentUri = "geo:${task.latitude},${task.longitude}?q=${task.latitude},${task.longitude}(${task.locationName})".toUri()
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                context.startActivity(mapIntent)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View attached map location", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Evidence Section (Read-Only)
                uiState.attendance?.let { att ->
                    HorizontalDivider()
                    Text(text = "Attendance Proof", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    if (!att.checkInImageUrl.isNullOrEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            AsyncImage(
                                model = att.checkInImageUrl,
                                contentDescription = "Proof Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Checked In: ${att.checkInTime?.split("T")?.firstOrNull()} ${att.checkInTime?.split("T")?.lastOrNull()?.take(5)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (att.checkInLat != null && att.checkInLng != null) {
                            TextButton(
                                onClick = {
                                    val uri = "geo:${att.checkInLat},${att.checkInLng}?q=${att.checkInLat},${att.checkInLng}(Check-in Spot)".toUri()
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("View check-in location on map", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        
                        if (att.status == "CHECKED_OUT") {
                            Text(
                                text = "Checked Out: ${att.checkOutTime?.split("T")?.firstOrNull()} ${att.checkOutTime?.split("T")?.lastOrNull()?.take(5)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (!att.checkOutImageUrl.isNullOrEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(240.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    AsyncImage(
                                        model = att.checkOutImageUrl,
                                        contentDescription = "Check-out Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action Buttons for Employees
                if (userRole == UserRole.EMPLOYEE) {
                    if (isExpired && task.status != TaskStatus.COMPLETED) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Deadline passed. You failed to deliver the task.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        when (task.status) {
                            TaskStatus.PENDING -> {
                                Button(
                                    onClick = { 
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.CAMERA,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            )
                                        )
                                        viewModel.requestCheckIn(task.id!!, task.latitude ?: 0.0, task.longitude ?: 0.0)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isLoading
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Verify & Check In")
                                    }
                                }
                            }
                            TaskStatus.IN_PROGRESS -> {
                                Button(
                                    onClick = { 
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.CAMERA,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            )
                                        )
                                        viewModel.requestCheckOut(task.id!!) 
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Complete & Check Out")
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    if (uiState.isCameraOpen) {
        CameraScreen(
            onPhotoCaptured = { path -> viewModel.handlePhotoCaptured(path) },
            onCancel = { viewModel.closeCamera() }
        )
    }
}
