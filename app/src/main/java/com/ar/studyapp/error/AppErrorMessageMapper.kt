package com.ar.studyapp.error

import androidx.annotation.StringRes
import com.ar.core.error.AppError
import com.ar.domain.error.CategoryError
import com.ar.domain.error.NoteError
import com.ar.core.error.CommonError
import com.ar.domain.auth.error.AuthError
import com.ar.studyapp.R

@StringRes
fun AppError?.toMessageResOrNull(): Int? = when (this) {
    null -> null

    // NOTE
    NoteError.EmptyNoteId -> R.string.note_error_empty_id
    NoteError.EmptyTitleAndContent -> R.string.note_error_empty_title_and_content
    NoteError.NoteNotFound -> R.string.note_error_not_found
    NoteError.LoadNotesLocalFailed -> R.string.note_error_load_local_failed
    NoteError.LoadNotesRemoteFailed -> R.string.note_error_load_remote_failed
    NoteError.LoadNotesFallbackFailed -> R.string.note_error_load_fallback_failed
    NoteError.NoInternetConnection -> R.string.note_error_no_internet
    NoteError.SyncNotesFailed -> R.string.note_error_sync_failed
    NoteError.SaveNoteLocalFailed -> R.string.note_error_save_local_failed
    NoteError.UpdateNoteFailed -> R.string.note_error_update_failed
    NoteError.DeleteNoteFailed -> R.string.note_error_delete_failed
    NoteError.RefreshNotesFailed -> R.string.note_error_refresh_failed
    NoteError.SignInRequiredForRemoteFetch -> R.string.note_error_sign_in_required_remote_fetch

    CategoryError.RefreshFailed -> R.string.category_error_refresh_failed
    CategoryError.NotFoundLocal -> R.string.category_error_not_found_local
    CategoryError.NotFound -> R.string.category_error_not_found
    CategoryError.LoadFailedSingle -> R.string.category_error_load_failed_single
    CategoryError.LoadFailedList -> R.string.category_error_load_failed
    CategoryError.CreateFailed -> R.string.category_error_create_failed
    CategoryError.UpdateFailed -> R.string.category_error_update_failed
    CategoryError.DeleteFailed -> R.string.category_error_delete_failed
    CategoryError.LoadLocalFailed -> R.string.category_error_load_local_failed
    CategoryError.ObserveRemoteFailed -> R.string.category_error_observe_remote_failed
    CategoryError.SignInRequiredForRemoteFetch -> R.string.category_error_sign_in_required_remote_fetch
    CategoryError.LoadRemoteFailed -> R.string.category_error_load_remote_failed
    CategoryError.LoadFallbackFailed -> R.string.category_error_load_fallback_failed
    CategoryError.SyncFailed -> R.string.category_error_sync_failed

    // COMMON
    CommonError.NoInternet -> R.string.common_error_no_internet
    CommonError.SignInRequired -> R.string.common_error_sign_in_required
    CommonError.Unknown -> R.string.common_error_unknown

    // AUTH
    AuthError.EmptyGoogleIdToken -> R.string.auth_error_empty_google_id_token
    AuthError.GoogleSignInFailed -> R.string.auth_error_google_sign_in_failed
    AuthError.SignOutFailed -> R.string.auth_error_sign_out_failed
    AuthError.DeleteAccountFailed -> R.string.auth_error_delete_account_failed
    AuthError.MissingWebClientId -> R.string.auth_error_missing_web_client_id
    AuthError.GoogleIdTokenFetchFailed -> R.string.auth_error_google_id_token_fetch_failed
    AuthError.GoogleIdTokenEmpty -> R.string.auth_error_google_id_token_empty

    else -> null
}