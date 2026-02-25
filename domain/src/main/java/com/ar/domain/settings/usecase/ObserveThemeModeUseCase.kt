package com.ar.domain.settings.usecase

import com.ar.domain.settings.model.ThemeMode
import com.ar.domain.settings.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<ThemeMode> = settingsRepository.themeMode
}