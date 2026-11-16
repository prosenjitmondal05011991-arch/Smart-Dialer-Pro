package com.example.smartdialer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.smartdialer.data.model.SimAccount
import com.example.smartdialer.data.model.T9Match
import com.example.smartdialer.telecom.DialerTelecomManager
import com.example.smartdialer.ui.components.ContactAvatar
import com.example.smartdialer.ui.components.DialerMainTopBar
import com.example.smartdialer.ui.theme.LocalDialerExtendedColors

private data class DialKeySpec(
    val primary: String,
    val secondary: String = "",
    val isVoicemail: Boolean = false
)

private val DialPadRows = listOf(
    listOf(
        DialKeySpec("1", isVoicemail = true),
        DialKeySpec("2", "ABC"),
        DialKeySpec("3", "DEF")
    ),
    listOf(
        DialKeySpec("4", "GHI"),
        DialKeySpec("5", "JKL"),
        DialKeySpec("6", "MNO")
    ),
    listOf(
        DialKeySpec("7", "PQRS"),
        DialKeySpec("8", "TUV"),
        DialKeySpec("9", "WXYZ")
    ),
    listOf(
        DialKeySpec("*", ","),
        DialKeySpec("0", "+"),
        DialKeySpec("#", ";")
    )
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun KeypadScreen(
    rawDigits: String,
    formattedDigits: String,
    t9Matches: List<T9Match>,
    availableSims: List<SimAccount>,
    selectedSimSlot: Int,
    vibrationEnabled: Boolean,
    onDigitPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    onSelectSim: (Int) -> Unit,
    onPlaceCall: (String, SimAccount?) -> Unit,
    onAddContactWithNumber: (String) -> Unit,
    onOpenContactDetail: (Long) -> Unit,
    onOpenBlockedNumbers: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenDefaultDialerStatus: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val extendedColors = LocalDialerExtendedColors.current
    var showAllMatchesSheet by remember { mutableStateOf(false) }
    var simDropdownExpanded by remember { mutableStateOf(false) }
    var keypadVisible by remember { mutableStateOf(true) }

    val selectedSim = remember(availableSims, selectedSimSlot) {
        availableSims.firstOrNull { it.slotIndex == selectedSimSlot } ?: availableSims.firstOrNull()
    }

    val performFeedback = {
        if (vibrationEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            DialerTelecomManager.vibrateKeyTap(context)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DialerMainTopBar(
            title = stringResource(R.string.app_name),
            showAppEmblem = true,
            onSearchClick = null,
            onOpenBlockedNumbers = onOpenBlockedNumbers,
            onOpenSettings = onOpenSettings,
            onOpenDefaultDialerStatus = onOpenDefaultDialerStatus
        )

        BoxWithConstraints(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
        ) {
            val compactHeight = maxHeight < 580.dp
            val keyHeight = if (compactHeight) 56.dp else 66.dp
            val rowSpacing = if (compactHeight) 2.dp else 6.dp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Upper Area: T9 Suggestions + Formatted Number Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    AnimatedVisibility(
                        visible = t9Matches.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val topMatch = t9Matches.first()
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { onOpenContactDetail(topMatch.contact.id) }
                                .testTag("keypad_t9_top_match")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                ContactAvatar(
                                    name = topMatch.contact.displayName,
                                    photoUri = topMatch.contact.photoUri,
                                    size = 42.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = topMatch.contact.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = topMatch.matchedNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (t9Matches.size > 1) {
                                    TextButton(
                                        onClick = { showAllMatchesSheet = true },
                                        modifier = Modifier.testTag("keypad_t9_more_button")
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.keypad_t9_more_matches,
                                                t9Matches.size
                                            ),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onPlaceCall(topMatch.matchedNumber, selectedSim) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("keypad_t9_call_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = stringResource(
                                            R.string.action_call_contact,
                                            topMatch.contact.displayName
                                        ),
                                        tint = extendedColors.callAccept
                                    )
                                }
                            }
                        }
                    }

                    if (rawDigits.isNotEmpty() && t9Matches.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onAddContactWithNumber(rawDigits) }
                                .testTag("keypad_add_contact_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.keypad_add_to_contacts),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // SIM indicator chip when multiple SIMs are present
                    if (availableSims.size > 1) {
                        Box(modifier = Modifier.padding(bottom = 6.dp)) {
                            val simLabel = selectedSim?.let {
                                stringResource(R.string.keypad_sim_slot, it.slotIndex)
                            } ?: stringResource(R.string.keypad_sim_default)

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { simDropdownExpanded = true }
                                    .testTag("keypad_sim_selector")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SimCard,
                                        contentDescription = stringResource(R.string.keypad_sim_selector),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = simLabel,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = simDropdownExpanded,
                                onDismissRequest = { simDropdownExpanded = false }
                            ) {
                                availableSims.forEach { sim ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.keypad_sim_slot, sim.slotIndex),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (sim.carrierName.isNotBlank()) {
                                                    Text(
                                                        text = sim.carrierName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Filled.SimCard, contentDescription = null)
                                        },
                                        onClick = {
                                            simDropdownExpanded = false
                                            onSelectSim(sim.slotIndex)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Large Centered Number Input Display
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        if (formattedDigits.isEmpty()) {
                            Text(
                                text = stringResource(R.string.keypad_enter_number),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val fontSize = when {
                                formattedDigits.length > 16 -> 24.sp
                                formattedDigits.length > 12 -> 30.sp
                                else -> 36.sp
                            }
                            Text(
                                text = formattedDigits,
                                fontSize = fontSize,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("keypad_number_display")
                            )
                        }
                    }
                }

                // Bottom-Sheet Style Rounded Dialpad Surface (matching Screenshot 1)
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Subtle sheet drag handle with accessible touch area
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { keypadVisible = !keypadVisible }
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            )
                        }

                        AnimatedVisibility(visible = keypadVisible) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(rowSpacing),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                DialPadRows.forEach { row ->
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        row.forEach { spec ->
                                            ExpressiveDialKey(
                                                spec = spec,
                                                keyHeight = keyHeight,
                                                onTap = {
                                                    performFeedback()
                                                    onDigitPress(spec.primary.first())
                                                },
                                                onLongTap = {
                                                    if (spec.primary == "0") {
                                                        performFeedback()
                                                        onDigitPress('+')
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Action Row: Dialpad Toggle | Green Circle Call Button | Backspace
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            // Left: Toggle keypad or Add to Contacts
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = { keypadVisible = !keypadVisible },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .testTag("keypad_toggle_visibility")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Dialpad,
                                        contentDescription = if (keypadVisible) {
                                            stringResource(R.string.keypad_hide_pad)
                                        } else {
                                            stringResource(R.string.keypad_show_pad)
                                        },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // Center: Circular Vibrant Green Call Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = extendedColors.callAccept,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            performFeedback()
                                            onPlaceCall(rawDigits, selectedSim)
                                        }
                                        .testTag("keypad_call_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.PhoneInTalk,
                                            contentDescription = stringResource(R.string.keypad_call_button),
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            // Right: Outlined Backspace Button (Tap = backspace, Long-press = clear all)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .combinedClickable(
                                            onClick = {
                                                if (rawDigits.isNotEmpty()) {
                                                    performFeedback()
                                                    onBackspace()
                                                }
                                            },
                                            onLongClick = {
                                                if (rawDigits.isNotEmpty()) {
                                                    performFeedback()
                                                    onClearAll()
                                                }
                                            }
                                        )
                                        .testTag("keypad_backspace_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                        contentDescription = stringResource(R.string.keypad_backspace),
                                        tint = if (rawDigits.isNotEmpty()) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        },
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAllMatchesSheet && t9Matches.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showAllMatchesSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.keypad_t9_more_matches, t9Matches.size),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(t9Matches, key = { "${it.contact.id}_${it.matchedNumber}" }) { match ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    showAllMatchesSheet = false
                                    onOpenContactDetail(match.contact.id)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                ContactAvatar(
                                    name = match.contact.displayName,
                                    photoUri = match.contact.photoUri,
                                    size = 44.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = match.contact.displayName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = match.matchedNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        showAllMatchesSheet = false
                                        onPlaceCall(match.matchedNumber, selectedSim)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = stringResource(
                                            R.string.action_call_contact,
                                            match.contact.displayName
                                        ),
                                        tint = extendedColors.callAccept
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpressiveDialKey(
    spec: DialKeySpec,
    keyHeight: Dp,
    onTap: () -> Unit,
    onLongTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(keyHeight)
            .clip(RoundedCornerShape(32.dp))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongTap
            )
            .testTag("dial_key_${spec.primary}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = spec.primary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            when {
                spec.isVoicemail -> {
                    Icon(
                        imageVector = Icons.Filled.Voicemail,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
                spec.secondary.isNotEmpty() -> {
                    Text(
                        text = spec.secondary,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
