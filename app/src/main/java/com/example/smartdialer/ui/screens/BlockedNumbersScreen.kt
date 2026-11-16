package com.example.smartdialer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.smartdialer.data.local.BlockedNumberEntity
import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.telecom.DialerTelecomManager
import com.example.smartdialer.ui.components.ContactAvatar
import com.example.smartdialer.ui.components.DialerSubTopBar
import com.example.smartdialer.ui.components.EmptyStateCard
import com.example.smartdialer.ui.theme.LocalDialerExtendedColors
import com.example.smartdialer.util.BlocklistMatcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedNumbersScreen(
    blockedNumbers: List<BlockedNumberEntity>,
    isCallScreeningActive: Boolean,
    availableContacts: List<Contact>,
    onRefreshRoleStatus: () -> Unit,
    onAddBlockedNumber: (String, String?) -> Unit,
    onUnblock: (BlockedNumberEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val extendedColors = LocalDialerExtendedColors.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showContactPickerSheet by remember { mutableStateOf(false) }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshRoleStatus()
    }

    LaunchedEffect(Unit) {
        onRefreshRoleStatus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            DialerSubTopBar(
                title = stringResource(R.string.blocklist_title),
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
            ) {
                // Live CallScreeningService Role Status Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isCallScreeningActive) {
                        extendedColors.callAccept.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCallScreeningActive) {
                                            extendedColors.callAccept.copy(alpha = 0.18f)
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        }
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isCallScreeningActive) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Filled.Shield
                                    },
                                    contentDescription = null,
                                    tint = if (isCallScreeningActive) {
                                        extendedColors.callAccept
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isCallScreeningActive) {
                                        stringResource(R.string.blocklist_role_active_title)
                                    } else {
                                        stringResource(R.string.blocklist_role_inactive_title)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.testTag("blocklist_role_status_title")
                                )
                                Text(
                                    text = if (isCallScreeningActive) {
                                        stringResource(R.string.blocklist_role_active_body)
                                    } else {
                                        stringResource(R.string.blocklist_role_inactive_body)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!isCallScreeningActive) {
                            val roleIntent = DialerTelecomManager.createRequestCallScreeningRoleIntent(context)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (roleIntent != null) {
                                        runCatching { roleLauncher.launch(roleIntent) }
                                    }
                                    onRefreshRoleStatus()
                                },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("blocklist_enable_role_button")
                            ) {
                                Text(stringResource(R.string.blocklist_enable_screening_button))
                            }
                        }
                    }
                }

                if (blockedNumbers.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Filled.Block,
                        title = stringResource(R.string.blocklist_empty_title),
                        subtitle = stringResource(R.string.blocklist_empty_subtitle),
                        actionLabel = stringResource(R.string.blocklist_add_number),
                        onAction = { showAddDialog = true }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 92.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(blockedNumbers, key = { it.id }) { entry ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("blocked_item_${entry.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Block,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.displayLabel ?: entry.normalizedNumber,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!entry.displayLabel.isNullOrBlank()) {
                                            Text(
                                                text = entry.normalizedNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onUnblock(entry) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("unblock_button_${entry.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = stringResource(R.string.blocklist_unblock),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .testTag("blocklist_add_fab")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.blocklist_add_number)
            )
        }
    }

    if (showAddDialog) {
        AddBlockedNumberDialog(
            hasContacts = availableContacts.isNotEmpty(),
            onPickFromContacts = {
                showAddDialog = false
                showContactPickerSheet = true
            },
            onConfirm = { number, label ->
                showAddDialog = false
                onAddBlockedNumber(number, label)
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showContactPickerSheet) {
        ModalBottomSheet(onDismissRequest = { showContactPickerSheet = false }) {
            val blockableContacts = remember(availableContacts, blockedNumbers) {
                availableContacts.filter { contact ->
                    contact.primaryNumber.isNotBlank() &&
                        blockedNumbers.none { entry ->
                            BlocklistMatcher.isMatch(contact.primaryNumber, entry.normalizedNumber)
                        }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.blocklist_pick_from_contacts),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
                if (blockableContacts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.blocklist_pick_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(blockableContacts, key = { it.id }) { contact ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        showContactPickerSheet = false
                                        onAddBlockedNumber(contact.primaryNumber, contact.displayName)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    ContactAvatar(
                                        name = contact.displayName,
                                        photoUri = contact.photoUri,
                                        size = 42.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = contact.primaryNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.Block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddBlockedNumberDialog(
    hasContacts: Boolean,
    onPickFromContacts: () -> Unit,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var number by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.blocklist_add_number)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = {
                        number = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.blocklist_number_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blocklist_number_input")
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.blocklist_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blocklist_label_input")
                )
                if (hasContacts) {
                    OutlinedButton(
                        onClick = onPickFromContacts,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Contacts,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.blocklist_pick_from_contacts))
                    }
                }
                if (showError) {
                    Text(
                        text = stringResource(R.string.blocklist_number_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (BlocklistMatcher.normalizeNumber(number).isEmpty()) {
                        showError = true
                    } else {
                        onConfirm(number, label)
                    }
                },
                modifier = Modifier.testTag("blocklist_confirm_add")
            ) {
                Text(stringResource(R.string.blocklist_block_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
