package com.example.smartdialer.viewmodel

import android.Manifest
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartdialer.data.model.CallDirection
import com.example.smartdialer.data.model.CallFilter
import com.example.smartdialer.data.model.CallLogEntry
import com.example.smartdialer.data.model.UiState
import com.example.smartdialer.data.repository.CallLogRepository
import com.example.smartdialer.permissions.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecentsViewModel(
    application: Application,
    private val callLogRepository: CallLogRepository
) : AndroidViewModel(application) {

    private val _activeFilter = MutableStateFlow(CallFilter.ALL)
    val activeFilter: StateFlow<CallFilter> = _activeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _rawEntries = MutableStateFlow<List<CallLogEntry>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _loadError = MutableStateFlow<String?>(null)
    private val _hasPermission = MutableStateFlow(
        PermissionHelper.hasCallLogPermission(application)
    )
    private val _permanentlyDenied = MutableStateFlow(false)

    val callLogState: StateFlow<UiState<List<CallLogEntry>>> = combine(
        _hasPermission,
        _permanentlyDenied,
        _isLoading,
        _loadError,
        _rawEntries,
        _activeFilter,
        _searchQuery
    ) { values ->
        val hasPerm = values[0] as Boolean
        val permDenied = values[1] as Boolean
        val loading = values[2] as Boolean
        val error = values[3] as String?
        @Suppress("UNCHECKED_CAST")
        val entries = values[4] as List<CallLogEntry>
        val filter = values[5] as CallFilter
        val query = (values[6] as String).trim()

        when {
            !hasPerm -> UiState.PermissionRequired(
                permission = Manifest.permission.READ_CALL_LOG,
                permanentlyDenied = permDenied
            )
            loading && entries.isEmpty() -> UiState.Loading
            error != null && entries.isEmpty() -> UiState.Error(error)
            entries.isEmpty() -> UiState.Empty(filteredEmpty = false)
            else -> {
                val filtered = entries.filter { entry ->
                    val matchesFilter = when (filter) {
                        CallFilter.ALL -> true
                        CallFilter.MISSED -> entry.direction == CallDirection.MISSED
                        CallFilter.OUTGOING -> entry.direction == CallDirection.OUTGOING
                        CallFilter.INCOMING -> entry.direction == CallDirection.INCOMING
                    }
                    val matchesQuery = query.isEmpty() ||
                        (entry.cachedName?.contains(query, ignoreCase = true) == true) ||
                        entry.number.contains(query, ignoreCase = true) ||
                        entry.formattedNumber.contains(query, ignoreCase = true)
                    matchesFilter && matchesQuery
                }
                if (filtered.isEmpty()) {
                    UiState.Empty(filteredEmpty = true)
                } else {
                    UiState.Success(filtered)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        refreshCallLog()
    }

    fun setFilter(filter: CallFilter) {
        _activeFilter.value = filter
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean = false) {
        _hasPermission.value = granted
        _permanentlyDenied.value = permanentlyDenied
        if (granted) {
            refreshCallLog()
        }
    }

    fun refreshCallLog() {
        val hasPerm = PermissionHelper.hasCallLogPermission(getApplication())
        _hasPermission.value = hasPerm
        if (!hasPerm) return

        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            callLogRepository.loadDeviceCallLog()
                .onSuccess { list ->
                    _rawEntries.value = list
                }
                .onFailure { err ->
                    _loadError.value = err.message ?: "Unable to read call log"
                }
            _isLoading.value = false
        }
    }
}
