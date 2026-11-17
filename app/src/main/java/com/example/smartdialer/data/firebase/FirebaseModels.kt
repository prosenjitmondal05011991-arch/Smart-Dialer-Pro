package com.example.smartdialer.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val autoCallRecordingEnabled: Boolean = false,
    val callRecordingScope: String = "ALL_CALLS",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedBlockedNumber(
    @DocumentId val id: String = "",
    val userId: String = "",
    val normalizedNumber: String = "",
    val displayLabel: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedFavoriteContact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val lookupKey: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val speedDialSlot: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedContactNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val lookupKey: String = "",
    val noteText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
