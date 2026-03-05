package com.ar.studyapp.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.core.result.Result
import com.ar.domain.auth.usecase.DeleteAccountUseCase
import com.ar.domain.auth.usecase.ObserveGoogleUserUseCase
import com.ar.domain.auth.usecase.SignInWithGoogleIdTokenUseCase
import com.ar.domain.auth.usecase.SignOutUseCase
import com.ar.studyapp.R
import com.ar.studyapp.error.toMessageResOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsAuthUiState(
    val isInProgress: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)

/**
 * Settings screen ViewModel.
 *
 * Language is NOT handled here. The app uses the device locale automatically
 * via Android string resources (values/, values-tr/, ...).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeGoogleUserUseCase: ObserveGoogleUserUseCase,
    private val signInWithGoogleIdTokenUseCase: SignInWithGoogleIdTokenUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {

    val user = observeGoogleUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _authUiState = MutableStateFlow(SettingsAuthUiState())
    val authUiState: StateFlow<SettingsAuthUiState> = _authUiState

    fun reportAuthError(@StringRes messageRes: Int) {
        _authUiState.value = SettingsAuthUiState(
            isInProgress = false,
            errorMessageRes = messageRes
        )
    }

    fun clearAuthError() {
        _authUiState.value = _authUiState.value.copy(errorMessageRes = null)
    }

    fun signInWithGoogleIdToken(idToken: String) = viewModelScope.launch {
        _authUiState.value = SettingsAuthUiState(isInProgress = true)

        when (val result = signInWithGoogleIdTokenUseCase(idToken)) {
            is Result.Success -> _authUiState.value = SettingsAuthUiState(isInProgress = false)

            is Result.Error -> _authUiState.value = SettingsAuthUiState(
                isInProgress = false,
                errorMessageRes = result.error.toMessageResOrNull()
                    ?: R.string.auth_error_google_sign_in_failed
            )

            Result.Loading -> Unit
        }
    }

    fun signOut() = viewModelScope.launch {
        _authUiState.value = SettingsAuthUiState(isInProgress = true)

        when (val result = signOutUseCase(Unit)) {
            is Result.Success -> _authUiState.value = SettingsAuthUiState(isInProgress = false)

            is Result.Error -> _authUiState.value = SettingsAuthUiState(
                isInProgress = false,
                errorMessageRes = result.error.toMessageResOrNull()
                    ?: R.string.auth_error_sign_out_failed
            )

            Result.Loading -> Unit
        }
    }

    fun deleteAccount() = viewModelScope.launch {
        _authUiState.value = SettingsAuthUiState(isInProgress = true)

        when (val result = deleteAccountUseCase(Unit)) {
            is Result.Success -> _authUiState.value = SettingsAuthUiState(isInProgress = false)

            is Result.Error -> _authUiState.value = SettingsAuthUiState(
                isInProgress = false,
                errorMessageRes = result.error.toMessageResOrNull()
                    ?: R.string.auth_error_delete_account_failed
            )

            Result.Loading -> Unit
        }
    }
}