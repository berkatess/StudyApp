package com.ar.studyapp.core.coroutines

import kotlinx.coroutines.Dispatchers

class DefaultDispatcherProvider : DispatcherProvider {
    override val io = Dispatchers.IO
    override val main = Dispatchers.Main
    override val default = Dispatchers.Default
}