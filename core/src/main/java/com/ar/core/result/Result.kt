package com.ar.core.result

import com.ar.core.error.AppError

sealed class Result<out T> {

    data class Success<T>(val data: T) : Result<T>()

    data class Error(
        val message: String? = null,
        val throwable: Throwable? = null,
        val error: AppError? = null
    ) : Result<Nothing>()

    object Loading : Result<Nothing>()
}