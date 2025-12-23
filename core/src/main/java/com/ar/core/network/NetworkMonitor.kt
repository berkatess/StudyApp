package com.ar.core.network

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
    suspend fun isOnlineNow(): Boolean
}
