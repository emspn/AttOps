package com.app.attops.features.tasks.domain.usecase

import com.app.attops.core.common.result.Result
import com.app.attops.core.location.LocationTracker
import com.app.attops.features.tasks.domain.repository.TaskRepository
import javax.inject.Inject

class CheckOutUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val locationTracker: LocationTracker
) {
    suspend operator fun invoke(
        taskId: String,
        imagePath: String? = null
    ): Result<Unit> {
        val currentLocation = locationTracker.getCurrentLocation()
            ?: return Result.Error(message = "Unable to get current location.")

        return repository.checkOut(
            taskId = taskId,
            latitude = currentLocation.latitude,
            longitude = currentLocation.longitude,
            imagePath = imagePath
        )
    }
}
