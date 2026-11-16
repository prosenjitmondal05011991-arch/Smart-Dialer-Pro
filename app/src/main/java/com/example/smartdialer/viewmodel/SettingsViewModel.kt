package com.example.smartdialer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartdialer.data.model.DefaultDialerStatus
import com.example.smartdialer.data.preferences.AccentColorOption
import com.example.smartdialer.data.preferences.AppLanguage
import com.example.smartdialer.data.preferences.CallRecordingScope
import com.example.smartdialer.data.preferences.FontSizeScale
import com.example.smartdialer.data.preferences.PreferencesRepository
import com.example.smartdialer.data.preferences.ThemeMode
import com.example.smartdialer.data.preferences.UserPreferences
import com.example.smartdialer.telecom.DialerTelecomManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val preferencesRepository: PreferencesRepository
) : AndroidViewModel(application) {

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _defaultDialerStatus = MutableStateFlow(
        DialerTelecomManager.checkDefaultDialerStatus(application)
    )
    val defaultDialerStatus: StateFlow<DefaultDialerStatus> = _defaultDialerStatus.asStateFlow()

    fun refreshDefaultDialerStatus() {
        _defaultDialerStatus.value = DialerTelecomManager.checkDefaultDialerStatus(getApplication())
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setAccentColor(option: AccentColorOption) {
        viewModelScope.launch { preferencesRepository.setAccentColor(option) }
    }

    fun setFontSizeScale(scale: FontSizeScale) {
        viewModelScope.launch { preferencesRepository.setFontSizeScale(scale) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setVibrationEnabled(enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun setPreferredSimSlot(slotIndex: Int) {
        viewModelScope.launch { preferencesRepository.setPreferredSimSlot(slotIndex) }
    }

    fun setAutoCallRecordingEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoCallRecordingEnabled(enabled) }
    }

    fun setCallRecordingScope(scope: CallRecordingScope) {
        viewModelScope.launch { preferencesRepository.setCallRecordingScope(scope) }
    }
}
