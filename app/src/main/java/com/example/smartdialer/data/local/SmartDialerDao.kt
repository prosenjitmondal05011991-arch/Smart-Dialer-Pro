package com.example.smartdialer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteContactDao {
    @Query("SELECT * FROM favorite_contacts ORDER BY COALESCE(speedDialSlot, 999) ASC, addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(entity: FavoriteContactEntity)

    @Query("DELETE FROM favorite_contacts WHERE lookupKey = :lookupKey")
    suspend fun deleteByLookupKey(lookupKey: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_contacts WHERE lookupKey = :lookupKey)")
    suspend fun isFavorite(lookupKey: String): Boolean
}

@Dao
interface ContactNoteDao {
    @Query("SELECT * FROM contact_notes WHERE lookupKey = :lookupKey LIMIT 1")
    fun observeNote(lookupKey: String): Flow<ContactNoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(entity: ContactNoteEntity)

    @Query("DELETE FROM contact_notes WHERE lookupKey = :lookupKey")
    suspend fun deleteNote(lookupKey: String)
}

@Dao
interface BlockedNumberDao {
    @Query("SELECT * FROM blocked_numbers ORDER BY createdAt DESC")
    fun observeBlockedNumbers(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT * FROM blocked_numbers")
    suspend fun getAllBlockedSnapshot(): List<BlockedNumberEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE normalizedNumber = :normalizedNumber)")
    suspend fun existsByNormalizedNumber(normalizedNumber: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocked(entity: BlockedNumberEntity)

    @Query("DELETE FROM blocked_numbers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blocked_numbers WHERE normalizedNumber = :normalizedNumber")
    suspend fun deleteByNormalizedNumber(normalizedNumber: String)
}
