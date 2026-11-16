package com.example.smartdialer.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.data.model.UiState
import com.example.smartdialer.ui.components.ContactAvatar
import com.example.smartdialer.ui.components.DialerSearchPillBar
import com.example.smartdialer.ui.components.EmptyStateCard
import com.example.smartdialer.ui.components.ErrorStateCard
import com.example.smartdialer.ui.components.LoadingStateView
import com.example.smartdialer.ui.components.PermissionRationaleCard

@Composable
fun ContactsScreen(
    contactsState: UiState<List<Contact>>,
    favorites: List<Contact>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRequestContactsPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRetry: () -> Unit,
    onOpenContactDetail: (Long) -> Unit,
    onCallNumber: (String) -> Unit,
    onAddNewContact: () -> Unit,
    onSeeAllFavorites: () -> Unit,
    onOpenBlockedNumbers: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenDefaultDialerStatus: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
            ) {
                DialerSearchPillBar(
                    query = searchQuery,
                    placeholder = stringResource(R.string.contacts_search_hint),
                    onQueryChange = onSearchQueryChange,
                    onOpenBlockedNumbers = onOpenBlockedNumbers,
                    onOpenSettings = onOpenSettings,
                    onOpenDefaultDialerStatus = onOpenDefaultDialerStatus
                )

                when (contactsState) {
                    is UiState.Loading -> LoadingStateView()

                    is UiState.PermissionRequired -> {
                        PermissionRationaleCard(
                            icon = Icons.Filled.Contacts,
                            title = if (contactsState.permanentlyDenied) {
                                stringResource(R.string.contacts_permission_denied_title)
                            } else {
                                stringResource(R.string.contacts_permission_title)
                            },
                            rationale = if (contactsState.permanentlyDenied) {
                                stringResource(R.string.contacts_permission_denied_subtitle)
                            } else {
                                stringResource(R.string.contacts_permission_rationale)
                            },
                            permanentlyDenied = contactsState.permanentlyDenied,
                            onGrantClick = onRequestContactsPermission,
                            onOpenSettingsClick = onOpenAppSettings
                        )
                    }

                    is UiState.Error -> {
                        ErrorStateCard(
                            message = contactsState.message,
                            onRetry = onRetry
                        )
                    }

                    is UiState.Empty -> {
                        EmptyStateCard(
                            icon = Icons.Filled.Contacts,
                            title = stringResource(R.string.contacts_empty_title),
                            subtitle = stringResource(R.string.contacts_empty_subtitle),
                            actionLabel = if (!contactsState.filteredEmpty) {
                                stringResource(R.string.contacts_add_new)
                            } else null,
                            onAction = if (!contactsState.filteredEmpty) onAddNewContact else null
                        )
                    }

                    is UiState.Success -> {
                        val contacts = contactsState.data
                        LazyColumn(
                            contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (searchQuery.isBlank()) {
                                item(key = "my_profile_card") {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable(onClick = onOpenSettings)
                                            .testTag("contacts_my_profile_card")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(14.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(R.string.contacts_my_profile),
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = stringResource(R.string.contacts_my_profile_subtitle),
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
                                }

                                if (favorites.isNotEmpty()) {
                                    item(key = "favorites_strip") {
                                        Column(modifier = Modifier.padding(top = 6.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 18.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.contacts_favorites_strip),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                TextButton(onClick = onSeeAllFavorites) {
                                                    Text(stringResource(R.string.contacts_see_all))
                                                }
                                            }
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                contentPadding = PaddingValues(
                                                    horizontal = 16.dp,
                                                    vertical = 4.dp
                                                )
                                            ) {
                                                items(favorites.take(10), key = { it.lookupKey }) { fav ->
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier
                                                            .width(66.dp)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .clickable {
                                                                if (fav.id > 0L) onOpenContactDetail(fav.id)
                                                            }
                                                            .padding(4.dp)
                                                    ) {
                                                        ContactAvatar(
                                                            name = fav.displayName,
                                                            photoUri = fav.photoUri,
                                                            size = 52.dp,
                                                            showStarBadge = true
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = fav.displayName.substringBefore(" "),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item(key = "all_contacts_header") {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.contacts_all_section),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.contacts_count_label, contacts.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(contacts, key = { it.id }) { contact ->
                                ContactCleanRow(
                                    contact = contact,
                                    onClick = { onOpenContactDetail(contact.id) },
                                    onCallClick = { onCallNumber(contact.primaryNumber) }
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddNewContact,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .testTag("contacts_add_fab")
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = stringResource(R.string.contacts_add_new)
            )
        }
    }
}

@Composable
private fun ContactCleanRow(
    contact: Contact,
    onClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTag("contact_row_${contact.id}")
    ) {
        ContactAvatar(
            name = contact.displayName,
            photoUri = contact.photoUri,
            size = 44.dp,
            showStarBadge = contact.isFavorite
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = contact.primaryNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onCallClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("contact_call_${contact.id}")
        ) {
            Icon(
                imageVector = Icons.Outlined.Call,
                contentDescription = stringResource(
                    R.string.action_call_contact,
                    contact.displayName
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
