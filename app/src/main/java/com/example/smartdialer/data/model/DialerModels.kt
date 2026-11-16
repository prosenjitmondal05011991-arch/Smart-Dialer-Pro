package com.example.smartdialer.data.model

import android.telecom.PhoneAccountHandle

data class PhoneNumberEntry(
    val number: String,
    val normalizedNumber: String,
    val label: String,
    val isPrimary: Boolean = false
)

data class Contact(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val primaryNumber: String,
    val phoneNumbers: List<PhoneNumberEntry> = emptyList(),
    val photoUri: String? = null,
    val isStarred: Boolean = false,
    val isLocalFavorite: Boolean = false,
    val emails: List<String> = emptyList(),
    val organization: String? = null
) {
    val isFavorite: Boolean
        get() = isStarred || isLocalFavorite

    val initials: String
        get() {
            val parts = displayName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            return when {
                parts.isEmpty() -> "?"
                parts.size == 1 -> parts.first().take(1).uppercase()
                else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
            }
        }
}

data class ContactDraft(
    val contactId: Long? = null,
    val displayName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val organization: String = "",
    val photoUri: String? = null,
    val localNote: String = ""
)

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    VOICEMAIL,
    UNKNOWN
}

enum class CallFilter {
    ALL,
    MISSED,
    OUTGOING,
    INCOMING
}

data class CallLogEntry(
    val id: Long,
    val cachedName: String?,
    val number: String,
    val formattedNumber: String,
    val direction: CallDirection,
    val timestampMillis: Long,
    val durationSeconds: Long,
    val photoUri: String? = null,
    val simLabel: String? = null
)

data class T9Match(
    val contact: Contact,
    val matchedNumber: String,
    val score: Int
)

data class SimAccount(
    val slotIndex: Int,
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val phoneAccountHandle: PhoneAccountHandle? = null
)

data class DefaultDialerStatus(
    val isDefaultDialer: Boolean,
    val currentDefaultPackage: String?,
    val supportsRoleManager: Boolean
)
