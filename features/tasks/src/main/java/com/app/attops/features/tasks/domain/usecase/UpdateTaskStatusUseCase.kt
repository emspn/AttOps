package com.app.attops.features.tasks.domain.usecase

import com.app.attops.core.network.model.TaskStatus
import com.app.attops.features.tasks.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskStatusUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, status: TaskStatus) = 
        repository.updateTaskStatus(taskId, status)
}
