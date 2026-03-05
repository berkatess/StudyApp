package com.ar.domain.auth.error

import com.ar.core.error.AppError

sealed interface AuthError : AppError {
    data object EmptyGoogleIdToken : AuthError
    data object GoogleSignInFailed : AuthError
    data object SignOutFailed : AuthError
    data object DeleteAccountFailed : AuthError
    data object MissingWebClientId : AuthError
    data object GoogleIdTokenFetchFailed : AuthError
    data object GoogleIdTokenEmpty : AuthError
}