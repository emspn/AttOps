package com.app.attops.features.employee.presentation.state

import com.app.attops.core.network.model.User
import com.app.attops.core.network.model.UserRole

data class EmployeeUiState(
    val employees: List<User> = emptyList(),
    val selectedEmployee: User? = null,
    val currentUserRole: UserRole? = null,
    val searchQuery: String = "",
    val sortOrder: EmployeeSortOrder = EmployeeSortOrder.NAME_ASC,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val operationSuccess: Boolean = false
)

enum class EmployeeSortOrder {
    NAME_ASC,
    NAME_DESC,
    ID_ASC,
    DESIGNATION
}

sealed interface EmployeeUiEvent {
    data class ShowError(val message: String) : EmployeeUiEvent
    data object NavigateBack : EmployeeUiEvent
}
