package com.example.smartdialer.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smartdialer.AppContainer
import kotlinx.coroutines.flow.StateFlow
import com.example.smartdialer.data.model.Contact

class DialerViewModelFactory(
    private val application: Application,
    private val container: AppContainer,
    private val contactsFlowProvider: (() -> StateFlow<List<Contact>>)? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(application, container.preferencesRepository) as T
            }
            modelClass.isAssignableFrom(ContactsViewModel::class.java) -> {
                ContactsViewModel(application, container.contactsRepository) as T
            }
            modelClass.isAssignableFrom(RecentsViewModel::class.java) -> {
                RecentsViewModel(application, container.callLogRepository) as T
            }
            modelClass.isAssignableFrom(KeypadViewModel::class.java) -> {
                val contactsFlow = contactsFlowProvider?.invoke()
                    ?: throw IllegalStateException("contactsFlowProvider is required for KeypadViewModel")
                KeypadViewModel(application, contactsFlow) as T
            }
            modelClass.isAssignableFrom(BlocklistViewModel::class.java) -> {
                BlocklistViewModel(application, container.blocklistRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
