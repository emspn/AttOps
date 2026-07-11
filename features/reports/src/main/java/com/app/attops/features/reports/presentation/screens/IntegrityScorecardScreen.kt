package com.app.attops.features.reports.presentation.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.FileProvider
import com.app.attops.features.reports.domain.model.IntegrityScorecard
import com.app.attops.features.reports.domain.repository.ReportFilter
import com.app.attops.features.reports.presentation.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrityScorecardScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit,
    onEmployeeClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.exportFile) {
        uiState.exportFile?.let { file ->
            val uri = FileProvider.getUriForFile(
                context,
                "com.app.attops.fileprovider",
                file
            )
            val extension = file.extension.lowercase()
            val mimeType = if (extension == "pdf") "application/pdf" else "text/csv"
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Report"))
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
                        Text("Integrity Scorecards", fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = uiState.currentFilter.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Filter Menu
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                            ReportFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter.name.replace("_", " ")) },
                                    onClick = {
                                        showFilterMenu = false
                                        viewModel.loadScorecards(filter)
                                    },
                                    leadingIcon = {
                                        if (uiState.currentFilter == filter) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Export Menu
                    Box {
                        IconButton(
                            onClick = { showExportMenu = true },
                            enabled = uiState.scorecards.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export")
                        }
                        
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.exportToPdf(context)
                                },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as CSV") },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.exportToCsv(context)
                                },
                                leadingIcon = { Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.error != null -> Text(uiState.error!!, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.scorecards) { scorecard ->
                            ScorecardItem(scorecard, onClick = { onEmployeeClick(scorecard.employeeId, scorecard.fullName) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScorecardItem(scorecard: IntegrityScorecard, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = scorecard.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Completed: ${scorecard.completedTasks}/${scorecard.totalTasks}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Surface(
                    color = getScoreColor(scorecard.attendanceRate),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${(scorecard.attendanceRate * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IntegrityStat(
                    label = "Avg. Deviation",
                    value = "${scorecard.averageIntegrityDistance.toInt()}m",
                    isWarning = scorecard.averageIntegrityDistance > 200
                )
                
                IntegrityStat(
                    label = "Integrity Flags",
                    value = scorecard.flagCount.toString(),
                    isWarning = scorecard.flagCount > 0
                )
            }
        }
    }
}

@Composable
fun IntegrityStat(label: String, value: String, isWarning: Boolean) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.ExtraBold,
                color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (isWarning) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun getScoreColor(rate: Float): Color {
    return when {
        rate >= 0.9f -> Color(0xFF4CAF50)
        rate >= 0.7f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}
