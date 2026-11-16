package com.example.smartdialer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.data.model.UiState
import com.example.smartdialer.permissions.PermissionHelper
import com.example.smartdialer.ui.components.ContactAvatar
import com.example.smartdialer.ui.components.DialerMainTopBar
import com.example.smartdialer.ui.components.EmptyStateCard
import com.example.smartdialer.ui.components.ErrorStateCard
import com.example.smartdialer.ui.components.LoadingStateView
import com.example.smartdialer.ui.components.PermissionRationaleCard
import com.example.smartdialer.ui.theme.LocalDialerExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoritesState: UiState<List<Contact>>,
    allContacts: List<Contact>,
    onCallNumber: (String) -> Unit,
    onOpenContactDetail: (Long) -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    onRequestContactsPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRetry: () -> Unit,
    onOpenBlockedNumbers: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenDefaultDialerStatus: () -> Unit
) {
    val context = LocalContext.current
    var showPickContactSheet by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DialerMainTopBar(
            title = stringResource(R.string.favorites_title),
            onSearchClick = null,
            onOpenBlockedNumbers = onOpenBlockedNumbers,
            onOpenSettings = onOpenSettings,
            onOpenDefaultDialerStatus = onOpenDefaultDialerStatus
        )

        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            if (favoritesState is UiState.Success) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.favorites_section_speed),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = { showPickContactSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("favorites_add_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.favorites_add_button))
                    }
                }
            }

            when (favoritesState) {
                is UiState.Loading -> LoadingStateView()

                is UiState.PermissionRequired -> {
                    PermissionRationaleCard(
                        icon = Icons.Filled.Contacts,
                        title = if (favoritesState.permanentlyDenied) {
                            stringResource(R.string.contacts_permission_denied_title)
                        } else {
                            stringResource(R.string.contacts_permission_title)
                        },
                        rationale = if (favoritesState.permanentlyDenied) {
                            stringResource(R.string.contacts_permission_denied_subtitle)
                        } else {
                            stringResource(R.string.contacts_permission_rationale)
                        },
                        permanentlyDenied = favoritesState.permanentlyDenied,
                        onGrantClick = onRequestContactsPermission,
                        onOpenSettingsClick = onOpenAppSettings
                    )
                }

                is UiState.Empty -> {
                    EmptyStateCard(
                        icon = Icons.Filled.StarOutline,
                        title = stringResource(R.string.favorites_empty_title),
                        subtitle = stringResource(R.string.favorites_empty_subtitle),
                        actionLabel = stringResource(R.string.favorites_add_button),
                        onAction = {
                            if (!PermissionHelper.hasContactsReadPermission(context)) {
                                onRequestContactsPermission()
                            } else {
                                showPickContactSheet = true
                            }
                        }
                    )
                }

                is UiState.Error -> {
                    ErrorStateCard(
                        message = favoritesState.message,
                        onRetry = onRetry
                    )
                }

                is UiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 156.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(favoritesState.data, key = { it.lookupKey }) { contact ->
                            FavoriteGridCard(
                                contact = contact,
                                onCall = { onCallNumber(contact.primaryNumber) },
                                onOpenDetails = {
                                    if (contact.id > 0L) onOpenContactDetail(contact.id)
                                },
                                onRemoveFavorite = { onToggleFavorite(contact) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPickContactSheet) {
        ModalBottomSheet(onDismissRequest = { showPickContactSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.favorites_pick_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
                val candidates = remember(allContacts) {
                    allContacts.filter { !it.isFavorite }
                }
                if (candidates.isEmpty()) {
                    Text(
                        text = stringResource(R.string.favorites_pick_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates, key = { it.id }) { contact ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onToggleFavorite(contact)
                                        showPickContactSheet = false
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    ContactAvatar(
                                        name = contact.displayName,
                                        photoUri = contact.photoUri,
                                        size = 44.dp
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
                                        imageVector = Icons.Filled.StarOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
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
private fun FavoriteGridCard(
    contact: Contact,
    onCall: () -> Unit,
    onOpenDetails: () -> Unit,
    onRemoveFavorite: () -> Unit
) {
    val extendedColors = LocalDialerExtendedColors.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenDetails)
            .testTag("favorite_card_${contact.id}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onRemoveFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("favorite_unstar_${contact.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(R.string.favorites_remove),
                        tint = extendedColors.starGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            ContactAvatar(
                name = contact.displayName,
                photoUri = contact.photoUri,
                size = 64.dp,
                showStarBadge = false
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = contact.primaryNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = extendedColors.callAccept.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onCall)
                    .minimumInteractiveComponentSize()
                    .testTag("favorite_call_${contact.id}")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = stringResource(
                            R.string.action_call_contact,
                            contact.displayName
                        ),
                        tint = extendedColors.callAccept,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_call),
                        style = MaterialTheme.typography.labelLarge,
                        color = extendedColors.callAccept
                    )
                }
            }
        }
    }
}
