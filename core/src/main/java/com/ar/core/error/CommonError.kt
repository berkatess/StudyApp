package com.ar.core.error

sealed interface CommonError : AppError {
    data object NoInternet : CommonError
    data object SignInRequired : CommonError
    data object Unknown : CommonError
}