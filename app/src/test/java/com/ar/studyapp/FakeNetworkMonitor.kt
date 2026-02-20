package com.ar.studyapp

import com.ar.core.network.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNetworkMonitor(
    initialOnline: Boolean = true
) : NetworkMonitor {

    private val onlineFlow = MutableStateFlow(initialOnline)

    override val isOnline: Flow<Boolean> = onlineFlow

    override suspend fun isOnlineNow(): Boolean = onlineFlow.value

    fun setOnline(value: Boolean) {
        onlineFlow.value = value
    }
}
