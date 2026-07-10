package com.app.attops.features.tasks.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.attops.core.common.result.Result
import com.app.attops.core.common.util.MapShareBus
import com.app.attops.core.common.util.RefreshBus
import com.app.attops.core.location.LocationTracker
import com.app.attops.core.location.MapLinkResolver
import com.app.attops.core.location.ResolvedLocation
import com.app.attops.core.network.model.*
import com.app.attops.features.tasks.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskUiState(
    val userRole: UserRole? = null,
    val tasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val employees: List<User> = emptyList(),
    val sites: List<OrganizationSite> = emptyList(),
    val attendance: TaskAttendance? = null,
    val isLoading: Boolean = false,
    val isPerformingAttendance: Boolean = false,
    val localStatusOverrides: Map<String, TaskStatus> = emptyMap(),
    val localAttendanceOverrides: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val sortOrder: TaskSortOrder = TaskSortOrder.RECENTLY_ADDED,
    val error: String? = null,
    val isOperationSuccess: Boolean = false,
    val sharedLocation: ResolvedLocation? = null,
    val isCameraOpen: Boolean = false,
    val pendingAction: AttendanceAction? = null,
    val activeTaskId: String? = null,
    val activeTaskLat: Double? = null,
    val activeTaskLng: Double? = null
)

