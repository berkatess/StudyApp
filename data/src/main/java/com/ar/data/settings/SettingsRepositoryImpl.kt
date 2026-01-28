package com.ar.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ar.domain.settings.model.ThemeMode
import com.ar.domain.settings.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANG = stringPreferencesKey("language_tag")
    }

    override val themeMode: Flow<ThemeMode> =
        context.dataStore.data.map { prefs ->
            when (prefs[Keys.THEME]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override val languageTag: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[Keys.LANG] }

    override suspend fun setLanguageTag(tag: String?) {
        context.dataStore.edit { prefs ->
            if (tag == null) prefs.remove(Keys.LANG) else prefs[Keys.LANG] = tag
        }
    }
}
