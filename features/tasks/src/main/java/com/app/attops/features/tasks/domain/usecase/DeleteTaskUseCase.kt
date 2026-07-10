package com.app.attops.features.tasks.domain.usecase

import com.app.attops.core.common.result.Result
import com.app.attops.features.tasks.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> = repository.deleteTask(taskId)
}
