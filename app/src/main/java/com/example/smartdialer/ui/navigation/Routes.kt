package com.example.smartdialer.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object FavoritesRoute

@Serializable
data object RecentsRoute

@Serializable
data object ContactsRoute

@Serializable
data object KeypadRoute

@Serializable
data class ContactDetailsRoute(val contactId: Long)

@Serializable
data object SettingsRoute

@Serializable
data object PrivacyInfoRoute

@Serializable
data object DefaultDialerStatusRoute

@Serializable
data object BlockedNumbersRoute

@Serializable
data object AppPermissionsRoute

@Serializable
data object CloudSyncRoute
