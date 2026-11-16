package com.example.smartdialer.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneEnabled
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.smartdialer.data.model.DefaultDialerStatus
import com.example.smartdialer.data.preferences.AccentColorOption
import com.example.smartdialer.data.preferences.AppLanguage
import com.example.smartdialer.data.preferences.CallRecordingScope
import com.example.smartdialer.data.preferences.FontSizeScale
import com.example.smartdialer.data.preferences.ThemeMode
import com.example.smartdialer.data.preferences.UserPreferences
import com.example.smartdialer.ui.components.DialerSubTopBar
import com.example.smartdialer.ui.theme.AmberGoldPrimary
import com.example.smartdialer.ui.theme.EmeraldPrimary
import com.example.smartdialer.ui.theme.OceanBluePrimary
import com.example.smartdialer.ui.theme.RoyalPurplePrimary
import com.example.smartdialer.util.ContactExportFormat

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    defaultDialerStatus: DefaultDialerStatus,
    blockedCount: Int = 0,
    isCallScreeningActive: Boolean = false,
    isExportingContacts: Boolean = false,
    grantedPermissionsCount: Int = 0,
    totalPermissionsCount: Int = 5,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (AccentColorOption) -> Unit,
    onFontSizeChange: (FontSizeScale) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onAutoCallRecordingChange: (Boolean) -> Unit = {},
    onCallRecordingScopeChange: (CallRecordingScope) -> Unit = {},
    onExportContacts: (ContactExportFormat) -> Unit = {},
    onOpenAppPermissions: () -> Unit = {},
    onOpenBlockedNumbers: () -> Unit = {},
    onOpenPrivacyInfo: () -> Unit,
    onOpenDefaultDialerStatus: () -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DialerSubTopBar(
            title = stringResource(R.string.settings_title),
            onBack = onBack
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // App Identity Banner Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(18.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dialer_launcher_fg),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.app_version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Appearance Section
            SettingsSectionLabel(stringResource(R.string.settings_section_appearance))
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val themeLabel = when (preferences.themeMode) {
                        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    }
                    SettingsRowItem(
                        icon = Icons.Outlined.DarkMode,
                        title = stringResource(R.string.settings_theme_title),
                        subtitle = themeLabel,
                        onClick = { showThemeDialog = true },
                        testTag = "settings_theme_row"
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Accent Color Swatches Row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.settings_accent_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val swatches = listOf(
                                AccentColorOption.OCEAN_BLUE to OceanBluePrimary,
                                AccentColorOption.EMERALD_GREEN to EmeraldPrimary,
                                AccentColorOption.ROYAL_PURPLE to RoyalPurplePrimary,
                                AccentColorOption.AMBER_ORANGE to AmberGoldPrimary
                            )
                            swatches.forEach { (option, swatchColor) ->
                                val isSelected = preferences.accentColor == option
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { onAccentColorChange(option) }
                                        .testTag("settings_accent_${option.name.lowercase()}")
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                FilterChip(
                                    selected = preferences.accentColor == AccentColorOption.DYNAMIC,
                                    onClick = { onAccentColorChange(AccentColorOption.DYNAMIC) },
                                    label = { Text(stringResource(R.string.settings_accent_dynamic)) },
                                    modifier = Modifier.testTag("settings_accent_dynamic")
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    val fontLabel = when (preferences.fontSizeScale) {
                        FontSizeScale.SMALL -> stringResource(R.string.settings_font_small)
                        FontSizeScale.NORMAL -> stringResource(R.string.settings_font_normal)
                        FontSizeScale.LARGE -> stringResource(R.string.settings_font_large)
                        FontSizeScale.EXTRA_LARGE -> stringResource(R.string.settings_font_extra_large)
                    }
                    SettingsRowItem(
                        icon = Icons.Filled.FormatSize,
                        title = stringResource(R.string.settings_font_size_title),
                        subtitle = fontLabel,
                        onClick = { showFontDialog = true },
                        testTag = "settings_font_size_row"
                    )
                }
            }

            // Dialer & Language Section
            SettingsSectionLabel(stringResource(R.string.settings_section_dialer))
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVibrationChange(!preferences.vibrationEnabled) }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("settings_vibration_row")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_vibration_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.settings_vibration_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.vibrationEnabled,
                            onCheckedChange = onVibrationChange,
                            modifier = Modifier.testTag("settings_vibration_switch")
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    val langLabel = when (preferences.language) {
                        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                        AppLanguage.ENGLISH -> stringResource(R.string.settings_language_en)
                        AppLanguage.BENGALI -> stringResource(R.string.settings_language_bn)
                    }
                    SettingsRowItem(
                        icon = Icons.Filled.Language,
                        title = stringResource(R.string.settings_language_title),
                        subtitle = langLabel,
                        onClick = { showLanguageDialog = true },
                        testTag = "settings_language_row"
                    )
                }
            }

            // Call Recording Section
            SettingsSectionLabel(stringResource(R.string.settings_section_call_recording))
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAutoCallRecordingChange(!preferences.autoCallRecordingEnabled)
                            }
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("settings_auto_recording_row")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_auto_recording_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.settings_auto_recording_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = preferences.autoCallRecordingEnabled,
                            onCheckedChange = onAutoCallRecordingChange,
                            modifier = Modifier.testTag("settings_auto_recording_switch")
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.settings_recording_scope_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val scopeOptions = listOf(
                            Triple(
                                CallRecordingScope.ALL_CALLS,
                                stringResource(R.string.settings_recording_scope_all),
                                stringResource(R.string.settings_recording_scope_all_desc)
                            ),
                            Triple(
                                CallRecordingScope.UNKNOWN_CONTACTS_ONLY,
                                stringResource(R.string.settings_recording_scope_unknown),
                                stringResource(R.string.settings_recording_scope_unknown_desc)
                            )
                        )

                        scopeOptions.forEach { (scopeOption, label, description) ->
                            val selected = preferences.callRecordingScope == scopeOption
                            val tagSuffix = if (scopeOption == CallRecordingScope.ALL_CALLS) {
                                "all"
                            } else {
                                "unknown"
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onCallRecordingScopeChange(scopeOption) }
                                    .minimumInteractiveComponentSize()
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                                    .testTag("settings_recording_scope_$tagSuffix")
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onCallRecordingScopeChange(scopeOption) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.settings_recording_device_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Backup & Export Section
            SettingsSectionLabel(stringResource(R.string.settings_section_backup))
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    if (isExportingContacts) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_export_in_progress),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    SettingsRowItem(
                        icon = Icons.Filled.ContactPage,
                        title = stringResource(R.string.settings_export_vcf_title),
                        subtitle = stringResource(R.string.settings_export_vcf_subtitle),
                        onClick = {
                            if (!isExportingContacts) {
                                onExportContacts(ContactExportFormat.VCF)
                            }
                        },
                        testTag = "settings_export_vcf_row"
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsRowItem(
                        icon = Icons.Filled.Description,
                        title = stringResource(R.string.settings_export_csv_title),
                        subtitle = stringResource(R.string.settings_export_csv_subtitle),
                        onClick = {
                            if (!isExportingContacts) {
                                onExportContacts(ContactExportFormat.CSV)
                            }
                        },
                        testTag = "settings_export_csv_row"
                    )
                }
            }

            // Privacy & System Section
            SettingsSectionLabel(stringResource(R.string.settings_section_privacy_system))
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Filled.AdminPanelSettings,
                        title = stringResource(R.string.settings_permissions_title),
                        subtitle = if (grantedPermissionsCount >= totalPermissionsCount) {
                            stringResource(
                                R.string.settings_permissions_subtitle_all_granted,
                                totalPermissionsCount
                            )
                        } else {
                            stringResource(
                                R.string.settings_permissions_subtitle_partial,
                                grantedPermissionsCount,
                                totalPermissionsCount
                            )
                        },
                        onClick = onOpenAppPermissions,
                        testTag = "settings_permissions_row"
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsRowItem(
                        icon = Icons.Filled.Block,
                        title = stringResource(R.string.blocklist_title),
                        subtitle = if (isCallScreeningActive) {
                            stringResource(R.string.blocklist_settings_subtitle_active, blockedCount)
                        } else {
                            stringResource(R.string.blocklist_settings_subtitle_inactive, blockedCount)
                        },
                        onClick = onOpenBlockedNumbers,
                        testTag = "settings_blocked_numbers_row"
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsRowItem(
                        icon = Icons.Filled.PrivacyTip,
                        title = stringResource(R.string.settings_privacy_title),
                        subtitle = stringResource(R.string.settings_privacy_subtitle),
                        onClick = onOpenPrivacyInfo,
                        testTag = "settings_privacy_row"
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsRowItem(
                        icon = Icons.Filled.PhoneEnabled,
                        title = stringResource(R.string.settings_default_dialer_title),
                        subtitle = if (defaultDialerStatus.isDefaultDialer) {
                            stringResource(R.string.settings_default_dialer_active)
                        } else {
                            stringResource(R.string.settings_default_dialer_inactive)
                        },
                        onClick = onOpenDefaultDialerStatus,
                        testTag = "settings_default_dialer_row"
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsRowItem(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.settings_about_title),
                        subtitle = stringResource(R.string.settings_about_subtitle),
                        onClick = { showAboutDialog = true },
                        testTag = "settings_about_row"
                    )
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showThemeDialog) {
        RadioSelectionDialog(
            title = stringResource(R.string.settings_theme_title),
            options = listOf(
                ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                ThemeMode.DARK to stringResource(R.string.settings_theme_dark)
            ),
            selected = preferences.themeMode,
            onSelect = {
                onThemeModeChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showFontDialog) {
        RadioSelectionDialog(
            title = stringResource(R.string.settings_font_size_title),
            options = listOf(
                FontSizeScale.SMALL to stringResource(R.string.settings_font_small),
                FontSizeScale.NORMAL to stringResource(R.string.settings_font_normal),
                FontSizeScale.LARGE to stringResource(R.string.settings_font_large),
                FontSizeScale.EXTRA_LARGE to stringResource(R.string.settings_font_extra_large)
            ),
            selected = preferences.fontSizeScale,
            onSelect = {
                onFontSizeChange(it)
                showFontDialog = false
            },
            onDismiss = { showFontDialog = false }
        )
    }

    if (showLanguageDialog) {
        RadioSelectionDialog(
            title = stringResource(R.string.settings_language_title),
            options = listOf(
                AppLanguage.SYSTEM to stringResource(R.string.settings_language_system),
                AppLanguage.ENGLISH to stringResource(R.string.settings_language_en),
                AppLanguage.BENGALI to stringResource(R.string.settings_language_bn)
            ),
            selected = preferences.language,
            onSelect = {
                onLanguageChange(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.app_name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.app_version),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.dialer_status_roadmap_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun <T> RadioSelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}
