package com.app.attops.core.common.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionBus @Inject constructor() {
    private val _signOutEvent = MutableSharedFlow<Unit>()
    val signOutEvent = _signOutEvent.asSharedFlow()

    fun triggerSignOut() {
        _signOutEvent.tryEmit(Unit)
    }
}
