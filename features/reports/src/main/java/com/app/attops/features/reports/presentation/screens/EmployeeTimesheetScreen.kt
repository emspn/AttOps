package com.app.attops.features.reports.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.attops.core.network.model.TaskAttendance
import com.app.attops.features.reports.presentation.viewmodel.ReportsViewModel
import android.content.Intent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeTimesheetScreen(
    employeeId: String,
    fullName: String,
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val attendances = remember { mutableStateListOf<TaskAttendance>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(employeeId) {
        val now = LocalDateTime.now()
        viewModel.loadTimesheet(employeeId, now.monthValue, now.year) { result ->
            attendances.clear()
            attendances.addAll(result)
            isLoading = false
        }
    }

    LaunchedEffect(uiState.exportFile) {
        uiState.exportFile?.let { file ->
            val uri = FileProvider.getUriForFile(
                context,
                "com.app.attops.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Timesheet"))
            viewModel.clearExportFile()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Monthly Timesheet", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.exportTimesheetToPdf(context, fullName, attendances) },
                        enabled = !isLoading && attendances.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export PDF")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (attendances.isEmpty()) {
                Text("No logs found for this month.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(attendances) { log ->
                        TimesheetItem(log)
                    }
                }
            }
        }
    }
}

@Composable
fun TimesheetItem(log: TaskAttendance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatDate(log.checkInTime),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Check In", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTime(log.checkInTime), style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Check Out", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTime(log.checkOutTime) ?: "--:--", style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            if (log.integrityDistance != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "GPS Deviation: ${log.integrityDistance?.toInt()}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = if ((log.integrityDistance ?: 0.0) > 200) MaterialTheme.colorScheme.error else Color.Gray
                )
            }
        }
    }
}

fun formatDate(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        val dt = LocalDateTime.parse(isoString.replace("Z", ""))
        dt.format(DateTimeFormatter.ofPattern("EEE, MMM dd"))
    } catch (e: Exception) {
        isoString
    }
}

fun formatTime(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        val dt = LocalDateTime.parse(isoString.replace("Z", ""))
        dt.format(DateTimeFormatter.ofPattern("hh:mm a"))
    } catch (e: Exception) {
        isoString
    }
}
