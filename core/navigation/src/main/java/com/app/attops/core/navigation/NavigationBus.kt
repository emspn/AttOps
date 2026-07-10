package com.app.attops.core.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationBus @Inject constructor() {
    private val _events = MutableSharedFlow<Destination>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun navigateTo(destination: Destination) {
        _events.tryEmit(destination)
    }
}
