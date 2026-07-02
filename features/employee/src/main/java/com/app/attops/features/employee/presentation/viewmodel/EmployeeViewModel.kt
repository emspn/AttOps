package com.app.attops.features.employee.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.User
import com.app.attops.core.network.model.UserRole
import com.app.attops.core.network.model.UserStatus
import com.app.attops.features.employee.presentation.state.EmployeeUiEvent
import com.app.attops.features.employee.presentation.state.EmployeeUiState
import com.app.attops.features.employee.usecase.AddEmployeeUseCase
import com.app.attops.features.employee.usecase.DeleteEmployeeUseCase
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
    private val addEmployeeUseCase: AddEmployeeUseCase,
    private val updateEmployeeUseCase: UpdateEmployeeUseCase,
    private val deleteEmployeeUseCase: DeleteEmployeeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<EmployeeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var allEmployees = listOf<User>()

    init {
        loadEmployees()
    }

    fun loadEmployees() {
        viewModelScope.launch {
            getEmployeesUseCase()
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { employees ->
                    allEmployees = employees
                    filterEmployees(_uiState.value.searchQuery)
                }
        }
    }

    fun loadEmployee(id: String) {
        viewModelScope.launch {
            getEmployeeUseCase(id)
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
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

    private fun filterEmployees(query: String) {
        val filteredList = if (query.isBlank()) {
            allEmployees
        } else {
            allEmployees.filter {
                it.name.contains(query, ignoreCase = true) ||
                        (it.employeeId?.contains(query, ignoreCase = true) ?: false)
            }
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
                organizationId = "", // Scoped in Repository
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
                    loadEmployees()
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
                    loadEmployees()
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
                    loadEmployees()
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
