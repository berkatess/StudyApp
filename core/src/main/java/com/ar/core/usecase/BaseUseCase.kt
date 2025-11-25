package com.ar.core.usecase

import com.ar.core.result.Result

abstract class BaseUseCase<in Params, out T> {

    suspend operator fun invoke(params: Params): Result<T> {
        return try {
            execute(params)
        } catch (e: Exception) {
            Result.Error(e.message, e)
        }
    }

    protected abstract suspend fun execute(params: Params): Result<T>
}