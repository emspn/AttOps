package com.app.attops.features.tasks.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.attops.core.common.result.Result
import com.app.attops.core.common.util.MapShareBus
import com.app.attops.core.location.LocationTracker
import com.app.attops.core.location.MapLinkResolver
import com.app.attops.core.location.ResolvedLocation
import com.app.attops.core.network.model.*
import com.app.attops.features.tasks.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskUiState(
    val userRole: UserRole? = null,
    val tasks: List<Task> = emptyList(),
    val employees: List<User> = emptyList(),
    val sites: List<OrganizationSite> = emptyList(),
    val attendance: TaskAttendance? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOperationSuccess: Boolean = false,
    val sharedLocation: ResolvedLocation? = null,
    val isCameraOpen: Boolean = false,
    val pendingAction: AttendanceAction? = null,
    val activeTaskId: String? = null,
    val activeTaskLat: Double? = null,
    val activeTaskLng: Double? = null
)

enum class AttendanceAction { CHECK_IN, CHECK_OUT }

sealed interface TaskUiEvent {
    data class ShowError(val message: String) : TaskUiEvent
    data object NavigateBack : TaskUiEvent
    data object ShowMapLocationPicked : TaskUiEvent
}

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val checkInUseCase: CheckInUseCase,
    private val checkOutUseCase: CheckOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getAssignableEmployeesUseCase: GetAssignableEmployeesUseCase,
    private val getSitesUseCase: GetSitesUseCase,
    private val createSiteUseCase: CreateSiteUseCase,
    private val getTaskAttendanceUseCase: GetTaskAttendanceUseCase,
    private val locationTracker: LocationTracker,
    private val mapShareBus: MapShareBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<TaskUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadUser()
        loadTasks()
        loadEmployees()
        loadSites()
        observeSharedLocation()
    }

    private fun observeSharedLocation() {
        viewModelScope.launch {
            mapShareBus.sharedLocation.collect { text ->
                _uiState.update { it.copy(isLoading = true) }
                val resolved = MapLinkResolver.resolve(text)
                if (resolved != null) {
                    _uiState.update { it.copy(isLoading = false, sharedLocation = resolved) }
                    _uiEvent.emit(TaskUiEvent.ShowMapLocationPicked)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(TaskUiEvent.ShowError("Unable to extract location from shared link."))
                }
                mapShareBus.clear()
            }
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _uiState.update { it.copy(userRole = user?.role) }
            }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            getTasksUseCase().collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update { it.copy(isLoading = false, tasks = result.data, error = null) }
                    is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun loadTaskDetails(taskId: String) {
        viewModelScope.launch {
            getTaskAttendanceUseCase(taskId).collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(attendance = result.data) }
                }
            }
        }
    }

    fun loadEmployees() {
        viewModelScope.launch {
            getAssignableEmployeesUseCase().collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(employees = result.data) }
                }
            }
        }
    }

    fun loadSites() {
        viewModelScope.launch {
            getSitesUseCase().collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(sites = result.data) }
                }
            }
        }
    }
    
    fun createSite(name: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            createSiteUseCase(name, lat, lng)
            loadSites()
        }
    }

    fun fetchCurrentLocation(onLocationFetched: (Double, Double) -> Unit) {
        viewModelScope.launch {
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                onLocationFetched(location.latitude, location.longitude)
            } else {
                _uiEvent.emit(TaskUiEvent.ShowError("Unable to fetch location. Please ensure GPS is enabled."))
            }
        }
    }

    fun createTask(
        title: String,
        description: String?,
        assignedTo: String?,
        priority: TaskPriority,
        locationName: String?,
        lat: Double?,
        lng: Double?,
        dueDate: String?,
        saveToRegistry: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            if (saveToRegistry && (lat != null) && (lng != null) && !locationName.isNullOrBlank()) {
                createSite(locationName, lat, lng)
            }

            val task = Task(
                organizationId = "", // Set in Repository
                title = title,
                description = description,
                createdBy = "", // Set in Repository
                assignedTo = assignedTo,
                priority = priority,
                locationName = locationName,
                latitude = lat,
                longitude = lng,
                dueDate = dueDate
            )
            
            when (val result = createTaskUseCase(task)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isOperationSuccess = true) }
                    _uiEvent.emit(TaskUiEvent.NavigateBack)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(TaskUiEvent.ShowError(result.message ?: "Failed to create task"))
                }
                else -> {}
            }
        }
    }

    fun requestCheckIn(taskId: String, lat: Double, lng: Double) {
        _uiState.update { it.copy(
            isCameraOpen = true,
            pendingAction = AttendanceAction.CHECK_IN,
            activeTaskId = taskId,
            activeTaskLat = lat,
            activeTaskLng = lng
        ) }
    }

    fun requestCheckOut(taskId: String) {
        _uiState.update { it.copy(
            isCameraOpen = true,
            pendingAction = AttendanceAction.CHECK_OUT,
            activeTaskId = taskId
        ) }
    }

    fun closeCamera() {
        _uiState.update { it.copy(isCameraOpen = false, pendingAction = null) }
    }

    fun handlePhotoCaptured(path: String) {
        val state = _uiState.value
        val action = state.pendingAction
        val taskId = state.activeTaskId ?: return
        
        _uiState.update { it.copy(isCameraOpen = false, isLoading = true) }
        
        viewModelScope.launch {
            val result = when (action) {
                AttendanceAction.CHECK_IN -> {
                    checkInUseCase(taskId, state.activeTaskLat ?: 0.0, state.activeTaskLng ?: 0.0, path)
                }
                AttendanceAction.CHECK_OUT -> {
                    checkOutUseCase(taskId, path)
                }
                else -> Result.Error(message = "Invalid action")
            }
            
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, pendingAction = null) }
                    loadTasks()
                    loadTaskDetails(taskId)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(TaskUiEvent.ShowError(result.message ?: "Action failed"))
                }
                else -> {}
            }
        }
    }

    fun clearSharedLocation() {
        _uiState.update { it.copy(sharedLocation = null) }
    }
}
