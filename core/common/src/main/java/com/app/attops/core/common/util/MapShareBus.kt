package com.app.attops.core.common.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A simple bus to communicate shared location data from MainActivity to ViewModels.
 */
@Singleton
class MapShareBus @Inject constructor() {
    private val _sharedLocation = MutableSharedFlow<String>(replay = 1)
    val sharedLocation = _sharedLocation.asSharedFlow()

    suspend fun postLocation(text: String) {
        _sharedLocation.emit(text)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() {
        _sharedLocation.resetReplayCache()
    }
}
