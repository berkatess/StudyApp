package com.ar.domain.error

import com.ar.core.error.AppError

sealed interface CategoryError : AppError {

    // Validation (use case)
    data object EmptyCategoryId : CategoryError
    data object EmptyCategoryName : CategoryError

    // Repository / data flow
    data object RefreshFailed : CategoryError
    data object NotFoundLocal : CategoryError
    data object NotFound : CategoryError
    data object LoadFailedSingle : CategoryError
    data object LoadFailedList : CategoryError
    data object CreateFailed : CategoryError
    data object UpdateFailed : CategoryError
    data object DeleteFailed : CategoryError
    data object LoadLocalFailed : CategoryError
    data object ObserveRemoteFailed : CategoryError
    data object SignInRequiredForRemoteFetch : CategoryError
    data object LoadRemoteFailed : CategoryError
    data object LoadFallbackFailed : CategoryError
    data object SyncFailed : CategoryError
}