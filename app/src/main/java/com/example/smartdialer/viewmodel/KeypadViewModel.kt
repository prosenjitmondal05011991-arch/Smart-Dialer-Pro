package com.example.smartdialer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.data.model.SimAccount
import com.example.smartdialer.data.model.T9Match
import com.example.smartdialer.telecom.DialerTelecomManager
import com.example.smartdialer.util.T9Matcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class KeypadViewModel(
    application: Application,
    contactsFlow: StateFlow<List<Contact>>
) : AndroidViewModel(application) {

    private val _dialedDigits = MutableStateFlow("")
    val dialedDigits: StateFlow<String> = _dialedDigits.asStateFlow()

    val formattedDigits: StateFlow<String> = _dialedDigits
        .map { T9Matcher.formatDialDisplay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val t9Matches: StateFlow<List<T9Match>> = combine(
        _dialedDigits,
        contactsFlow
    ) { digits, contacts ->
        T9Matcher.matchContacts(digits, contacts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _availableSims = MutableStateFlow<List<SimAccount>>(emptyList())
    val availableSims: StateFlow<List<SimAccount>> = _availableSims.asStateFlow()

    private val _selectedSimSlot = MutableStateFlow(1)
    val selectedSimSlot: StateFlow<Int> = _selectedSimSlot.asStateFlow()

    init {
        refreshSims()
    }

    fun appendDigit(ch: Char) {
        if (_dialedDigits.value.length < 24) {
            _dialedDigits.value += ch
        }
    }

    fun backspace() {
        val current = _dialedDigits.value
        if (current.isNotEmpty()) {
            _dialedDigits.value = current.dropLast(1)
        }
    }

    fun clearAll() {
        _dialedDigits.value = ""
    }

    fun prefillNumber(number: String) {
        _dialedDigits.value = T9Matcher.normalizeDigits(number)
    }

    fun selectSim(slotIndex: Int) {
        _selectedSimSlot.value = slotIndex
    }

    fun refreshSims() {
        val sims = DialerTelecomManager.getAvailableSims(getApplication())
        _availableSims.value = sims
        if (sims.isNotEmpty() && sims.none { it.slotIndex == _selectedSimSlot.value }) {
            _selectedSimSlot.value = sims.first().slotIndex
        }
    }
}
