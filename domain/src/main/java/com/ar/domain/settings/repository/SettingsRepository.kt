package com.ar.domain.settings.repository

import com.ar.domain.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    val languageTag: Flow<String?> // örn: "tr", "en", "es" / null => system
    suspend fun setLanguageTag(tag: String?)
}
