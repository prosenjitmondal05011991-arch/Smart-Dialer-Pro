package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.smartdialer.data.model.DefaultDialerStatus
import com.example.smartdialer.data.preferences.AppLanguage
import com.example.smartdialer.data.preferences.CallRecordingScope
import com.example.smartdialer.data.preferences.PreferencesRepository
import com.example.smartdialer.data.preferences.UserPreferences
import com.example.smartdialer.localization.LocaleHelper
import com.example.smartdialer.ui.screens.SettingsScreen
import com.example.smartdialer.ui.theme.SmartDialerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CallRecordingSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `preferences repository persists auto call recording and scope`() = runTest {
        val defaults = UserPreferences()
        assertFalse(defaults.autoCallRecordingEnabled)
        assertEquals(CallRecordingScope.ALL_CALLS, defaults.callRecordingScope)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PreferencesRepository(context)

        // Ensure clean baseline regardless of prior tests sharing DataStore in Robolectric
        repo.setAutoCallRecordingEnabled(false)
        repo.setCallRecordingScope(CallRecordingScope.ALL_CALLS)

        val baseline = repo.preferencesFlow.first()
        assertFalse(baseline.autoCallRecordingEnabled)
        assertEquals(CallRecordingScope.ALL_CALLS, baseline.callRecordingScope)

        repo.setAutoCallRecordingEnabled(true)
        repo.setCallRecordingScope(CallRecordingScope.UNKNOWN_CONTACTS_ONLY)

        val updated = repo.preferencesFlow.first()
        assertTrue(updated.autoCallRecordingEnabled)
        assertEquals(CallRecordingScope.UNKNOWN_CONTACTS_ONLY, updated.callRecordingScope)

        repo.setAutoCallRecordingEnabled(false)
        repo.setCallRecordingScope(CallRecordingScope.ALL_CALLS)
    }

    @Test
    fun `settings screen toggles automatic call recording and switches scope`() {
        var currentPrefs by mutableStateOf(UserPreferences())

        composeRule.setContent {
            SmartDialerTheme {
                SettingsScreen(
                    preferences = currentPrefs,
                    defaultDialerStatus = DefaultDialerStatus(
                        isDefaultDialer = false,
                        currentDefaultPackage = null,
                        supportsRoleManager = true
                    ),
                    onBack = {},
                    onThemeModeChange = {},
                    onAccentColorChange = {},
                    onFontSizeChange = {},
                    onVibrationChange = {},
                    onLanguageChange = {},
                    onAutoCallRecordingChange = { enabled ->
                        currentPrefs = currentPrefs.copy(autoCallRecordingEnabled = enabled)
                    },
                    onCallRecordingScopeChange = { scope ->
                        currentPrefs = currentPrefs.copy(callRecordingScope = scope)
                    },
                    onOpenPrivacyInfo = {},
                    onOpenDefaultDialerStatus = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_auto_recording_switch")
            .performScrollTo()
            .assertIsOff()
            .performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_auto_recording_switch").assertIsOn()
        assertTrue(currentPrefs.autoCallRecordingEnabled)

        composeRule.onNodeWithTag("settings_recording_scope_unknown")
            .performScrollTo()
            .performClick()

        composeRule.waitForIdle()
        assertEquals(CallRecordingScope.UNKNOWN_CONTACTS_ONLY, currentPrefs.callRecordingScope)

        composeRule.onNodeWithTag("settings_recording_scope_all")
            .performScrollTo()
            .performClick()

        composeRule.waitForIdle()
        assertEquals(CallRecordingScope.ALL_CALLS, currentPrefs.callRecordingScope)
    }

    @Test
    fun `call recording strings are localized for English and Bengali`() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val en = LocaleHelper.createLocalizedContext(base, AppLanguage.ENGLISH)
        val bn = LocaleHelper.createLocalizedContext(base, AppLanguage.BENGALI)

        assertEquals("Call Recording", en.getString(R.string.settings_section_call_recording))
        assertEquals("Automatic Call Recording", en.getString(R.string.settings_auto_recording_title))
        assertEquals("All calls", en.getString(R.string.settings_recording_scope_all))
        assertEquals("Unknown contacts only", en.getString(R.string.settings_recording_scope_unknown))

        assertFalse(
            bn.getString(R.string.settings_section_call_recording) ==
                en.getString(R.string.settings_section_call_recording)
        )
        assertFalse(
            bn.getString(R.string.settings_recording_scope_unknown) ==
                en.getString(R.string.settings_recording_scope_unknown)
        )
    }
}
