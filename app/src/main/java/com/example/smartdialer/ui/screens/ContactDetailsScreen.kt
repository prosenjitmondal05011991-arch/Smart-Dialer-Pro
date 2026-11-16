package com.example.smartdialer.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.data.model.UiState
import com.example.smartdialer.ui.components.ContactAvatar
import com.example.smartdialer.ui.components.DialerSubTopBar
import com.example.smartdialer.ui.components.ErrorStateCard
import com.example.smartdialer.ui.components.LoadingStateView
import com.example.smartdialer.ui.theme.LocalDialerExtendedColors

@Composable
fun ContactDetailsScreen(
    detailState: UiState<Contact>,
    localNote: String,
    isBlocked: Boolean = false,
    onBack: () -> Unit,
    onCallNumber: (String) -> Unit,
    onMessageNumber: (String) -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    onToggleBlock: (Contact, Boolean) -> Unit = { _, _ -> },
    onEditContact: (Contact) -> Unit,
    onDeleteContactConfirmed: (Contact) -> Unit,
    onSaveNote: (String, String) -> Unit,
    onRetry: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val extendedColors = LocalDialerExtendedColors.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DialerSubTopBar(
            title = stringResource(R.string.contact_details_title),
            onBack = onBack,
            actions = {
                val contact = (detailState as? UiState.Success)?.data
                if (contact != null) {
                    IconButton(
                        onClick = { onEditContact(contact) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("contact_details_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.action_edit)
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("contact_details_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )

        when (detailState) {
            is UiState.Loading -> LoadingStateView()
            is UiState.Error -> ErrorStateCard(message = detailState.message, onRetry = onRetry)
            is UiState.Empty, is UiState.PermissionRequired -> Unit
            is UiState.Success -> {
                val contact = detailState.data
                var noteDraft by remember(contact.lookupKey, localNote) {
                    mutableStateOf(localNote)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Hero Identity Card
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            ContactAvatar(
                                name = contact.displayName,
                                photoUri = contact.photoUri,
                                size = 88.dp,
                                showStarBadge = contact.isFavorite
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = contact.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            if (!contact.organization.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = contact.organization,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))

                            // Action Pills Row
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                QuickDetailActionButton(
                                    icon = Icons.Filled.Call,
                                    label = stringResource(R.string.action_call),
                                    tint = extendedColors.callAccept,
                                    onClick = { onCallNumber(contact.primaryNumber) },
                                    testTag = "detail_action_call"
                                )
                                QuickDetailActionButton(
                                    icon = Icons.AutoMirrored.Filled.Message,
                                    label = stringResource(R.string.contact_details_message),
                                    tint = MaterialTheme.colorScheme.primary,
                                    onClick = { onMessageNumber(contact.primaryNumber) },
                                    testTag = "detail_action_message"
                                )
                                QuickDetailActionButton(
                                    icon = if (contact.isFavorite) Icons.Filled.Star else Icons.Filled.StarOutline,
                                    label = if (contact.isFavorite) {
                                        stringResource(R.string.contact_details_favorite_remove)
                                    } else {
                                        stringResource(R.string.contact_details_favorite_add)
                                    },
                                    tint = if (contact.isFavorite) extendedColors.starGold else MaterialTheme.colorScheme.primary,
                                    onClick = { onToggleFavorite(contact) },
                                    testTag = "detail_action_favorite"
                                )
                                QuickDetailActionButton(
                                    icon = Icons.Filled.Block,
                                    label = if (isBlocked) {
                                        stringResource(R.string.blocklist_unblock_action)
                                    } else {
                                        stringResource(R.string.blocklist_block_action)
                                    },
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = { onToggleBlock(contact, !isBlocked) },
                                    testTag = "detail_action_block"
                                )
                            }
                        }
                    }

                    // Phone Numbers Card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.contact_details_phone_numbers),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            val numbers = contact.phoneNumbers.ifEmpty {
                                listOf(
                                    com.example.smartdialer.data.model.PhoneNumberEntry(
                                        number = contact.primaryNumber,
                                        normalizedNumber = contact.primaryNumber,
                                        label = "Mobile"
                                    )
                                )
                            }
                            val copiedMsg = stringResource(R.string.contact_details_copied)
                            numbers.forEach { entry ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onCallNumber(entry.number) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.number,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = entry.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(ClipData.newPlainText("phone", entry.number))
                                            onShowMessage(copiedMsg)
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = stringResource(R.string.contact_details_copy_number),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onCallNumber(entry.number) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Call,
                                            contentDescription = stringResource(R.string.action_call),
                                            tint = extendedColors.callAccept
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Emails & Organization Card
                    if (contact.emails.isNotEmpty() || !contact.organization.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                if (contact.emails.isNotEmpty()) {
                                    Text(
                                        text = stringResource(R.string.contact_details_emails),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    contact.emails.forEach { email ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Email,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(text = email, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                                if (!contact.organization.isNullOrBlank()) {
                                    Text(
                                        text = stringResource(R.string.contact_details_organization),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Business,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = contact.organization,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Local Private Dialer Note (Room)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.contact_details_notes),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = noteDraft,
                                onValueChange = { noteDraft = it },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("contact_local_note_field")
                            )
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { onSaveNote(contact.lookupKey, noteDraft) },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.testTag("contact_save_note_button")
                                ) {
                                    Text(stringResource(R.string.action_save))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text(stringResource(R.string.contact_details_delete_title)) },
                        text = {
                            Text(
                                stringResource(
                                    R.string.contact_details_delete_message,
                                    contact.displayName
                                )
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteConfirm = false
                                    onDeleteContactConfirmed(contact)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.testTag("confirm_delete_contact_button")
                            ) {
                                Text(stringResource(R.string.action_delete))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickDetailActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
