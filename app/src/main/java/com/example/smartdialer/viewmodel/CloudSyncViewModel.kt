package com.example.smartdialer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartdialer.data.firebase.CloudSyncRepository
import com.example.smartdialer.data.firebase.SyncedBlockedNumber
import com.example.smartdialer.data.firebase.SyncedContactNote
import com.example.smartdialer.data.firebase.SyncedFavoriteContact
import com.example.smartdialer.data.firebase.UserProfile
import com.example.smartdialer.data.local.ContactNoteDao
import com.example.smartdialer.data.local.ContactNoteEntity
import com.example.smartdialer.data.local.FavoriteContactDao
import com.example.smartdialer.data.local.FavoriteContactEntity
import com.example.smartdialer.data.model.UiState
import com.example.smartdialer.data.preferences.CallRecordingScope
import com.example.smartdialer.data.preferences.PreferencesRepository
import com.example.smartdialer.data.repository.BlocklistRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CloudSyncEvent {
    data class BackupCompleted(val itemCount: Int) : CloudSyncEvent
    data class RestoreCompleted(val itemCount: Int) : CloudSyncEvent
    data class Error(val message: String) : CloudSyncEvent
}

class CloudSyncViewModel(
    private val cloudSyncRepository: CloudSyncRepository,
    private val blocklistRepository: BlocklistRepository,
    private val favoriteDao: FavoriteContactDao,
    private val contactNoteDao: ContactNoteDao,
    private val preferencesRepository: PreferencesRepository,
    private val currentUserId: String
) : ViewModel() {

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        cloudSyncRepository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load profile"))) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val blockedNumbersState: StateFlow<UiState<List<SyncedBlockedNumber>>> =
        cloudSyncRepository.observeBlockedNumbers(currentUserId)
            .map<List<SyncedBlockedNumber>, UiState<List<SyncedBlockedNumber>>> { UiState.Success(it) }
            .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to sync blocked numbers"))) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val favoritesState: StateFlow<UiState<List<SyncedFavoriteContact>>> =
        cloudSyncRepository.observeFavorites(currentUserId)
            .map<List<SyncedFavoriteContact>, UiState<List<SyncedFavoriteContact>>> { UiState.Success(it) }
            .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to sync favorites"))) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val contactNotesState: StateFlow<UiState<List<SyncedContactNote>>> =
        cloudSyncRepository.observeContactNotes(currentUserId)
            .map<List<SyncedContactNote>, UiState<List<SyncedContactNote>>> { UiState.Success(it) }
            .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to sync notes"))) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _events = MutableSharedFlow<CloudSyncEvent>()
    val events: SharedFlow<CloudSyncEvent> = _events.asSharedFlow()

    fun backupLocalDataToCloud(displayName: String, email: String?) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val prefs = preferencesRepository.preferencesFlow.first()
                cloudSyncRepository.upsertUserProfile(
                    displayName = displayName,
                    email = email,
                    autoCallRecordingEnabled = prefs.autoCallRecordingEnabled,
                    callRecordingScope = prefs.callRecordingScope
                ).getOrThrow()

                val blocked = blocklistRepository.blockedNumbersFlow.first()
                val favorites = favoriteDao.observeFavorites().first()

                var count = 1
                blocked.forEach { entry ->
                    cloudSyncRepository.upsertBlockedNumber(entry).getOrThrow()
                    count++
                }
                favorites.forEach { fav ->
                    cloudSyncRepository.upsertFavoriteContact(fav).getOrThrow()
                    count++
                }
                _events.emit(CloudSyncEvent.BackupCompleted(count))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(CloudSyncEvent.Error(e.localizedMessage ?: "Cloud backup failed"))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun restoreCloudDataToLocal() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                var restoredCount = 0

                val profile = (userProfileState.value as? UiState.Success)?.data
                if (profile != null) {
                    preferencesRepository.setAutoCallRecordingEnabled(profile.autoCallRecordingEnabled)
                    val scope = runCatching {
                        CallRecordingScope.valueOf(profile.callRecordingScope)
                    }.getOrDefault(CallRecordingScope.ALL_CALLS)
                    preferencesRepository.setCallRecordingScope(scope)
                    restoredCount++
                }

                val blocked = (blockedNumbersState.value as? UiState.Success)?.data.orEmpty()
                blocked.forEach { item ->
                    blocklistRepository.addBlockedNumber(item.normalizedNumber, item.displayLabel)
                    restoredCount++
                }

                val favs = (favoritesState.value as? UiState.Success)?.data.orEmpty()
                favs.forEach { item ->
                    favoriteDao.upsertFavorite(
                        FavoriteContactEntity(
                            lookupKey = item.lookupKey,
                            contactId = 0L,
                            displayName = item.displayName,
                            phoneNumber = item.phoneNumber,
                            speedDialSlot = item.speedDialSlot?.toInt()
                        )
                    )
                    restoredCount++
                }

                val notes = (contactNotesState.value as? UiState.Success)?.data.orEmpty()
                notes.forEach { note ->
                    contactNoteDao.upsertNote(
                        ContactNoteEntity(
                            lookupKey = note.lookupKey,
                            noteText = note.noteText
                        )
                    )
                    restoredCount++
                }

                _events.emit(CloudSyncEvent.RestoreCompleted(restoredCount))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(CloudSyncEvent.Error(e.localizedMessage ?: "Cloud restore failed"))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteCloudBlockedNumber(blockId: String) {
        viewModelScope.launch {
            cloudSyncRepository.deleteBlockedNumber(blockId).onFailure { e ->
                _events.emit(CloudSyncEvent.Error(e.localizedMessage ?: "Failed to remove item"))
            }
        }
    }

    fun deleteCloudFavorite(favoriteId: String) {
        viewModelScope.launch {
            cloudSyncRepository.deleteFavoriteContact(favoriteId).onFailure { e ->
                _events.emit(CloudSyncEvent.Error(e.localizedMessage ?: "Failed to remove favorite"))
            }
        }
    }

    fun deleteCloudNote(noteId: String) {
        viewModelScope.launch {
            cloudSyncRepository.deleteContactNote(noteId).onFailure { e ->
                _events.emit(CloudSyncEvent.Error(e.localizedMessage ?: "Failed to remove note"))
            }
        }
    }

    private fun unwrapFirestoreMessage(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return (firestoreError ?: error).localizedMessage ?: fallback
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
