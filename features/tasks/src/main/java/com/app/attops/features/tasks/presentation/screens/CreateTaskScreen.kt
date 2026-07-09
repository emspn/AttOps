package com.app.attops.features.tasks.presentation.screens

import android.Manifest
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var priorityExpanded by remember { mutableStateOf(value = false) }

    var assignedEmployeeId by remember { mutableStateOf<String?>(null) }
    var assignedEmployeeName by remember { mutableStateOf("Unassigned") }
    var employeeExpanded by remember { mutableStateOf(false) }

    // Deadline State
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    // Search Feature
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
                snackbarHostState.showSnackbar("Location permission is required to fetch your coordinates.")
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
                } else {
                    viewModel.viewModelScope.launch {
                        snackbarHostState.showSnackbar("No locations found.")
                    }
                }
            } catch (e: Exception) {
                viewModel.viewModelScope.launch {
                    snackbarHostState.showSnackbar("Search error: ${e.message}")
                }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Task") },
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
                value = title,
                onValueChange = { title = it },
                label = "Task Title (e.g. Repair AC)"
            )

            AttOpsTextField(
                value = description,
                onValueChange = { description = it },
                label = "Work Description",
                singleLine = false
            )

            // Employee Assignment
            ExposedDropdownMenuBox(
                expanded = employeeExpanded,
                onExpandedChange = { employeeExpanded = !employeeExpanded }
            ) {
                OutlinedTextField(
                    value = assignedEmployeeName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assign To") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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

            HorizontalDivider()

            Text(text = "Deadline Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Date", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = selectedDate?.toString() ?: "Pick Date",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                OutlinedCard(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Time", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = selectedTime?.toString() ?: "Pick Time",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(text = "Work Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            AttOpsTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = "Site Name or Address"
            )

            if (latitude == null) {
                OutlinedButton(
                    onClick = { showSearchDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Attach GPS Location (Optional)")
                }
            } else {
                InputChip(
                    selected = true,
                    onClick = { 
                        latitude = null
                        longitude = null
                    },
                    label = { Text("GPS Location Attached") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp)) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            // Priority
            ExposedDropdownMenuBox(
                expanded = priorityExpanded,
                onExpandedChange = { priorityExpanded = !priorityExpanded }
            ) {
                OutlinedTextField(
                    value = priority.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Priority") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = priorityExpanded,
                    onDismissRequest = { priorityExpanded = false }
                ) {
                    TaskPriority.entries.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = {
                                priority = p
                                priorityExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AttOpsPrimaryButton(
                text = "Create & Assign Task",
                onClick = {
                    val deadlineStr = if (selectedDate != null && selectedTime != null) {
                        LocalDateTime.of(selectedDate, selectedTime)
                            .atZone(ZoneId.systemDefault())
                            .toOffsetDateTime()
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    } else null

                    viewModel.createTask(
                        title = title,
                        description = description.ifBlank { null },
                        assignedTo = assignedEmployeeId,
                        priority = priority,
                        locationName = locationName,
                        lat = latitude,
                        lng = longitude,
                        dueDate = deadlineStr
                    )
                },
                enabled = !uiState.isLoading && title.isNotBlank() && locationName.isNotBlank()
            )
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Select Time", style = MaterialTheme.typography.titleLarge)
                    TimePicker(state = timePickerState)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }

    if (showSearchDialog) {
        Dialog(onDismissRequest = { showSearchDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Search Site Location", style = MaterialTheme.typography.titleLarge)
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Enter address...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { performSearch() }) {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (showSearchResults) {
                        Text("Search Results", style = MaterialTheme.typography.labelMedium)
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(searchResults) { address ->
                                Surface(
                                    onClick = {
                                        latitude = address.latitude
                                        longitude = address.longitude
                                        locationName = address.getAddressLine(0) ?: locationName
                                        showSearchResults = false
                                        showSearchDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = address.getAddressLine(0) ?: "Unknown Location",
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use My Current Location")
                    }
                }
            }
        }
    }
}
