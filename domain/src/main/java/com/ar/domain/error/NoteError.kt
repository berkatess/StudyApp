package com.ar.domain.error

import com.ar.core.error.AppError

sealed interface NoteError : AppError {

    // Validation (use case)
    data object EmptyNoteId : NoteError
    data object EmptyTitleAndContent : NoteError

    // Repository / data flow
    data object NoteNotFound : NoteError
    data object LoadNotesLocalFailed : NoteError
    data object LoadNotesRemoteFailed : NoteError
    data object LoadNotesFallbackFailed : NoteError
    data object NoInternetConnection : NoteError
    data object SyncNotesFailed : NoteError
    data object SaveNoteLocalFailed : NoteError
    data object UpdateNoteFailed : NoteError
    data object DeleteNoteFailed : NoteError
    data object RefreshNotesFailed : NoteError
    data object SignInRequiredForRemoteFetch : NoteError
}