package com.app.attops.core.common.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshBus @Inject constructor() {
    private val _refreshEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshEvent = _refreshEvent.asSharedFlow()

    fun trigger() {
        _refreshEvent.tryEmit(Unit)
    }
}
