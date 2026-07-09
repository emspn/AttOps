package com.app.attops.features.tasks.domain.usecase

import com.app.attops.core.common.result.Result
import com.app.attops.core.location.LocationTracker
import com.app.attops.core.location.LocationUtils
import com.app.attops.features.tasks.domain.repository.TaskRepository
import javax.inject.Inject

class CheckInUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val locationTracker: LocationTracker
) {
    suspend operator fun invoke(
        taskId: String,
        taskLat: Double,
        taskLng: Double,
        imagePath: String? = null
    ): Result<Unit> {
        val currentLocation = locationTracker.getCurrentLocation()
            ?: return Result.Error(message = "Unable to get current location. Ensure GPS is enabled.")

        val distance = LocationUtils.calculateDistance(
            taskLat, taskLng,
            currentLocation.latitude, currentLocation.longitude
        )

        // We capture the real distance, but NO LONGER block the employee.
        // This ensures the app is flexible and reliable in the field.
        return repository.checkIn(
            taskId = taskId,
            latitude = currentLocation.latitude,
            longitude = currentLocation.longitude,
            distance = distance,
            imagePath = imagePath
        )
    }
}
