package com.ar.studyapp.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.domain.auth.repository.AuthRepository
import com.ar.domain.auth.repository.GoogleAuthRepository
import com.ar.domain.settings.model.ThemeMode
import com.ar.domain.settings.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val googleAuthRepository: GoogleAuthRepository
) : ViewModel() {

    val themeMode = settingsRepository.themeMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM
    )

    val languageTag = settingsRepository.languageTag.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null
    )

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(mode)
    }

    fun setLanguage(tag: String?) = viewModelScope.launch {
        settingsRepository.setLanguageTag(tag)
    }

    val user = googleAuthRepository.user // Flow<UserInfo?>

    fun signOut() = viewModelScope.launch { googleAuthRepository.signOut() }
    fun deleteAccount() = viewModelScope.launch { googleAuthRepository.deleteAccount() }

    fun signInWithGoogleIdToken(idToken: String) = viewModelScope.launch {
        googleAuthRepository.signInWithGoogleIdToken(idToken)
    }
}
