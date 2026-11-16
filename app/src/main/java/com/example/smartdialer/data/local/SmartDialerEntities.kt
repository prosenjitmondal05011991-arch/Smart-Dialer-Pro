package com.example.smartdialer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_contacts")
data class FavoriteContactEntity(
    @PrimaryKey val lookupKey: String,
    val contactId: Long,
    val displayName: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val speedDialSlot: Int? = null,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contact_notes")
data class ContactNoteEntity(
    @PrimaryKey val lookupKey: String,
    val noteText: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_numbers")
data class BlockedNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val normalizedNumber: String,
    val displayLabel: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
