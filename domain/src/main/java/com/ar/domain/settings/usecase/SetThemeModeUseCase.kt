package com.ar.domain.settings.usecase

import com.ar.domain.settings.model.ThemeMode
import com.ar.domain.settings.repository.SettingsRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}