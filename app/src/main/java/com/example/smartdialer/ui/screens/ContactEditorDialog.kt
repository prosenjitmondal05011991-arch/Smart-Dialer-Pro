package com.example.smartdialer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.smartdialer.data.model.ContactDraft
import com.example.smartdialer.ui.components.ContactAvatar

@Composable
fun ContactEditorDialog(
    initialDraft: ContactDraft,
    onSave: (ContactDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialDraft) { mutableStateOf(initialDraft.displayName) }
    var phone by remember(initialDraft) { mutableStateOf(initialDraft.phoneNumber) }
    var email by remember(initialDraft) { mutableStateOf(initialDraft.email) }
    var organization by remember(initialDraft) { mutableStateOf(initialDraft.organization) }
    var photoUri by remember(initialDraft) { mutableStateOf(initialDraft.photoUri) }
    var showValidationError by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialDraft.contactId == null) {
                    stringResource(R.string.contact_editor_add_title)
                } else {
                    stringResource(R.string.contact_editor_edit_title)
                }
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ContactAvatar(
                        name = name.ifBlank { "?" },
                        photoUri = photoUri,
                        size = 56.dp
                    )
                    if (initialDraft.contactId == null) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(stringResource(R.string.contact_editor_pick_photo))
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showValidationError = false
                    },
                    label = { Text(stringResource(R.string.contact_editor_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        showValidationError = false
                    },
                    label = { Text(stringResource(R.string.contact_editor_phone_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_phone_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.contact_editor_email_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_email_input")
                )

                OutlinedTextField(
                    value = organization,
                    onValueChange = { organization = it },
                    label = { Text(stringResource(R.string.contact_editor_company_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_org_input")
                )

                if (showValidationError) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.contact_editor_validation_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank()) {
                        showValidationError = true
                    } else {
                        onSave(
                            initialDraft.copy(
                                displayName = name.trim(),
                                phoneNumber = phone.trim(),
                                email = email.trim(),
                                organization = organization.trim(),
                                photoUri = photoUri
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("editor_save_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
