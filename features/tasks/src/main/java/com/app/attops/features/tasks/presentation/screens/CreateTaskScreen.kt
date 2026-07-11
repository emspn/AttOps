package com.app.attops.features.tasks.presentation.screens

import android.Manifest
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import com.app.attops.core.designsystem.components.AttOpsPrimaryButton
import com.app.attops.core.designsystem.components.AttOpsTextField
import com.app.attops.core.network.model.TaskPriority
import com.app.attops.features.tasks.presentation.viewmodel.TaskUiEvent
import com.app.attops.features.tasks.presentation.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    viewModel: TaskViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }

    var assignedEmployeeId by remember { mutableStateOf<String?>(null) }
    var assignedEmployeeName by remember { mutableStateOf("Unassigned") }
    var employeeExpanded by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                     permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchCurrentLocation { lat, lng ->
                latitude = lat
                longitude = lng
                showSearchDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Current location captured.")
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission required.")
            }
        }
    }

    fun performSearch() {
        if (searchQuery.isBlank()) return
        isSearching = true
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(searchQuery, 5)
                if (!results.isNullOrEmpty()) {
                    searchResults = results
                    showSearchResults = true
                }
            } catch (e: Exception) {
                // handle error
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is TaskUiEvent.NavigateBack -> onBackClick()
                is TaskUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Task", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            
            AttOpsTextField(
                value = title,
                onValueChange = { title = it },
                label = "Title",
                placeholder = "Task name"
            )

            AttOpsTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                singleLine = false,
                placeholder = "Instructions..."
            )

            // Assignment
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Assign To", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = employeeExpanded,
                    onExpandedChange = { employeeExpanded = !employeeExpanded }
                ) {
                    OutlinedTextField(
                        value = assignedEmployeeName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = employeeExpanded,
                        onDismissRequest = { employeeExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unassigned") },
                            onClick = {
                                assignedEmployeeId = null
                                assignedEmployeeName = "Unassigned"
                                employeeExpanded = false
                            }
                        )
                        uiState.employees.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text("${emp.name} (${emp.employeeId})") },
                                onClick = {
                                    assignedEmployeeId = emp.id
                                    assignedEmployeeName = emp.name
                                    employeeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Deadline
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Deadline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Date", style = MaterialTheme.typography.labelSmall)
                            Text(selectedDate?.toString() ?: "Set Date", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Time", style = MaterialTheme.typography.labelSmall)
                            Text(selectedTime?.toString() ?: "Set Time", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Location
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                AttOpsTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = "Site Location"
                )
                if (latitude == null) {
                    OutlinedButton(
                        onClick = { showSearchDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddLocation, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Attach GPS (Optional)")
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Location Linked", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { latitude = null; longitude = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Priority
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Priority", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.name, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val deadlineStr = if (selectedDate != null && selectedTime != null) {
                        LocalDateTime.of(selectedDate, selectedTime).atZone(ZoneId.systemDefault()).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    } else null

                    viewModel.createTask(title, description.ifBlank { null }, assignedEmployeeId, priority, locationName, latitude, longitude, deadlineStr)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isLoading && title.isNotBlank() && locationName.isNotBlank()
            ) {
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Create & Assign Task", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                showDatePicker = false
            }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("Select Deadline Time", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    TimePicker(state = timePickerState)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text("Set Time") }
                    }
                }
            }
        }
    }

    if (showSearchDialog) {
        Dialog(onDismissRequest = { showSearchDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Pin Site Location", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search address...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            else IconButton(onClick = { performSearch() }) { Icon(Icons.Default.Search, null) }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (showSearchResults) {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(searchResults) { address ->
                                Surface(
                                    onClick = {
                                        latitude = address.latitude
                                        longitude = address.longitude
                                        locationName = address.getAddressLine(0) ?: locationName
                                        showSearchDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = address.getAddressLine(0) ?: "",
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Use My Location")
                    }
                }
            }
        }
    }
}
