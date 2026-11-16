package com.example.smartdialer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartdialer.data.local.BlockedNumberEntity
import com.example.smartdialer.data.repository.BlocklistRepository
import com.example.smartdialer.telecom.DialerTelecomManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BlocklistEvent {
    data class Blocked(val number: String) : BlocklistEvent
    data class Unblocked(val number: String) : BlocklistEvent
    data class Error(val message: String) : BlocklistEvent
}

class BlocklistViewModel(
    application: Application,
    private val blocklistRepository: BlocklistRepository
) : AndroidViewModel(application) {

    val blockedNumbers: StateFlow<List<BlockedNumberEntity>> = blocklistRepository.blockedNumbersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isCallScreeningActive = MutableStateFlow(
        DialerTelecomManager.isCallScreeningRoleHeld(application)
    )
    val isCallScreeningActive: StateFlow<Boolean> = _isCallScreeningActive.asStateFlow()

    private val _events = MutableSharedFlow<BlocklistEvent>()
    val events: SharedFlow<BlocklistEvent> = _events.asSharedFlow()

    fun refreshScreeningRoleStatus() {
        _isCallScreeningActive.value = DialerTelecomManager.isCallScreeningRoleHeld(getApplication())
    }

    fun blockNumber(rawNumber: String, label: String? = null) {
        viewModelScope.launch {
            blocklistRepository.addBlockedNumber(rawNumber, label)
                .onSuccess {
                    _events.emit(BlocklistEvent.Blocked(rawNumber.trim()))
                }
                .onFailure { err ->
                    _events.emit(BlocklistEvent.Error(err.message ?: "Invalid phone number"))
                }
        }
    }

    fun unblockById(entity: BlockedNumberEntity) {
        viewModelScope.launch {
            blocklistRepository.unblockById(entity.id)
            _events.emit(BlocklistEvent.Unblocked(entity.normalizedNumber))
        }
    }

    fun unblockNumber(rawNumber: String) {
        viewModelScope.launch {
            blocklistRepository.unblockNumber(rawNumber)
            _events.emit(BlocklistEvent.Unblocked(rawNumber.trim()))
        }
    }
}
