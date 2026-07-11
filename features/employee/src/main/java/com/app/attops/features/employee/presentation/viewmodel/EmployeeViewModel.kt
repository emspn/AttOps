package com.app.attops.features.employee.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.attops.core.common.result.Result
import com.app.attops.core.common.util.RefreshBus
import com.app.attops.core.network.model.User
import com.app.attops.core.network.model.UserRole
import com.app.attops.core.network.model.UserStatus
import com.app.attops.features.employee.presentation.state.EmployeeSortOrder
import com.app.attops.features.employee.presentation.state.EmployeeUiEvent
import com.app.attops.features.employee.presentation.state.EmployeeUiState
import com.app.attops.features.employee.usecase.AddEmployeeUseCase
import com.app.attops.features.employee.usecase.DeleteEmployeeUseCase
import com.app.attops.features.employee.usecase.GetCurrentUserUseCase
import com.app.attops.features.employee.usecase.GetEmployeeUseCase
import com.app.attops.features.employee.usecase.GetEmployeesUseCase
import com.app.attops.features.employee.usecase.UpdateEmployeeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmployeeViewModel @Inject constructor(
    private val getEmployeesUseCase: GetEmployeesUseCase,
    private val getEmployeeUseCase: GetEmployeeUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val addEmployeeUseCase: AddEmployeeUseCase,
    private val updateEmployeeUseCase: UpdateEmployeeUseCase,
    private val deleteEmployeeUseCase: DeleteEmployeeUseCase,
    private val refreshBus: RefreshBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<EmployeeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var allEmployees = listOf<User>()

    init {
        observeCurrentUser()
        observeEmployees()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _uiState.update { it.copy(currentUserRole = user?.role) }
            }
        }
    }

    private fun observeEmployees() {
        viewModelScope.launch {
            getEmployeesUseCase()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { employees ->
                    allEmployees = employees
                    filterEmployees(_uiState.value.searchQuery)
                }
        }
    }

    fun loadEmployees() {
        refreshBus.trigger()
    }

    fun loadEmployee(id: String) {
        viewModelScope.launch {
            getEmployeeUseCase(id)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { employee ->
                    _uiState.update { it.copy(isLoading = false, selectedEmployee = employee) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterEmployees(query)
    }

    fun onSortOrderChange(order: EmployeeSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        filterEmployees(_uiState.value.searchQuery)
    }

    private fun filterEmployees(query: String) {
        val state = _uiState.value
        var filteredList = if (query.isBlank()) {
            allEmployees
        } else {
            allEmployees.filter {
                it.name.contains(query, ignoreCase = true) ||
                        (it.employeeId?.contains(query, ignoreCase = true) ?: false)
            }
        }

        // Apply Sorting
        filteredList = when (state.sortOrder) {
            EmployeeSortOrder.NAME_ASC -> filteredList.sortedBy { it.name }
            EmployeeSortOrder.NAME_DESC -> filteredList.sortedByDescending { it.name }
            EmployeeSortOrder.ID_ASC -> filteredList.sortedBy { it.employeeId ?: "" }
            EmployeeSortOrder.DESIGNATION -> filteredList.sortedBy { it.designation ?: "" }
        }

        _uiState.update { it.copy(isLoading = false, employees = filteredList) }
    }

    fun addEmployee(
        employeeId: String,
        name: String,
        email: String?,
        phone: String?,
        department: String?,
        designation: String?,
        role: UserRole,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val newUser = User(
                id = java.util.UUID.randomUUID().toString(),
                organizationId = "", 
                employeeId = employeeId,
                name = name,
                email = email,
                phone = phone,
                department = department,
                designation = designation,
                role = role,
                status = UserStatus.ACTIVE
            )
            
            when (val result = addEmployeeUseCase(newUser, password)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, operationSuccess = true) }
                    _uiEvent.emit(EmployeeUiEvent.NavigateBack)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                    _uiEvent.emit(EmployeeUiEvent.ShowError(result.message ?: "Failed to add employee"))
                }
                else -> {}
            }
        }
    }

    fun updateEmployee(user: User) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = updateEmployeeUseCase(user)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, operationSuccess = true) }
                    _uiEvent.emit(EmployeeUiEvent.NavigateBack)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                    _uiEvent.emit(EmployeeUiEvent.ShowError(result.message ?: "Failed to update employee"))
                }
                else -> {}
            }
        }
    }

    fun deleteEmployee(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = deleteEmployeeUseCase(id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEvent.emit(EmployeeUiEvent.NavigateBack)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                    _uiEvent.emit(EmployeeUiEvent.ShowError(result.message ?: "Failed to delete employee"))
                }
                else -> {}
            }
        }
    }
}
