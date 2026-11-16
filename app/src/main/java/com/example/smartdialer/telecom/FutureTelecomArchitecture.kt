package com.example.smartdialer.telecom

import android.net.Uri
import com.example.smartdialer.data.model.SimAccount

/**
 * Architectural contracts reserved for Phase 2 implementation.
 * Phase 1 provides the UI, Navigation, Theme, Preferences, Room storage,
 * and safe Contacts/CallLog/TelecomManager foundations without simulating
 * an InCallService or fake call state.
 */
enum class CallAudioRoute {
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    WIRED_HEADSET
}

enum class Phase2CallState {
    IDLE,
    RINGING,
    DIALING,
    ACTIVE,
    HOLDING,
    DISCONNECTED
}

data class InCallUiModel(
    val callId: String,
    val displayName: String,
    val number: String,
    val state: Phase2CallState,
    val durationSeconds: Long,
    val isMuted: Boolean,
    val audioRoute: CallAudioRoute,
    val simAccount: SimAccount?
)

interface DialerInCallContract {
    fun answerCall(callId: String)
    fun disconnectCall(callId: String)
    fun toggleHold(callId: String)
    fun toggleMute(muted: Boolean)
    fun setAudioRoute(route: CallAudioRoute)
    fun sendDtmfTone(digit: Char)
}

interface CallRecordingCapability {
    fun isOfficiallySupportedOnDevice(): Boolean
}

interface LocalBackupContract {
    suspend fun exportLocalBackup(destinationUri: Uri): Result<Unit>
    suspend fun restoreLocalBackup(sourceUri: Uri): Result<Unit>
}
