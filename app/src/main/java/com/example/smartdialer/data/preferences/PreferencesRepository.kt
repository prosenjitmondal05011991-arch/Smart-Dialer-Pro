package com.example.smartdialer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.smartDialerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "smart_dialer_preferences"
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val FONT_SIZE_SCALE = stringPreferencesKey("font_size_scale")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val PREFERRED_SIM_SLOT = intPreferencesKey("preferred_sim_slot")
        val AUTO_CALL_RECORDING_ENABLED = booleanPreferencesKey("auto_call_recording_enabled")
        val CALL_RECORDING_SCOPE = stringPreferencesKey("call_recording_scope")
    }

    val preferencesFlow: Flow<UserPreferences> = context.smartDialerDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            UserPreferences(
                themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                accentColor = prefs[Keys.ACCENT_COLOR]?.let { runCatching { AccentColorOption.valueOf(it) }.getOrNull() }
                    ?: AccentColorOption.OCEAN_BLUE,
                fontSizeScale = prefs[Keys.FONT_SIZE_SCALE]?.let { runCatching { FontSizeScale.valueOf(it) }.getOrNull() }
                    ?: FontSizeScale.NORMAL,
                vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
                language = prefs[Keys.APP_LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                    ?: AppLanguage.SYSTEM,
                preferredSimSlot = prefs[Keys.PREFERRED_SIM_SLOT] ?: -1,
                autoCallRecordingEnabled = prefs[Keys.AUTO_CALL_RECORDING_ENABLED] ?: false,
                callRecordingScope = prefs[Keys.CALL_RECORDING_SCOPE]?.let {
                    runCatching { CallRecordingScope.valueOf(it) }.getOrNull()
                } ?: CallRecordingScope.ALL_CALLS
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.smartDialerDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(option: AccentColorOption) {
        context.smartDialerDataStore.edit { it[Keys.ACCENT_COLOR] = option.name }
    }

    suspend fun setFontSizeScale(scale: FontSizeScale) {
        context.smartDialerDataStore.edit { it[Keys.FONT_SIZE_SCALE] = scale.name }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.smartDialerDataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.smartDialerDataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }

    suspend fun setPreferredSimSlot(slotIndex: Int) {
        context.smartDialerDataStore.edit { it[Keys.PREFERRED_SIM_SLOT] = slotIndex }
    }

    suspend fun setAutoCallRecordingEnabled(enabled: Boolean) {
        context.smartDialerDataStore.edit { it[Keys.AUTO_CALL_RECORDING_ENABLED] = enabled }
    }

    suspend fun setCallRecordingScope(scope: CallRecordingScope) {
        context.smartDialerDataStore.edit { it[Keys.CALL_RECORDING_SCOPE] = scope.name }
    }
}
