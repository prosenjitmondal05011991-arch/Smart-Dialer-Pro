package com.example.smartdialer.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.smartdialer.data.model.CallDirection
import com.example.smartdialer.data.model.CallFilter
import com.example.smartdialer.data.model.CallLogEntry
import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.data.model.UiState
import com.example.smartdialer.ui.components.ContactAvatar
import com.example.smartdialer.ui.components.DialerSearchPillBar
import com.example.smartdialer.ui.components.EmptyStateCard
import com.example.smartdialer.ui.components.ErrorStateCard
import com.example.smartdialer.ui.components.LoadingStateView
import com.example.smartdialer.ui.components.PermissionRationaleCard
import com.example.smartdialer.ui.theme.LocalDialerExtendedColors
import java.text.DateFormat
import java.util.Date

@Composable
fun RecentsScreen(
    callLogState: UiState<List<CallLogEntry>>,
    favorites: List<Contact>,
    activeFilter: CallFilter,
    searchQuery: String,
    onFilterChange: (CallFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRequestCallLogPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRetry: () -> Unit,
    onCallNumber: (String) -> Unit,
    onPrefillKeypad: (String) -> Unit,
    onBlockNumber: (String, String?) -> Unit = { _, _ -> },
    onOpenContactDetail: (Long) -> Unit,
    onOpenKeypad: () -> Unit,
    onOpenBlockedNumbers: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenDefaultDialerStatus: () -> Unit
) {
    var favoritesExpanded by remember { mutableStateOf(false) }
    var pendingBlockEntry by remember { mutableStateOf<CallLogEntry?>(null) }

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
                    placeholder = stringResource(R.string.recents_search_hint),
                    onQueryChange = onSearchQueryChange,
                    onOpenBlockedNumbers = onOpenBlockedNumbers,
                    onOpenSettings = onOpenSettings,
                    onOpenDefaultDialerStatus = onOpenDefaultDialerStatus
                )

                // Outlined/Tonal Filter Chips Row matching Screenshot 3
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        CallFilter.ALL to R.string.recents_filter_all,
                        CallFilter.MISSED to R.string.recents_filter_missed,
                        CallFilter.OUTGOING to R.string.recents_filter_outgoing,
                        CallFilter.INCOMING to R.string.recents_filter_incoming
                    )
                    items(filters, key = { it.first.name }) { (filter, labelRes) ->
                        val selected = activeFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterChange(filter) },
                            label = {
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = if (selected) {
                                null
                            } else {
                                FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("recents_filter_${filter.name.lowercase()}")
                        )
                    }
                }

                // Collapsible Favourites Row matching Screenshot 3
                if (favorites.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { favoritesExpanded = !favoritesExpanded }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .testTag("recents_favourites_toggle")
                    ) {
                        Text(
                            text = stringResource(R.string.favorites_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (favoritesExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = favoritesExpanded) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            items(favorites.take(10), key = { it.lookupKey }) { fav ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            if (fav.id > 0L) {
                                                onOpenContactDetail(fav.id)
                                            } else {
                                                onCallNumber(fav.primaryNumber)
                                            }
                                        }
                                        .padding(4.dp)
                                ) {
                                    ContactAvatar(
                                        name = fav.displayName,
                                        photoUri = fav.photoUri,
                                        size = 48.dp,
                                        showStarBadge = true
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fav.displayName.substringBefore(" "),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                when (callLogState) {
                    is UiState.Loading -> LoadingStateView()

                    is UiState.PermissionRequired -> {
                        PermissionRationaleCard(
                            icon = Icons.Filled.History,
                            title = if (callLogState.permanentlyDenied) {
                                stringResource(R.string.recents_permission_denied_title)
                            } else {
                                stringResource(R.string.recents_permission_title)
                            },
                            rationale = if (callLogState.permanentlyDenied) {
                                stringResource(R.string.recents_permission_denied_subtitle)
                            } else {
                                stringResource(R.string.recents_permission_rationale)
                            },
                            permanentlyDenied = callLogState.permanentlyDenied,
                            onGrantClick = onRequestCallLogPermission,
                            onOpenSettingsClick = onOpenAppSettings
                        )
                    }

                    is UiState.Empty -> {
                        EmptyStateCard(
                            icon = Icons.Filled.History,
                            title = if (callLogState.filteredEmpty) {
                                stringResource(R.string.recents_filter_empty_title)
                            } else {
                                stringResource(R.string.recents_empty_title)
                            },
                            subtitle = if (callLogState.filteredEmpty) {
                                stringResource(R.string.recents_filter_empty_subtitle)
                            } else {
                                stringResource(R.string.recents_empty_subtitle)
                            },
                            actionLabel = if (!callLogState.filteredEmpty) {
                                stringResource(R.string.recents_open_keypad)
                            } else null,
                            onAction = if (!callLogState.filteredEmpty) onOpenKeypad else null
                        )
                    }

                    is UiState.Error -> {
                        ErrorStateCard(
                            message = callLogState.message,
                            onRetry = onRetry
                        )
                    }

                    is UiState.Success -> {
                        val todayLabel = stringResource(R.string.recents_today)
                        val yesterdayLabel = stringResource(R.string.recents_yesterday)
                        val grouped = remember(callLogState.data, todayLabel, yesterdayLabel) {
                            groupCallsByDate(callLogState.data, todayLabel, yesterdayLabel)
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(top = 6.dp, bottom = 92.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            grouped.forEach { (sectionTitle, sectionEntries) ->
                                item(key = "header_$sectionTitle") {
                                    Text(
                                        text = sectionTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(
                                            horizontal = 20.dp,
                                            vertical = 10.dp
                                        )
                                    )
                                }
                                items(sectionEntries, key = { it.id }) { entry ->
                                    CallLogCleanRow(
                                        entry = entry,
                                        onRowClick = {
                                            if (entry.number.isNotBlank()) {
                                                onPrefillKeypad(entry.number)
                                            }
                                        },
                                        onRowLongClick = {
                                            if (entry.number.isNotBlank()) {
                                                pendingBlockEntry = entry
                                            }
                                        },
                                        onCallClick = {
                                            if (entry.number.isNotBlank()) {
                                                onCallNumber(entry.number)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onOpenKeypad,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .testTag("recents_keypad_fab")
        ) {
            Icon(
                imageVector = Icons.Filled.Dialpad,
                contentDescription = stringResource(R.string.recents_open_keypad)
            )
        }
    }

    pendingBlockEntry?.let { entry ->
        val label = entry.cachedName ?: entry.formattedNumber.ifBlank { entry.number }
        AlertDialog(
            onDismissRequest = { pendingBlockEntry = null },
            title = { Text(stringResource(R.string.blocklist_recents_block_title, label)) },
            text = { Text(stringResource(R.string.blocklist_recents_block_message, label)) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingBlockEntry = null
                        onBlockNumber(entry.number, entry.cachedName)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.blocklist_block_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBlockEntry = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallLogCleanRow(
    entry: CallLogEntry,
    onRowClick: () -> Unit,
    onRowLongClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val extendedColors = LocalDialerExtendedColors.current
    val displayTitle = entry.cachedName
        ?: entry.formattedNumber.ifBlank { stringResource(R.string.recents_unknown_caller) }

    val (directionIcon, directionTint, typeLabelRes) = when (entry.direction) {
        CallDirection.MISSED -> Triple(
            Icons.AutoMirrored.Filled.CallMissed,
            extendedColors.callMissed,
            R.string.recents_type_missed
        )
        CallDirection.INCOMING -> Triple(
            Icons.AutoMirrored.Filled.CallReceived,
            extendedColors.callIncoming,
            R.string.recents_type_incoming
        )
        CallDirection.OUTGOING -> Triple(
            Icons.AutoMirrored.Filled.CallMade,
            extendedColors.callOutgoing,
            R.string.recents_type_outgoing
        )
        CallDirection.REJECTED -> Triple(
            Icons.AutoMirrored.Filled.CallMissed,
            extendedColors.callMissed,
            R.string.recents_type_rejected
        )
        CallDirection.VOICEMAIL -> Triple(
            Icons.Filled.Voicemail,
            MaterialTheme.colorScheme.primary,
            R.string.recents_type_voicemail
        )
        CallDirection.UNKNOWN -> Triple(
            Icons.Filled.Call,
            MaterialTheme.colorScheme.onSurfaceVariant,
            R.string.recents_type_unknown
        )
    }

    val relativeTime = formatRelativeCallTime(entry.timestampMillis)
    val durationText = remember(entry.durationSeconds) { formatDuration(entry.durationSeconds) }
    val subtitleText = buildString {
        append(stringResource(typeLabelRes))
        if (relativeTime.isNotEmpty()) {
            append(" • ")
            append(relativeTime)
        }
        if (durationText != null) {
            append(" • ")
            append(durationText)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onRowClick,
                onLongClick = onRowLongClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTag("call_log_item_${entry.id}")
    ) {
        ContactAvatar(
            name = displayTitle,
            photoUri = entry.photoUri,
            size = 44.dp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = if (entry.direction == CallDirection.MISSED) {
                    extendedColors.callMissed
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = directionIcon,
                    contentDescription = null,
                    tint = directionTint,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = onCallClick,
            enabled = entry.number.isNotBlank(),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("call_log_call_btn_${entry.id}")
        ) {
            Icon(
                imageVector = Icons.Outlined.Call,
                contentDescription = stringResource(R.string.action_call_contact, displayTitle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun groupCallsByDate(
    entries: List<CallLogEntry>,
    todayLabel: String,
    yesterdayLabel: String
): List<Pair<String, List<CallLogEntry>>> {
    val result = linkedMapOf<String, MutableList<CallLogEntry>>()
    val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    for (entry in entries) {
        val header = when {
            DateUtils.isToday(entry.timestampMillis) -> todayLabel
            DateUtils.isToday(entry.timestampMillis + DateUtils.DAY_IN_MILLIS) -> yesterdayLabel
            else -> dateFormat.format(Date(entry.timestampMillis))
        }
        result.getOrPut(header) { mutableListOf() }.add(entry)
    }
    return result.entries.map { it.key to it.value }
}

@Composable
private fun formatRelativeCallTime(millis: Long): String {
    if (millis <= 0L) return ""
    val diffMillis = (System.currentTimeMillis() - millis).coerceAtLeast(0L)
    val mins = (diffMillis / 60_000L).toInt()
    val hours = (diffMillis / 3_600_000L).toInt()
    return when {
        mins < 1 -> stringResource(R.string.recents_just_now)
        mins < 60 -> stringResource(R.string.recents_min_ago, mins)
        hours < 12 && DateUtils.isToday(millis) -> stringResource(R.string.recents_hr_ago, hours)
        else -> DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
    }
}

private fun formatDuration(seconds: Long): String? {
    if (seconds <= 0L) return null
    val mins = seconds / 60
    val remSecs = seconds % 60
    return when {
        mins >= 60 -> "${mins / 60}h ${mins % 60}m"
        mins > 0 -> "${mins}m ${remSecs}s"
        else -> "${remSecs}s"
    }
}
