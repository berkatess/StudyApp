package com.ar.studyapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.domain.settings.model.ThemeMode
import com.ar.domain.settings.usecase.ObserveThemeModeUseCase
import com.ar.domain.settings.usecase.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds app-wide UI state that affects the entire Compose tree.
 *
 * Keep global preferences here (theme, locale overrides, etc.). Feature-specific state
 * should live in its own screen/feature ViewModel.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = observeThemeModeUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.LIGHT
        )

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        setThemeModeUseCase(mode)
    }
}