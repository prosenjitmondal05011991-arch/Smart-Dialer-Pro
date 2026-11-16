package com.example.smartdialer.viewmodel

import android.Manifest
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartdialer.data.local.FavoriteContactEntity
import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.data.model.ContactDraft
import com.example.smartdialer.data.model.UiState
import com.example.smartdialer.data.repository.ContactsRepository
import com.example.smartdialer.permissions.PermissionHelper
import com.example.smartdialer.util.ContactExportFormat
import com.example.smartdialer.util.ContactsExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ContactActionEvent {
    data object Saved : ContactActionEvent
    data object Deleted : ContactActionEvent
    data class FavoriteUpdated(val name: String, val added: Boolean) : ContactActionEvent
    data class Error(val message: String) : ContactActionEvent
}

sealed interface ContactExportEvent {
    data class Success(val count: Int, val format: ContactExportFormat) : ContactExportEvent
    data object EmptyContacts : ContactExportEvent
    data class Error(val message: String) : ContactExportEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModel(
    application: Application,
    private val contactsRepository: ContactsRepository
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _rawContacts = MutableStateFlow<List<Contact>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _loadError = MutableStateFlow<String?>(null)
    private val _hasReadPermission = MutableStateFlow(
        PermissionHelper.hasContactsReadPermission(application)
    )
    private val _permanentlyDenied = MutableStateFlow(false)

    private val localFavoritesFlow: StateFlow<List<FavoriteContactEntity>> =
        contactsRepository.observeLocalFavorites()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMergedContacts: StateFlow<List<Contact>> = combine(
        _rawContacts,
        localFavoritesFlow
    ) { contacts, localFavs ->
        val localKeys = localFavs.map { it.lookupKey }.toSet()
        contacts.map { contact ->
            contact.copy(isLocalFavorite = contact.lookupKey in localKeys)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contactsState: StateFlow<UiState<List<Contact>>> = combine(
        _hasReadPermission,
        _permanentlyDenied,
        _isLoading,
        _loadError,
        allMergedContacts,
        _searchQuery
    ) { values ->
        val hasPerm = values[0] as Boolean
        val permDenied = values[1] as Boolean
        val loading = values[2] as Boolean
        val error = values[3] as String?
        @Suppress("UNCHECKED_CAST")
        val contacts = values[4] as List<Contact>
        val query = (values[5] as String).trim()

        when {
            !hasPerm -> UiState.PermissionRequired(
                permission = Manifest.permission.READ_CONTACTS,
                permanentlyDenied = permDenied
            )
            loading && contacts.isEmpty() -> UiState.Loading
            error != null && contacts.isEmpty() -> UiState.Error(error)
            else -> {
                val filtered = if (query.isEmpty()) {
                    contacts
                } else {
                    val digitsQuery = query.filter { it.isDigit() }
                    contacts.filter { contact ->
                        contact.displayName.contains(query, ignoreCase = true) ||
                            contact.phoneNumbers.any { entry ->
                                entry.number.contains(query, ignoreCase = true) ||
                                    (digitsQuery.isNotEmpty() &&
                                        entry.normalizedNumber.contains(digitsQuery))
                            }
                    }
                }
                if (filtered.isEmpty()) {
                    UiState.Empty(filteredEmpty = query.isNotEmpty() && contacts.isNotEmpty())
                } else {
                    UiState.Success(filtered)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val favoritesState: StateFlow<UiState<List<Contact>>> = combine(
        _hasReadPermission,
        _permanentlyDenied,
        _isLoading,
        allMergedContacts,
        localFavoritesFlow
    ) { hasPerm, permDenied, loading, mergedContacts, localFavs ->
        when {
            !hasPerm && localFavs.isEmpty() -> UiState.PermissionRequired(
                permission = Manifest.permission.READ_CONTACTS,
                permanentlyDenied = permDenied
            )
            loading && mergedContacts.isEmpty() && localFavs.isEmpty() -> UiState.Loading
            else -> {
                val deviceFavorites = mergedContacts.filter { it.isFavorite }
                val deviceLookupKeys = deviceFavorites.map { it.lookupKey }.toSet()
                val extraLocal = localFavs
                    .filter { it.lookupKey !in deviceLookupKeys }
                    .map { entity ->
                        Contact(
                            id = entity.contactId,
                            lookupKey = entity.lookupKey,
                            displayName = entity.displayName,
                            primaryNumber = entity.phoneNumber,
                            photoUri = entity.photoUri,
                            isLocalFavorite = true
                        )
                    }
                val combined = (deviceFavorites + extraLocal)
                    .sortedBy { it.displayName.lowercase() }
                if (combined.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(combined)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _selectedContactId = MutableStateFlow<Long?>(null)
    private val _selectedDetailState = MutableStateFlow<UiState<Contact>>(UiState.Loading)
    val selectedDetailState: StateFlow<UiState<Contact>> = _selectedDetailState.asStateFlow()

    val selectedContactNote: StateFlow<String> = _selectedDetailState
        .flatMapLatest { state ->
            val lookupKey = (state as? UiState.Success)?.data?.lookupKey
            if (lookupKey != null) {
                contactsRepository.observeContactNote(lookupKey)
            } else {
                flowOf("")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _events = MutableSharedFlow<ContactActionEvent>()
    val events: SharedFlow<ContactActionEvent> = _events.asSharedFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportEvents = MutableSharedFlow<ContactExportEvent>()
    val exportEvents: SharedFlow<ContactExportEvent> = _exportEvents.asSharedFlow()

    init {
        refreshContacts()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onReadPermissionResult(granted: Boolean, permanentlyDenied: Boolean = false) {
        _hasReadPermission.value = granted
        _permanentlyDenied.value = permanentlyDenied
        if (granted) {
            refreshContacts()
        }
    }

    fun refreshContacts() {
        val hasPerm = PermissionHelper.hasContactsReadPermission(getApplication())
        _hasReadPermission.value = hasPerm
        if (!hasPerm) return

        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            contactsRepository.loadDeviceContacts()
                .onSuccess { list ->
                    _rawContacts.value = list
                }
                .onFailure { err ->
                    _loadError.value = err.message ?: "Unable to read contacts"
                }
            _isLoading.value = false
        }
    }

    fun loadContactDetail(contactId: Long) {
        _selectedContactId.value = contactId
        viewModelScope.launch {
            _selectedDetailState.value = UiState.Loading
            val detail = contactsRepository.loadContactDetail(contactId)
            if (detail != null) {
                _selectedDetailState.value = UiState.Success(detail)
            } else {
                val fallback = allMergedContacts.value.firstOrNull { it.id == contactId }
                _selectedDetailState.value = if (fallback != null) {
                    UiState.Success(fallback)
                } else {
                    UiState.Error("Contact not found")
                }
            }
        }
    }

    fun toggleFavorite(contact: Contact) {
        val nextFav = !contact.isFavorite
        viewModelScope.launch {
            contactsRepository.setFavorite(contact, nextFav)
                .onSuccess {
                    refreshContacts()
                    if (_selectedContactId.value == contact.id) {
                        loadContactDetail(contact.id)
                    }
                    _events.emit(ContactActionEvent.FavoriteUpdated(contact.displayName, nextFav))
                }
                .onFailure { err ->
                    _events.emit(ContactActionEvent.Error(err.message ?: "Failed to update favorite"))
                }
        }
    }

    fun saveContact(draft: ContactDraft) {
        viewModelScope.launch {
            val result = if (draft.contactId == null) {
                contactsRepository.insertContact(draft)
            } else {
                contactsRepository.updateContact(draft)
            }
            result
                .onSuccess {
                    refreshContacts()
                    draft.contactId?.let { loadContactDetail(it) }
                    _events.emit(ContactActionEvent.Saved)
                }
                .onFailure { err ->
                    _events.emit(ContactActionEvent.Error(err.message ?: "Could not save contact"))
                }
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            contactsRepository.deleteContact(contact)
                .onSuccess {
                    refreshContacts()
                    _events.emit(ContactActionEvent.Deleted)
                }
                .onFailure { err ->
                    _events.emit(ContactActionEvent.Error(err.message ?: "Could not delete contact"))
                }
        }
    }

    fun saveNote(lookupKey: String, note: String) {
        viewModelScope.launch {
            contactsRepository.saveContactNote(lookupKey, note)
        }
    }

    fun exportContactsToUri(uri: Uri, format: ContactExportFormat) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val contactsResult = contactsRepository.loadContactsForExport()
                val contacts = contactsResult.getOrElse { err ->
                    _exportEvents.emit(ContactExportEvent.Error(err.message ?: "Unable to read contacts"))
                    return@launch
                }
                if (contacts.isEmpty()) {
                    _exportEvents.emit(ContactExportEvent.EmptyContacts)
                    return@launch
                }

                val payload = withContext(Dispatchers.Default) {
                    ContactsExporter.formatContacts(contacts, format)
                }
                val writeResult = withContext(Dispatchers.IO) {
                    ContactsExporter.writeExportToUri(
                        contentResolver = getApplication<Application>().contentResolver,
                        uri = uri,
                        content = payload
                    )
                }
                writeResult
                    .onSuccess {
                        _exportEvents.emit(ContactExportEvent.Success(contacts.size, format))
                    }
                    .onFailure { err ->
                        _exportEvents.emit(ContactExportEvent.Error(err.message ?: "Export failed"))
                    }
            } finally {
                _isExporting.value = false
            }
        }
    }
}
