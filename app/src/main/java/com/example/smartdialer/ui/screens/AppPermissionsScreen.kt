package com.example.smartdialer.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneEnabled
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.smartdialer.data.model.DefaultDialerStatus
import com.example.smartdialer.permissions.PermissionHelper
import com.example.smartdialer.telecom.DialerTelecomManager
import com.example.smartdialer.ui.components.DialerSubTopBar
import com.example.smartdialer.ui.components.PermissionExplainDialog
import com.example.smartdialer.ui.theme.LocalDialerExtendedColors

private data class RuntimePermissionGroupSpec(
    val id: String,
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
    val permissions: List<String>,
    val isGranted: Boolean
)

@Composable
fun AppPermissionsScreen(
    defaultDialerStatus: DefaultDialerStatus,
    isCallScreeningActive: Boolean,
    onBack: () -> Unit,
    onPermissionsUpdated: (grantedAll: Boolean) -> Unit,
    onRefreshRoles: () -> Unit
) {
    val context = LocalContext.current
    val extendedColors = LocalDialerExtendedColors.current
    var statusRevision by remember { mutableIntStateOf(0) }
    var pendingRationaleRequest by remember { mutableStateOf<Pair<String, List<String>>?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                statusRevision++
                onRefreshRoles()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        statusRevision++
        onPermissionsUpdated(PermissionHelper.areAllRuntimePermissionsGranted(context))
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        statusRevision++
        onRefreshRoles()
    }

    val grantedCount = remember(statusRevision) {
        PermissionHelper.grantedRuntimePermissionCount(context)
    }
    val totalCount = PermissionHelper.ALL_RUNTIME_PERMISSIONS.size
    val allGranted = grantedCount == totalCount

    val groups = remember(statusRevision) {
        listOf(
            RuntimePermissionGroupSpec(
                id = "contacts",
                icon = Icons.Filled.Contacts,
                titleRes = R.string.permissions_group_contacts_title,
                descriptionRes = R.string.permissions_group_contacts_desc,
                permissions = listOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
                ),
                isGranted = PermissionHelper.hasContactsReadPermission(context) &&
                    PermissionHelper.hasContactsWritePermission(context)
            ),
            RuntimePermissionGroupSpec(
                id = "call_logs",
                icon = Icons.Filled.History,
                titleRes = R.string.permissions_group_call_logs_title,
                descriptionRes = R.string.permissions_group_call_logs_desc,
                permissions = listOf(Manifest.permission.READ_CALL_LOG),
                isGranted = PermissionHelper.hasCallLogPermission(context)
            ),
            RuntimePermissionGroupSpec(
                id = "phone",
                icon = Icons.Filled.Phone,
                titleRes = R.string.permissions_group_phone_title,
                descriptionRes = R.string.permissions_group_phone_desc,
                permissions = listOf(
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE
                ),
                isGranted = PermissionHelper.hasCallPhonePermission(context) &&
                    PermissionHelper.hasPhoneStatePermission(context)
            )
        )
    }

    val allowedGroups = groups.filter { it.isGranted }
    val missingGroups = groups.filterNot { it.isGranted }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DialerSubTopBar(
            title = stringResource(R.string.permissions_screen_title),
            onBack = onBack
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Summary Hero Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val badgeColor = if (allGranted) {
                            extendedColors.callAccept
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.14f))
                        ) {
                            Icon(
                                imageVector = if (allGranted) {
                                    Icons.Filled.CheckCircle
                                } else {
                                    Icons.Filled.AdminPanelSettings
                                },
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = if (allGranted) {
                                stringResource(R.string.permissions_summary_all_title)
                            } else {
                                stringResource(
                                    R.string.permissions_summary_partial_title,
                                    grantedCount,
                                    totalCount
                                )
                            },
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag("permissions_summary_title")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.permissions_summary_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!allGranted) {
                        val explainTitle = stringResource(R.string.permissions_explain_all_title)
                        val missing = PermissionHelper.missingRuntimePermissions(context)
                        Button(
                            onClick = {
                                if (missing.isNotEmpty()) {
                                    pendingRationaleRequest = explainTitle to missing
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("permissions_grant_all_button")
                        ) {
                            Text(stringResource(R.string.permissions_grant_all_button))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = { PermissionHelper.openAppSettings(context) },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("permissions_open_system_button")
                    ) {
                        Text(stringResource(R.string.permissions_open_system_button))
                    }
                }
            }

            // Allowed Runtime Permissions Group
            if (allowedGroups.isNotEmpty()) {
                PermissionsSectionTitle(stringResource(R.string.permissions_section_allowed))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        allowedGroups.forEachIndexed { index, spec ->
                            PermissionItemRow(
                                icon = spec.icon,
                                title = stringResource(spec.titleRes),
                                statusText = stringResource(R.string.permissions_status_allowed),
                                description = stringResource(spec.descriptionRes),
                                isPositiveStatus = true,
                                showChevron = false,
                                onClick = { PermissionHelper.openAppSettings(context) },
                                testTag = "perm_row_${spec.id}"
                            )
                            if (index < allowedGroups.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Needs Permission Group
            if (missingGroups.isNotEmpty()) {
                PermissionsSectionTitle(stringResource(R.string.permissions_section_not_allowed))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        missingGroups.forEachIndexed { index, spec ->
                            val title = stringResource(spec.titleRes)
                            PermissionItemRow(
                                icon = spec.icon,
                                title = title,
                                statusText = stringResource(R.string.permissions_status_not_allowed),
                                description = stringResource(spec.descriptionRes),
                                isPositiveStatus = false,
                                showChevron = true,
                                onClick = {
                                    val needed = spec.permissions.filterNot {
                                        PermissionHelper.hasPermission(context, it)
                                    }
                                    if (needed.isNotEmpty()) {
                                        pendingRationaleRequest = title to needed
                                    }
                                },
                                testTag = "perm_row_${spec.id}"
                            )
                            if (index < missingGroups.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Android System Roles
            PermissionsSectionTitle(stringResource(R.string.permissions_section_roles))
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    PermissionItemRow(
                        icon = Icons.Filled.PhoneEnabled,
                        title = stringResource(R.string.permissions_role_dialer_title),
                        statusText = if (defaultDialerStatus.isDefaultDialer) {
                            stringResource(R.string.permissions_status_active)
                        } else {
                            stringResource(R.string.permissions_status_inactive)
                        },
                        description = stringResource(R.string.permissions_role_dialer_desc),
                        isPositiveStatus = defaultDialerStatus.isDefaultDialer,
                        showChevron = !defaultDialerStatus.isDefaultDialer,
                        onClick = {
                            val roleIntent = DialerTelecomManager.createRequestDefaultDialerIntent(context)
                            if (roleIntent != null) {
                                runCatching { roleLauncher.launch(roleIntent) }
                            } else {
                                PermissionHelper.openAppSettings(context)
                            }
                        },
                        testTag = "perm_role_dialer"
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    PermissionItemRow(
                        icon = Icons.Filled.Shield,
                        title = stringResource(R.string.permissions_role_screening_title),
                        statusText = if (isCallScreeningActive) {
                            stringResource(R.string.permissions_status_active)
                        } else {
                            stringResource(R.string.permissions_status_inactive)
                        },
                        description = stringResource(R.string.permissions_role_screening_desc),
                        isPositiveStatus = isCallScreeningActive,
                        showChevron = !isCallScreeningActive,
                        onClick = {
                            val roleIntent = DialerTelecomManager.createRequestCallScreeningRoleIntent(context)
                            if (roleIntent != null) {
                                runCatching { roleLauncher.launch(roleIntent) }
                            } else {
                                PermissionHelper.openAppSettings(context)
                            }
                        },
                        testTag = "perm_role_screening"
                    )
                }
            }

            // Built-In Zero-Permission Privacy APIs
            PermissionsSectionTitle(stringResource(R.string.permissions_section_zero_perm))
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    PermissionItemRow(
                        icon = Icons.Filled.PhotoLibrary,
                        title = stringResource(R.string.permissions_zero_photos_title),
                        statusText = stringResource(R.string.permissions_status_system_managed),
                        description = stringResource(R.string.permissions_zero_photos_desc),
                        isPositiveStatus = true,
                        showChevron = false,
                        onClick = null,
                        testTag = "perm_zero_photos"
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    PermissionItemRow(
                        icon = Icons.Filled.FolderOpen,
                        title = stringResource(R.string.permissions_zero_storage_title),
                        statusText = stringResource(R.string.permissions_status_system_managed),
                        description = stringResource(R.string.permissions_zero_storage_desc),
                        isPositiveStatus = true,
                        showChevron = false,
                        onClick = null,
                        testTag = "perm_zero_storage"
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    PermissionItemRow(
                        icon = Icons.Filled.Sms,
                        title = stringResource(R.string.permissions_zero_sms_title),
                        statusText = stringResource(R.string.permissions_status_system_managed),
                        description = stringResource(R.string.permissions_zero_sms_desc),
                        isPositiveStatus = true,
                        showChevron = false,
                        onClick = null,
                        testTag = "perm_zero_sms"
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    PermissionItemRow(
                        icon = Icons.Filled.Vibration,
                        title = stringResource(R.string.permissions_zero_vibrate_title),
                        statusText = stringResource(R.string.permissions_status_allowed),
                        description = stringResource(R.string.permissions_zero_vibrate_desc),
                        isPositiveStatus = true,
                        showChevron = false,
                        onClick = null,
                        testTag = "perm_zero_vibrate"
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    pendingRationaleRequest?.let { (dialogTitle, permissionsToRequest) ->
        PermissionExplainDialog(
            title = dialogTitle,
            message = stringResource(R.string.permissions_explain_all_message),
            primaryLabel = stringResource(R.string.action_grant_permission),
            secondaryLabel = stringResource(R.string.action_open_settings),
            onPrimaryConfirm = {
                pendingRationaleRequest = null
                multiPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            },
            onSecondaryAction = {
                pendingRationaleRequest = null
                PermissionHelper.openAppSettings(context)
            },
            onDismiss = { pendingRationaleRequest = null }
        )
    }
}

@Composable
private fun PermissionsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    statusText: String,
    description: String,
    isPositiveStatus: Boolean,
    showChevron: Boolean,
    onClick: (() -> Unit)?,
    testTag: String
) {
    val extendedColors = LocalDialerExtendedColors.current
    val baseModifier = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .minimumInteractiveComponentSize()
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .testTag(testTag)

    Row(
        verticalAlignment = Alignment.Top,
        modifier = baseModifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPositiveStatus) extendedColors.callAccept else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = if (isPositiveStatus) {
                    extendedColors.callAccept
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        } else if (isPositiveStatus) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = extendedColors.callAccept.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
        }
    }
}
