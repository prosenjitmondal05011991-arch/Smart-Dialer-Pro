package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.example.smartdialer.data.firebase.CloudSyncRepository
import com.example.smartdialer.data.local.BlockedNumberEntity
import com.example.smartdialer.data.local.ContactNoteEntity
import com.example.smartdialer.data.local.FavoriteContactEntity
import com.example.smartdialer.data.preferences.CallRecordingScope
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun `upsertUserProfile creates and updates owner profile`() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertUserProfile(
                displayName = "Alice Dialer",
                email = ALICE_EMAIL,
                autoCallRecordingEnabled = false,
                callRecordingScope = CallRecordingScope.ALL_CALLS
            )
        }
        assertTrue(createResult.isSuccess)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertUserProfile(
                displayName = "Alice Updated",
                email = ALICE_EMAIL,
                autoCallRecordingEnabled = true,
                callRecordingScope = CallRecordingScope.UNKNOWN_CONTACTS_ONLY
            )
        }
        assertTrue(updateResult.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it != null && it.displayName == "Alice Updated" }
        }
        assertNotNull(profile)
        assertEquals(true, profile?.autoCallRecordingEnabled)
        assertEquals("UNKNOWN_CONTACTS_ONLY", profile?.callRecordingScope)
    }

    @Test
    fun `upsertAndObserveBlockedNumber emits realtime updates for owner`() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore, auth)
        val suffix = UUID.randomUUID().toString().take(6)
        val number = "+8801712$suffix"

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertBlockedNumber(
                BlockedNumberEntity(
                    normalizedNumber = number,
                    displayLabel = "Spam $suffix"
                )
            )
        }
        assertTrue(result.isSuccess)
        val blockId = result.getOrThrow()

        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBlockedNumbers(uid).first { items -> items.any { it.id == blockId } }
        }
        assertEquals(number, list.first { it.id == blockId }.normalizedNumber)
    }

    @Test
    fun `upsertAndObserveFavoritesAndNotes emit realtime updates for owner`() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore, auth)
        val uniqueKey = "lookup_${UUID.randomUUID()}"

        val favResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertFavoriteContact(
                FavoriteContactEntity(
                    lookupKey = uniqueKey,
                    contactId = 42L,
                    displayName = "Sadia Akter",
                    phoneNumber = "+8801712345678",
                    speedDialSlot = 2
                )
            )
        }
        assertTrue(favResult.isSuccess)

        val noteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertContactNote(
                ContactNoteEntity(
                    lookupKey = uniqueKey,
                    noteText = "Call in the evening"
                )
            )
        }
        assertTrue(noteResult.isSuccess)

        val favs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFavorites(uid).first { items -> items.any { it.lookupKey == uniqueKey } }
        }
        assertEquals("Sadia Akter", favs.first { it.lookupKey == uniqueKey }.displayName)

        val notes = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeContactNotes(uid).first { items -> items.any { it.lookupKey == uniqueKey } }
        }
        assertEquals("Call in the evening", notes.first { it.lookupKey == uniqueKey }.noteText)
    }

    @Test
    fun `observeBlockedNumbers crossUserAccess fails with PERMISSION_DENIED`() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudSyncRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertBlockedNumber(
                BlockedNumberEntity(normalizedNumber = "+8801799999999", displayLabel = "Private")
            )
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudSyncRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeBlockedNumbers(aliceUid).first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        val firestoreError = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException for cross-user read, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreError.code)
    }

    @Test
    fun `observeBlockedNumbers unauthenticated fails with PERMISSION_DENIED`() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repo = CloudSyncRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeBlockedNumbers(aliceUid).first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        val firestoreError = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException when unauthenticated, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreError.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice.dialer@test.com"
        const val BOB_EMAIL = "bob.dialer@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
