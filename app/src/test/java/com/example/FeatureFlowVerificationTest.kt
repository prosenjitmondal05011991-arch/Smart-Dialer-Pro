package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FeatureFlowVerificationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `bottom navigation order is Keypad Recents Contacts Favorites and all tabs navigate`() {
        composeRule.waitForIdle()

        val keypadLeft = composeRule.onNodeWithTag("nav_keypad").fetchSemanticsNode().boundsInRoot.left
        val recentsLeft = composeRule.onNodeWithTag("nav_recents").fetchSemanticsNode().boundsInRoot.left
        val contactsLeft = composeRule.onNodeWithTag("nav_contacts").fetchSemanticsNode().boundsInRoot.left
        val favoritesLeft = composeRule.onNodeWithTag("nav_favorites").fetchSemanticsNode().boundsInRoot.left

        assertTrue(keypadLeft < recentsLeft)
        assertTrue(recentsLeft < contactsLeft)
        assertTrue(contactsLeft < favoritesLeft)

        composeRule.onNodeWithTag("nav_recents").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("recents_filter_all").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_contacts").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("contacts_add_fab").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_favorites").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_keypad").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("keypad_call_button").assertIsDisplayed()
    }

    @Test
    fun `keypad input and backspace update number display`() {
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dial_key_0").performClick()
        composeRule.onNodeWithTag("dial_key_1").performClick()
        composeRule.onNodeWithTag("dial_key_7").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("keypad_number_display").assertTextContains("017")

        composeRule.onNodeWithTag("keypad_backspace_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("keypad_number_display").assertTextContains("01")
    }

    @Test
    fun `settings call recording backup and blocklist screens work end to end`() {
        composeRule.waitForIdle()

        // Open overflow menu -> Settings
        composeRule.onNodeWithTag("top_bar_overflow_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_settings_item").performClick()
        composeRule.waitForIdle()

        // Toggle Automatic Call Recording and select Unknown contacts only
        composeRule.onNodeWithTag("settings_auto_recording_switch")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("settings_recording_scope_unknown")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        // Verify Backup & Export VCF/CSV rows are displayed
        composeRule.onNodeWithTag("settings_export_vcf_row")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings_export_csv_row")
            .performScrollTo()
            .assertIsDisplayed()

        // Navigate to Blocked Numbers from Settings
        composeRule.onNodeWithTag("settings_blocked_numbers_row")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        // Add a blocked number and verify it renders in the list
        composeRule.onNodeWithTag("blocklist_add_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("blocklist_number_input").performTextInput("+8801712345678")
        composeRule.onNodeWithTag("blocklist_label_input").performTextInput("Spam Test")
        composeRule.onNodeWithTag("blocklist_confirm_add").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Spam Test").assertIsDisplayed()
        composeRule.onNodeWithText("+8801712345678").assertIsDisplayed()

        // Return to Settings -> Privacy -> Back -> Default Dialer Status
        composeRule.onNodeWithTag("sub_top_bar_back").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_about_row").performScrollTo()
        composeRule.onNodeWithTag("settings_privacy_row")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("100% Local-First Architecture").assertIsDisplayed()

        composeRule.onNodeWithTag("sub_top_bar_back").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_about_row").performScrollTo()
        composeRule.onNodeWithTag("settings_default_dialer_row")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("default_dialer_status_heading").assertIsDisplayed()
    }

    @Test
    fun `settings opens app permissions hub with grant all rationale and zero permission apis`() {
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("top_bar_overflow_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_settings_item").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_about_row").performScrollTo()
        composeRule.onNodeWithTag("settings_permissions_row")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("permissions_summary_title").assertIsDisplayed()
        composeRule.onNodeWithTag("permissions_open_system_button").assertIsDisplayed()
        composeRule.onNodeWithTag("perm_role_dialer").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("perm_zero_photos").performScrollTo().assertIsDisplayed()

        // Tap Grant All Permissions and verify pre-explanation rationale dialog opens
        composeRule.onNodeWithTag("permissions_grant_all_button")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Allow Core Dialer Permissions").assertIsDisplayed()
    }
}
