package com.example

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.data.model.DefaultDialerStatus
import com.example.smartdialer.data.model.PhoneNumberEntry
import com.example.smartdialer.data.preferences.AppLanguage
import com.example.smartdialer.data.preferences.UserPreferences
import com.example.smartdialer.localization.LocaleHelper
import com.example.smartdialer.ui.screens.SettingsScreen
import com.example.smartdialer.ui.theme.SmartDialerTheme
import com.example.smartdialer.util.ContactExportFormat
import com.example.smartdialer.util.ContactsExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ContactsExporterTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleContacts = listOf(
        Contact(
            id = 1L,
            lookupKey = "k1",
            displayName = "Sadia Akter",
            primaryNumber = "+8801712345678",
            phoneNumbers = listOf(
                PhoneNumberEntry("+8801712345678", "8801712345678", "Mobile", isPrimary = true),
                PhoneNumberEntry("029876543", "029876543", "Work")
            ),
            isStarred = true,
            emails = listOf("sadia@example.com"),
            organization = "Dhaka Tech, Ltd"
        ),
        Contact(
            id = 2L,
            lookupKey = "k2",
            displayName = "Rakib \"Pro\" Hasan",
            primaryNumber = "01811223344",
            phoneNumbers = emptyList(),
            isStarred = false,
            emails = emptyList(),
            organization = null
        )
    )

    @Test
    fun `formatAsCsv outputs RFC 4180 escaped header and rows`() {
        val csv = ContactsExporter.formatAsCsv(sampleContacts)
        val lines = csv.trimEnd().split("\r\n")

        assertEquals("Name,Phone Numbers,Emails,Organization,Favorite", lines[0])
        assertEquals(
            "Sadia Akter,+8801712345678; 029876543,sadia@example.com,\"Dhaka Tech, Ltd\",Yes",
            lines[1]
        )
        assertEquals(
            "\"Rakib \"\"Pro\"\" Hasan\",01811223344,,,No",
            lines[2]
        )
    }

    @Test
    fun `formatAsVcf generates valid vCard 3_0 entries with escaped special chars`() {
        val vcf = ContactsExporter.formatAsVcf(sampleContacts)

        assertTrue(vcf.contains("BEGIN:VCARD\r\nVERSION:3.0"))
        assertTrue(vcf.contains("FN:Sadia Akter"))
        assertTrue(vcf.contains("N:Akter;Sadia;;;"))
        assertTrue(vcf.contains("TEL;TYPE=CELL:+8801712345678"))
        assertTrue(vcf.contains("TEL;TYPE=WORK:029876543"))
        assertTrue(vcf.contains("EMAIL;TYPE=INTERNET:sadia@example.com"))
        assertTrue(vcf.contains("ORG:Dhaka Tech\\, Ltd"))
        assertTrue(vcf.contains("TEL;TYPE=CELL:01811223344"))
        assertTrue(vcf.endsWith("END:VCARD"))
    }

    @Test
    fun `writeExportToUri writes UTF-8 backup file locally`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val targetFile = File(context.cacheDir, "contacts_backup_test.vcf")
        if (targetFile.exists()) targetFile.delete()

        val payload = ContactsExporter.formatContacts(sampleContacts, ContactExportFormat.VCF)
        val result = ContactsExporter.writeExportToUri(
            contentResolver = context.contentResolver,
            uri = Uri.fromFile(targetFile),
            content = payload
        )

        assertTrue(result.isSuccess)
        assertTrue(targetFile.exists())
        assertEquals(payload, targetFile.readText(Charsets.UTF_8))
        targetFile.delete()
    }

    @Test
    fun `defaultExportFileName uses expected extension`() {
        val vcfName = ContactsExporter.defaultExportFileName(ContactExportFormat.VCF, 1700000000000L)
        val csvName = ContactsExporter.defaultExportFileName(ContactExportFormat.CSV, 1700000000000L)

        assertTrue(vcfName.startsWith("smart_dialer_contacts_") && vcfName.endsWith(".vcf"))
        assertTrue(csvName.startsWith("smart_dialer_contacts_") && csvName.endsWith(".csv"))
    }

    @Test
    fun `settings screen triggers VCF and CSV export callbacks`() {
        val clickedFormats = mutableListOf<ContactExportFormat>()

        composeRule.setContent {
            SmartDialerTheme {
                SettingsScreen(
                    preferences = UserPreferences(),
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
                    onExportContacts = { format -> clickedFormats.add(format) },
                    onOpenPrivacyInfo = {},
                    onOpenDefaultDialerStatus = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_export_vcf_row")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("settings_export_csv_row")
            .performScrollTo()
            .performClick()

        composeRule.waitForIdle()
        assertEquals(listOf(ContactExportFormat.VCF, ContactExportFormat.CSV), clickedFormats)
    }

    @Test
    fun `backup and export strings are localized in English and Bengali`() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val en = LocaleHelper.createLocalizedContext(base, AppLanguage.ENGLISH)
        val bn = LocaleHelper.createLocalizedContext(base, AppLanguage.BENGALI)

        assertEquals("Backup & Export", en.getString(R.string.settings_section_backup))
        assertEquals("Export Contacts as VCF (.vcf)", en.getString(R.string.settings_export_vcf_title))
        assertEquals("Export Contacts as CSV (.csv)", en.getString(R.string.settings_export_csv_title))

        assertFalse(
            bn.getString(R.string.settings_section_backup) ==
                en.getString(R.string.settings_section_backup)
        )
        assertFalse(
            bn.getString(R.string.settings_export_csv_title) ==
                en.getString(R.string.settings_export_csv_title)
        )
    }
}
