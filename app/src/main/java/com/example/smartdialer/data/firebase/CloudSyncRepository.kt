package com.example.smartdialer.data.firebase

import android.content.Context
import com.example.R
import com.example.smartdialer.data.local.BlockedNumberEntity
import com.example.smartdialer.data.local.ContactNoteEntity
import com.example.smartdialer.data.local.FavoriteContactEntity
import com.example.smartdialer.data.preferences.CallRecordingScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = FirebaseAuth.getInstance()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, docRef.path, userId)
                    throw error
                }
        )
    }

    fun observeBlockedNumbers(userId: String): Flow<List<SyncedBlockedNumber>> = flow {
        val colRef = db.collection("users").document(userId).collection("blocked_numbers")
        emitAll(
            colRef.snapshots()
                .map { querySnapshot ->
                    querySnapshot.toObjects(
                        SyncedBlockedNumber::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observeFavorites(userId: String): Flow<List<SyncedFavoriteContact>> = flow {
        val colRef = db.collection("users").document(userId).collection("favorite_contacts")
        emitAll(
            colRef.snapshots()
                .map { querySnapshot ->
                    querySnapshot.toObjects(
                        SyncedFavoriteContact::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observeContactNotes(userId: String): Flow<List<SyncedContactNote>> = flow {
        val colRef = db.collection("users").document(userId).collection("contact_notes")
        emitAll(
            colRef.snapshots()
                .map { querySnapshot ->
                    querySnapshot.toObjects(
                        SyncedContactNote::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun upsertUserProfile(
        displayName: String,
        email: String?,
        autoCallRecordingEnabled: Boolean,
        callRecordingScope: CallRecordingScope
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val safeName = displayName.trim().take(120)
        val safeEmail = email?.trim()?.takeIf { it.isNotEmpty() }?.take(254)
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", safeName)
                    if (safeEmail != null) put("email", safeEmail)
                    put("autoCallRecordingEnabled", autoCallRecordingEnabled)
                    put("callRecordingScope", callRecordingScope.name)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                val updates = buildMap<String, Any?> {
                    put("displayName", safeName)
                    put("email", safeEmail)
                    put("autoCallRecordingEnabled", autoCallRecordingEnabled)
                    put("callRecordingScope", callRecordingScope.name)
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = OperationType.WRITE
            handleFirestoreError(e, op, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun upsertBlockedNumber(entity: BlockedNumberEntity): Result<String> {
        val uid = requireUserId()
        val docId = safeDocId("blk", entity.normalizedNumber)
        val docRef = db.collection("users").document(uid)
            .collection("blocked_numbers").document(docId)
        val number = entity.normalizedNumber.trim().take(32)
        val label = entity.displayLabel?.trim()?.takeIf { it.isNotEmpty() }?.take(120)
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("normalizedNumber", number)
                    if (label != null) put("displayLabel", label)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                val updates = buildMap<String, Any?> {
                    put("normalizedNumber", number)
                    put("displayLabel", label)
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null }
                docRef.update(updates).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteBlockedNumber(blockId: String): Result<Unit> {
        val uid = requireUserId()
        require(blockId.isNotBlank()) { "Block ID must not be blank" }
        val docRef = db.collection("users").document(uid)
            .collection("blocked_numbers").document(blockId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun upsertFavoriteContact(entity: FavoriteContactEntity): Result<String> {
        val uid = requireUserId()
        val docId = safeDocId("fav", entity.lookupKey)
        val docRef = db.collection("users").document(uid)
            .collection("favorite_contacts").document(docId)
        val lookupKey = entity.lookupKey.trim().take(256)
        val name = entity.displayName.trim().ifEmpty { entity.phoneNumber }.take(120)
        val phone = entity.phoneNumber.trim().take(32)
        val slot = entity.speedDialSlot?.takeIf { it in 1..9 }?.toLong()
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("lookupKey", lookupKey)
                    put("displayName", name)
                    put("phoneNumber", phone)
                    if (slot != null) put("speedDialSlot", slot)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                val updates = buildMap<String, Any?> {
                    put("lookupKey", lookupKey)
                    put("displayName", name)
                    put("phoneNumber", phone)
                    put("speedDialSlot", slot)
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null }
                docRef.update(updates).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteFavoriteContact(favoriteId: String): Result<Unit> {
        val uid = requireUserId()
        require(favoriteId.isNotBlank()) { "Favorite ID must not be blank" }
        val docRef = db.collection("users").document(uid)
            .collection("favorite_contacts").document(favoriteId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun upsertContactNote(entity: ContactNoteEntity): Result<String> {
        val uid = requireUserId()
        val text = entity.noteText.trim().take(2000)
        require(text.isNotEmpty()) { "Note text must not be blank" }
        val docId = safeDocId("note", entity.lookupKey)
        val docRef = db.collection("users").document(uid)
            .collection("contact_notes").document(docId)
        val lookupKey = entity.lookupKey.trim().take(256)
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "lookupKey" to lookupKey,
                    "noteText" to text,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "lookupKey" to lookupKey,
                    "noteText" to text,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteContactNote(noteId: String): Result<Unit> {
        val uid = requireUserId()
        require(noteId.isNotBlank()) { "Note ID must not be blank" }
        val docRef = db.collection("users").document(uid)
            .collection("contact_notes").document(noteId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path, auth)
        }
    }

    companion object {
        fun safeDocId(prefix: String, key: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(key.trim().toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString("") { "%02x".format(it) }.take(28)
            return "${prefix}_$hex"
        }
    }
}
