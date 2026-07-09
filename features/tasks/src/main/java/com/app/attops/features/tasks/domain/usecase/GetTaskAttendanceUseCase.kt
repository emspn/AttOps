package com.app.attops.features.tasks.domain.usecase

import com.app.attops.features.tasks.domain.repository.TaskRepository
import javax.inject.Inject

class GetTaskAttendanceUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(taskId: String) = repository.getTaskAttendance(taskId)
}
