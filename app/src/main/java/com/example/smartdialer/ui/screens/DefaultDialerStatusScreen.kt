package com.example.smartdialer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.smartdialer.data.model.DefaultDialerStatus
import com.example.smartdialer.telecom.DialerTelecomManager
import com.example.smartdialer.ui.components.DialerSubTopBar
import com.example.smartdialer.ui.theme.LocalDialerExtendedColors

@Composable
fun DefaultDialerStatusScreen(
    status: DefaultDialerStatus,
    onRefreshStatus: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val extendedColors = LocalDialerExtendedColors.current

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshStatus()
    }

    LaunchedEffect(Unit) {
        onRefreshStatus()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DialerSubTopBar(
            title = stringResource(R.string.dialer_status_title),
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
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    val badgeColor = if (status.isDefaultDialer) {
                        extendedColors.callAccept
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = if (status.isDefaultDialer) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Filled.PhoneDisabled
                            },
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (status.isDefaultDialer) {
                            stringResource(R.string.dialer_status_confirmed_title)
                        } else {
                            stringResource(R.string.dialer_status_not_default_title)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("default_dialer_status_heading")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (status.isDefaultDialer) {
                            stringResource(R.string.dialer_status_confirmed_body)
                        } else {
                            stringResource(
                                R.string.dialer_status_not_default_body,
                                status.currentDefaultPackage ?: "System Dialer"
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    val roleIntent = DialerTelecomManager.createRequestDefaultDialerIntent(context)
                    Button(
                        onClick = {
                            if (roleIntent != null) {
                                runCatching { roleLauncher.launch(roleIntent) }
                            }
                            onRefreshStatus()
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("default_dialer_check_button")
                    ) {
                        Text(stringResource(R.string.dialer_status_request_button))
                    }
                }
            }

            // Phase 2 Incoming Call Blueprint Preview (matching Screenshot 2 aesthetic, clearly labeled non-functional preview)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.dialer_status_preview_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.dialer_status_preview_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF211B18),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 22.dp, horizontal = 16.dp)
                        ) {
                            // Message quick-reply pill
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFF342B26)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Message,
                                        contentDescription = null,
                                        tint = Color(0xFFE9BA96),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.contact_details_message),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFF5EFEA)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            // Decline / Center Call Pill / Answer Track
                            Surface(
                                shape = RoundedCornerShape(44.dp),
                                color = Color(0xFF332A25),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.call_preview_decline),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFF5EFEA)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(28.dp),
                                        color = Color.White,
                                        modifier = Modifier.size(width = 76.dp, height = 56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Call,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(R.string.call_preview_answer),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFF5EFEA)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Two pagination dots
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF120F0D)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.35f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.dialer_status_phase_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.dialer_status_roadmap_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.dialer_status_roadmap_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