enum class TaskSortOrder {
    RECENTLY_ADDED,
    DUE_DATE_ASC,
    DUE_DATE_DESC,
    PRIORITY_HIGH,
    PRIORITY_LOW
}

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
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val checkInUseCase: CheckInUseCase,
    private val checkOutUseCase: CheckOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getAssignableEmployeesUseCase: GetAssignableEmployeesUseCase,
    private val getSitesUseCase: GetSitesUseCase,
    private val createSiteUseCase: CreateSiteUseCase,
    private val getTaskAttendanceUseCase: GetTaskAttendanceUseCase,
    private val locationTracker: LocationTracker,
    private val mapShareBus: MapShareBus,
    private val refreshBus: RefreshBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<TaskUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var currentUserId: String? = null
    private var attendanceJob: Job? = null

    init {
        loadUser()
        observeTasks()
        observeEmployees()
        observeSites()
        observeSharedLocation()
        observeSyncStatus()
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            getTasksUseCase.getPendingSyncCount().collect { count ->
                if (count == 0) {
                    refreshBus.trigger()
                }
            }
        }
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
                currentUserId = user?.id
                _uiState.update { it.copy(userRole = user?.role) }
            }
        }
    }

    private fun observeTasks() {
        viewModelScope.launch {
            getTasksUseCase().collect { result ->
                when (result) {
                    is Result.Loading -> {
                        if (!_uiState.value.isPerformingAttendance) {
                             _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                    is Result.Success -> {
                        val serverTasks = result.data
                        val currentOverrides = _uiState.value.localStatusOverrides
                        
                        val reconciledTasks = serverTasks.map { task ->
                            val overriddenStatus = currentOverrides[task.id]
                            if (overriddenStatus != null) {
                                task.copy(status = overriddenStatus)
                            } else {
                                task
                            }
                        }

                        val remainingOverrides = currentOverrides.filter { (id, status) ->
                            serverTasks.find { it.id == id }?.status != status
                        }

                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                tasks = reconciledTasks,
                                localStatusOverrides = remainingOverrides,
                                error = null
                            ) 
                        }
                        applyFilters()
                    }
                    is Result.Error -> _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            isPerformingAttendance = false, 
                            error = result.message 
                        ) 
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onSortOrderChange(order: TaskSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var list = state.tasks

        // Search
        if (state.searchQuery.isNotBlank()) {
            list = list.filter { 
                it.title.contains(state.searchQuery, ignoreCase = true) ||
                (it.description?.contains(state.searchQuery, ignoreCase = true) ?: false) ||
                (it.locationName?.contains(state.searchQuery, ignoreCase = true) ?: false)
            }
        }

        // Sort
        list = when (state.sortOrder) {
            TaskSortOrder.RECENTLY_ADDED -> list.sortedByDescending { it.createdAt }
            TaskSortOrder.DUE_DATE_ASC -> list.sortedBy { it.dueDate ?: "9999" }
            TaskSortOrder.DUE_DATE_DESC -> list.sortedByDescending { it.dueDate ?: "" }
            TaskSortOrder.PRIORITY_HIGH -> list.sortedByDescending { it.priority }
            TaskSortOrder.PRIORITY_LOW -> list.sortedBy { it.priority }
        }

        _uiState.update { it.copy(filteredTasks = list) }
    }

    fun loadTasks() {
        refreshBus.trigger()
    }

    fun loadTaskDetails(taskId: String) {
        _uiState.update { it.copy(activeTaskId = taskId) }
        attendanceJob?.cancel()
        attendanceJob = viewModelScope.launch {
            getTaskAttendanceUseCase(taskId).collect { result ->
                if (result is Result.Success) {
                    val serverAttendance = result.data
                    val override = _uiState.value.localAttendanceOverrides[taskId]
                    
                    if (override != null) {
                        if (serverAttendance?.status == override) {
                            _uiState.update { it.copy(localAttendanceOverrides = it.localAttendanceOverrides - taskId) }
                        }
                        _uiState.update { it.copy(attendance = serverAttendance?.copy(status = override) ?: it.attendance) }
                    } else {
                        _uiState.update { it.copy(attendance = serverAttendance) }
                    }
                }
            }
        }
    }

    private fun observeEmployees() {
        viewModelScope.launch {
            getAssignableEmployeesUseCase().collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(employees = result.data) }
                }
            }
        }
    }

    private fun observeSites() {
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

    fun canDeleteTask(task: Task?): Boolean {
        val userRole = _uiState.value.userRole ?: return false
        if (task == null) return false
        
        return when (userRole) {
            UserRole.OWNER -> true
            UserRole.ADMIN -> task.createdBy == currentUserId
            UserRole.EMPLOYEE -> false
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = deleteTaskUseCase(taskId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    loadTasks()
                    _uiEvent.emit(TaskUiEvent.NavigateBack)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(TaskUiEvent.ShowError(result.message ?: "Failed to delete task"))
                }
                else -> {}
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
                organizationId = "", 
                title = title,
                description = description,
                createdBy = "", 
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
        
        if (state.isPerformingAttendance) return
        
        _uiState.update { it.copy(isCameraOpen = false, isPerformingAttendance = true) }
        
        viewModelScope.launch {
            val result = when (action) {
                AttendanceAction.CHECK_IN -> checkInUseCase(taskId, path)
                AttendanceAction.CHECK_OUT -> checkOutUseCase(taskId, path)
                else -> Result.Error(message = "Invalid action")
            }
            
            when (result) {
                is Result.Success -> {
                    val nextStatus = if (action == AttendanceAction.CHECK_IN) TaskStatus.IN_PROGRESS else TaskStatus.COMPLETED
                    val attendanceStatus = if (action == AttendanceAction.CHECK_IN) "CHECKED_IN" else "CHECKED_OUT"
                    
                    _uiState.update { 
                        val updatedOverrides = it.localStatusOverrides + (taskId to nextStatus)
                        val updatedAttOverrides = it.localAttendanceOverrides + (taskId to attendanceStatus)
                        
                        val updatedTasks = it.tasks.map { task ->
                            if (task.id == taskId) task.copy(status = nextStatus) else task 
                        }
                        
                        val updatedAttendance = it.attendance?.copy(status = attendanceStatus) 
                            ?: TaskAttendance(
                                organizationId = it.tasks.find { t -> t.id == taskId }?.organizationId ?: "",
                                taskId = taskId,
                                employeeId = currentUserId ?: "",
                                status = attendanceStatus
                            )

                        it.copy(
                            tasks = updatedTasks, 
                            attendance = updatedAttendance,
                            localStatusOverrides = updatedOverrides,
                            localAttendanceOverrides = updatedAttOverrides,
                            pendingAction = null
                        )
                    }

                    refreshBus.trigger()
                    
                    // CRITICAL: Delay releasing the lock to ensure UI has recomposed with the new state
                    delay(500)
                    _uiState.update { it.copy(isPerformingAttendance = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isPerformingAttendance = false) }
                    _uiEvent.emit(TaskUiEvent.ShowError(result.message ?: "Action failed"))
                }
                else -> {
                    _uiState.update { it.copy(isPerformingAttendance = false) }
                }
            }
        }
    }

    fun clearSharedLocation() {
        _uiState.update { it.copy(sharedLocation = null) }
    }
}
