package com.app.attops.features.employee.repository

import com.app.attops.core.common.result.Result
import com.app.attops.core.network.model.User
import kotlinx.coroutines.flow.Flow

interface EmployeeRepository {
    fun getEmployees(): Flow<List<User>>
    fun getEmployee(id: String): Flow<User?>
    fun getCurrentUser(): Flow<User?>
    
    // Kept for backward compatibility and to resolve build errors, 
    // but marked as deprecated in favor of local filtering in ViewModel.
    @Deprecated("Use local filtering in ViewModel")
    fun searchEmployees(query: String): Flow<List<User>>

    suspend fun addEmployee(employee: User, temporaryPassword: String): Result<Unit>
    suspend fun updateEmployee(employee: User): Result<Unit>
    suspend fun deleteEmployee(id: String): Result<Unit>
}
