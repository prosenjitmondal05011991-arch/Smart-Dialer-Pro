package com.example.smartdialer

import android.app.Application
import com.example.smartdialer.data.local.SmartDialerDatabase
import com.example.smartdialer.data.preferences.PreferencesRepository
import com.example.smartdialer.data.repository.CallLogRepository
import com.example.smartdialer.data.repository.ContactsRepository

class AppContainer(application: Application) {
    val database: SmartDialerDatabase by lazy {
        SmartDialerDatabase.getInstance(application)
    }
    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(application)
    }
    val contactsRepository: ContactsRepository by lazy {
        ContactsRepository(
            context = application,
            favoriteDao = database.favoriteContactDao(),
            noteDao = database.contactNoteDao()
        )
    }
    val callLogRepository: CallLogRepository by lazy {
        CallLogRepository(application)
    }
    val blocklistRepository: com.example.smartdialer.data.repository.BlocklistRepository by lazy {
        com.example.smartdialer.data.repository.BlocklistRepository(database.blockedNumberDao())
    }
}

class SmartDialerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
