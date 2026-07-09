package com.app.attops.features.tasks.domain.usecase

import com.app.attops.features.tasks.domain.repository.TaskRepository
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke() = repository.getTasks()
}
