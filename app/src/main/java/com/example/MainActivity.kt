package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.smartdialer.SmartDialerApp
import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.data.model.ContactDraft
import com.example.smartdialer.data.model.SimAccount
import com.example.smartdialer.data.firebase.GoogleAuthManager.authStateFlow
import com.example.smartdialer.localization.LocaleHelper
import com.example.smartdialer.permissions.PermissionHelper
import com.example.smartdialer.telecom.DialerTelecomManager
import com.example.smartdialer.ui.components.PermissionExplainDialog
import com.example.smartdialer.ui.navigation.AppPermissionsRoute
import com.example.smartdialer.ui.navigation.BlockedNumbersRoute
import com.example.smartdialer.ui.navigation.CloudSyncRoute
import com.example.smartdialer.ui.navigation.ContactDetailsRoute
import com.example.smartdialer.ui.navigation.ContactsRoute
import com.example.smartdialer.ui.navigation.DefaultDialerStatusRoute
import com.example.smartdialer.ui.navigation.FavoritesRoute
import com.example.smartdialer.ui.navigation.KeypadRoute
import com.example.smartdialer.ui.navigation.PrivacyInfoRoute
import com.example.smartdialer.ui.navigation.RecentsRoute
import com.example.smartdialer.ui.navigation.SettingsRoute
import com.example.smartdialer.ui.screens.AppPermissionsScreen
import com.example.smartdialer.ui.screens.BlockedNumbersScreen
import com.example.smartdialer.ui.screens.CloudSyncScreen
import com.example.smartdialer.ui.screens.ContactDetailsScreen
import com.example.smartdialer.ui.screens.ContactEditorDialog
import com.example.smartdialer.ui.screens.ContactsScreen
import com.example.smartdialer.ui.screens.DefaultDialerStatusScreen
import com.example.smartdialer.ui.screens.FavoritesScreen
import com.example.smartdialer.ui.screens.KeypadScreen
import com.example.smartdialer.ui.screens.PrivacyInfoScreen
import com.example.smartdialer.ui.screens.RecentsScreen
import com.example.smartdialer.ui.screens.SettingsScreen
import com.example.smartdialer.ui.theme.SmartDialerTheme
import com.example.smartdialer.util.BlocklistMatcher
import com.example.smartdialer.util.ContactExportFormat
import com.example.smartdialer.util.ContactsExporter
import com.example.smartdialer.viewmodel.BlocklistEvent
import com.example.smartdialer.viewmodel.BlocklistViewModel
import com.example.smartdialer.viewmodel.ContactActionEvent
import com.example.smartdialer.viewmodel.ContactExportEvent
import com.example.smartdialer.viewmodel.ContactsViewModel
import com.example.smartdialer.viewmodel.DialerViewModelFactory
import com.example.smartdialer.viewmodel.KeypadViewModel
import com.example.smartdialer.viewmodel.RecentsViewModel
import com.example.smartdialer.viewmodel.SettingsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var incomingDialNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingDialNumber = extractDialNumber(intent)

        val app = application as SmartDialerApp
        val baseFactory = DialerViewModelFactory(app, app.container)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = baseFactory)
            val contactsViewModel: ContactsViewModel = viewModel(factory = baseFactory)
            val recentsViewModel: RecentsViewModel = viewModel(factory = baseFactory)
            val blocklistViewModel: BlocklistViewModel = viewModel(factory = baseFactory)
            val keypadFactory = remember(contactsViewModel) {
                DialerViewModelFactory(app, app.container) {
                    contactsViewModel.allMergedContacts
                }
            }
            val keypadViewModel: KeypadViewModel = viewModel(factory = keypadFactory)

            val preferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
            val localizedContext = remember(this, preferences.language) {
                LocaleHelper.createLocalizedContext(this, preferences.language)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration
            ) {
                SmartDialerTheme(
                    themeMode = preferences.themeMode,
                    accentColor = preferences.accentColor,
                    fontSizeScale = preferences.fontSizeScale
                ) {
                    SmartDialerRoot(
                        activity = this@MainActivity,
                        incomingDialNumber = incomingDialNumber,
                        onConsumeIncomingDial = { incomingDialNumber = null },
                        settingsViewModel = settingsViewModel,
                        contactsViewModel = contactsViewModel,
                        recentsViewModel = recentsViewModel,
                        keypadViewModel = keypadViewModel,
                        blocklistViewModel = blocklistViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingDialNumber = extractDialNumber(intent)
    }

    private fun extractDialNumber(intent: Intent?): String? {
        if (intent == null) return null
        val action = intent.action
        val data: Uri? = intent.data
        if ((action == Intent.ACTION_DIAL || action == Intent.ACTION_VIEW) && data?.scheme == "tel") {
            return data.schemeSpecificPart?.takeIf { it.isNotBlank() }
        }
        return null
    }
}

private data class TopLevelDestination(
    val id: String,
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
private fun SmartDialerRoot(
    activity: ComponentActivity,
    incomingDialNumber: String?,
    onConsumeIncomingDial: () -> Unit,
    settingsViewModel: SettingsViewModel,
    contactsViewModel: ContactsViewModel,
    recentsViewModel: RecentsViewModel,
    keypadViewModel: KeypadViewModel,
    blocklistViewModel: BlocklistViewModel
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val preferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
    val defaultDialerStatus by settingsViewModel.defaultDialerStatus.collectAsStateWithLifecycle()
    val blockedNumbers by blocklistViewModel.blockedNumbers.collectAsStateWithLifecycle()
    val isCallScreeningActive by blocklistViewModel.isCallScreeningActive.collectAsStateWithLifecycle()

    val contactsState by contactsViewModel.contactsState.collectAsStateWithLifecycle()
    val favoritesState by contactsViewModel.favoritesState.collectAsStateWithLifecycle()
    val allContacts by contactsViewModel.allMergedContacts.collectAsStateWithLifecycle()
    val contactsSearch by contactsViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedDetailState by contactsViewModel.selectedDetailState.collectAsStateWithLifecycle()
    val selectedContactNote by contactsViewModel.selectedContactNote.collectAsStateWithLifecycle()
    val isExportingContacts by contactsViewModel.isExporting.collectAsStateWithLifecycle()

    val callLogState by recentsViewModel.callLogState.collectAsStateWithLifecycle()
    val callFilter by recentsViewModel.activeFilter.collectAsStateWithLifecycle()
    val recentsSearch by recentsViewModel.searchQuery.collectAsStateWithLifecycle()

    val rawDigits by keypadViewModel.dialedDigits.collectAsStateWithLifecycle()
    val formattedDigits by keypadViewModel.formattedDigits.collectAsStateWithLifecycle()
    val t9Matches by keypadViewModel.t9Matches.collectAsStateWithLifecycle()
    val availableSims by keypadViewModel.availableSims.collectAsStateWithLifecycle()
    val selectedSimSlot by keypadViewModel.selectedSimSlot.collectAsStateWithLifecycle()

    // Pending actions waiting on permission rationale / confirmation
    var pendingCallRequest by remember { mutableStateOf<Pair<String, SimAccount?>?>(null) }
    var showCallPermissionRationale by remember { mutableStateOf<Pair<String, SimAccount?>?>(null) }
    var editorDraft by remember { mutableStateOf<ContactDraft?>(null) }
    var pendingWriteDraft by remember { mutableStateOf<ContactDraft?>(null) }
    var pendingDeleteContact by remember { mutableStateOf<Contact?>(null) }
    var showWriteRationaleForDraft by remember { mutableStateOf<ContactDraft?>(null) }
    var showWriteRationaleForDelete by remember { mutableStateOf<Contact?>(null) }
    var pendingExportFormat by remember { mutableStateOf<ContactExportFormat?>(null) }
    var showExportPermissionRationale by remember { mutableStateOf<ContactExportFormat?>(null) }
    var grantedPermissionsCount by remember {
        mutableIntStateOf(PermissionHelper.grantedRuntimePermissionCount(context))
    }
    val firebaseUserFlow = remember {
        runCatching { Firebase.auth.authStateFlow() }.getOrDefault(flowOf(null))
    }
    val signedInUser by firebaseUserFlow.collectAsStateWithLifecycle(
        initialValue = runCatching { Firebase.auth.currentUser }.getOrNull()
    )

    // Permission Launchers
    val readContactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val permDenied = !granted && !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.READ_CONTACTS
        )
        contactsViewModel.onReadPermissionResult(granted, permDenied)
    }

    val readCallLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val permDenied = !granted && !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.READ_CALL_LOG
        )
        recentsViewModel.onPermissionResult(granted, permDenied)
    }

    val callPhoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val req = pendingCallRequest
        pendingCallRequest = null
        if (req != null) {
            DialerTelecomManager.placeCallOrDial(context, req.first, req.second)
                .onSuccess { direct ->
                    if (!direct && !granted) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.keypad_dial_fallback_notice, req.first)
                            )
                        }
                    }
                }
        }
    }

    val writeContactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val draft = pendingWriteDraft
        val toDelete = pendingDeleteContact
        pendingWriteDraft = null
        pendingDeleteContact = null

        if (granted) {
            if (draft != null) {
                contactsViewModel.saveContact(draft)
            } else if (toDelete != null) {
                contactsViewModel.deleteContact(toDelete)
            }
        } else if (draft != null) {
            DialerTelecomManager.openSystemInsertContact(context, draft.phoneNumber)
        }
    }

    val exportVcfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ContactExportFormat.VCF.mimeType)
    ) { uri ->
        if (uri != null) {
            contactsViewModel.exportContactsToUri(uri, ContactExportFormat.VCF)
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ContactExportFormat.CSV.mimeType)
    ) { uri ->
        if (uri != null) {
            contactsViewModel.exportContactsToUri(uri, ContactExportFormat.CSV)
        }
    }

    val launchExportDocumentPicker: (ContactExportFormat) -> Unit = { format ->
        val fileName = ContactsExporter.defaultExportFileName(format)
        when (format) {
            ContactExportFormat.VCF -> exportVcfLauncher.launch(fileName)
            ContactExportFormat.CSV -> exportCsvLauncher.launch(fileName)
        }
    }

    val exportContactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val format = pendingExportFormat
        pendingExportFormat = null
        val permDenied = !granted && !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.READ_CONTACTS
        )
        contactsViewModel.onReadPermissionResult(granted, permDenied)
        if (granted && format != null) {
            launchExportDocumentPicker(format)
        }
    }

    // Refresh permissions/sims/default dialer status on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                grantedPermissionsCount = PermissionHelper.grantedRuntimePermissionCount(context)
                settingsViewModel.refreshDefaultDialerStatus()
                blocklistViewModel.refreshScreeningRoleStatus()
                keypadViewModel.refreshSims()
                if (PermissionHelper.hasContactsReadPermission(context)) {
                    contactsViewModel.onReadPermissionResult(true, false)
                }
                if (PermissionHelper.hasCallLogPermission(context)) {
                    recentsViewModel.onPermissionResult(true, false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle incoming ACTION_DIAL tel: intent
    LaunchedEffect(incomingDialNumber) {
        if (!incomingDialNumber.isNullOrBlank()) {
            keypadViewModel.prefillNumber(incomingDialNumber)
            navController.navigate(KeypadRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onConsumeIncomingDial()
        }
    }

    // Observe contact CRUD events
    LaunchedEffect(Unit) {
        contactsViewModel.events.collect { event ->
            when (event) {
                is ContactActionEvent.Saved -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.contact_saved_success))
                }
                is ContactActionEvent.Deleted -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.contact_deleted_success))
                    navController.popBackStack()
                }
                is ContactActionEvent.FavoriteUpdated -> {
                    val msg = if (event.added) {
                        context.getString(R.string.favorites_added_msg, event.name)
                    } else {
                        context.getString(R.string.favorites_removed_msg, event.name)
                    }
                    snackbarHostState.showSnackbar(msg)
                }
                is ContactActionEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.contact_operation_failed, event.message)
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        blocklistViewModel.events.collect { event ->
            when (event) {
                is BlocklistEvent.Blocked -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.blocklist_blocked_toast, event.number)
                    )
                }
                is BlocklistEvent.Unblocked -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.blocklist_unblocked_toast, event.number)
                    )
                }
                is BlocklistEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        contactsViewModel.exportEvents.collect { event ->
            when (event) {
                is ContactExportEvent.Success -> {
                    val formatLabel = event.format.extension.uppercase()
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.settings_export_success, event.count, formatLabel)
                    )
                }
                is ContactExportEvent.EmptyContacts -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_export_empty))
                }
                is ContactExportEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.settings_export_failed, event.message)
                    )
                }
            }
        }
    }

    val initiateCall: (String, SimAccount?) -> Unit = { number, sim ->
        if (number.isBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.keypad_no_number_entered))
            }
        } else if (PermissionHelper.hasCallPhonePermission(context)) {
            DialerTelecomManager.placeCallOrDial(context, number, sim)
        } else {
            showCallPermissionRationale = number to sim
        }
    }

    val saveContactWithConsent: (ContactDraft) -> Unit = { draft ->
        if (PermissionHelper.hasContactsWritePermission(context)) {
            contactsViewModel.saveContact(draft)
        } else {
            showWriteRationaleForDraft = draft
        }
    }

    val deleteContactWithConsent: (Contact) -> Unit = { contact ->
        if (PermissionHelper.hasContactsWritePermission(context)) {
            contactsViewModel.deleteContact(contact)
        } else {
            showWriteRationaleForDelete = contact
        }
    }

    val topDestinations = remember {
        listOf(
            TopLevelDestination(
                id = "keypad",
                route = KeypadRoute,
                labelRes = R.string.nav_keypad,
                selectedIcon = Icons.Filled.Dialpad,
                unselectedIcon = Icons.Outlined.Dialpad
            ),
            TopLevelDestination(
                id = "recents",
                route = RecentsRoute,
                labelRes = R.string.nav_recents,
                selectedIcon = Icons.Filled.History,
                unselectedIcon = Icons.Outlined.History
            ),
            TopLevelDestination(
                id = "contacts",
                route = ContactsRoute,
                labelRes = R.string.nav_contacts,
                selectedIcon = Icons.Filled.Contacts,
                unselectedIcon = Icons.Outlined.Contacts
            ),
            TopLevelDestination(
                id = "favorites",
                route = FavoritesRoute,
                labelRes = R.string.nav_favorites,
                selectedIcon = Icons.Filled.Star,
                unselectedIcon = Icons.Outlined.StarOutline
            )
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isTopLevel = currentDestination?.let { dest ->
        dest.hasRoute(FavoritesRoute::class) ||
            dest.hasRoute(RecentsRoute::class) ||
            dest.hasRoute(ContactsRoute::class) ||
            dest.hasRoute(KeypadRoute::class)
    } ?: true

    val navigateToTopLevel: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                if (isTopLevel && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        topDestinations.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            val label = stringResource(item.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${item.id}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevel && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topDestinations.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            val label = stringResource(item.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${item.id}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = KeypadRoute,
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                    modifier = Modifier.weight(1f)
                ) {
                    composable<FavoritesRoute> {
                        FavoritesScreen(
                            favoritesState = favoritesState,
                            allContacts = allContacts,
                            onCallNumber = { number -> initiateCall(number, null) },
                            onOpenContactDetail = { id ->
                                contactsViewModel.loadContactDetail(id)
                                navController.navigate(ContactDetailsRoute(id))
                            },
                            onToggleFavorite = contactsViewModel::toggleFavorite,
                            onRequestContactsPermission = {
                                readContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            onOpenAppSettings = { PermissionHelper.openAppSettings(context) },
                            onRetry = contactsViewModel::refreshContacts,
                            onOpenBlockedNumbers = { navController.navigate(BlockedNumbersRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onOpenDefaultDialerStatus = {
                                navController.navigate(DefaultDialerStatusRoute)
                            }
                        )
                    }

                    composable<RecentsRoute> {
                        val favList = (favoritesState as? com.example.smartdialer.data.model.UiState.Success)?.data
                            ?: emptyList()
                        RecentsScreen(
                            callLogState = callLogState,
                            favorites = favList,
                            activeFilter = callFilter,
                            searchQuery = recentsSearch,
                            onFilterChange = recentsViewModel::setFilter,
                            onSearchQueryChange = recentsViewModel::updateSearchQuery,
                            onRequestCallLogPermission = {
                                readCallLogLauncher.launch(Manifest.permission.READ_CALL_LOG)
                            },
                            onOpenAppSettings = { PermissionHelper.openAppSettings(context) },
                            onRetry = recentsViewModel::refreshCallLog,
                            onCallNumber = { number -> initiateCall(number, null) },
                            onPrefillKeypad = { number ->
                                keypadViewModel.prefillNumber(number)
                                navigateToTopLevel(KeypadRoute)
                            },
                            onBlockNumber = { number, label ->
                                blocklistViewModel.blockNumber(number, label)
                            },
                            onOpenContactDetail = { id ->
                                contactsViewModel.loadContactDetail(id)
                                navController.navigate(ContactDetailsRoute(id))
                            },
                            onOpenKeypad = { navigateToTopLevel(KeypadRoute) },
                            onOpenBlockedNumbers = { navController.navigate(BlockedNumbersRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onOpenDefaultDialerStatus = {
                                navController.navigate(DefaultDialerStatusRoute)
                            }
                        )
                    }

                    composable<ContactsRoute> {
                        val favList = (favoritesState as? com.example.smartdialer.data.model.UiState.Success)?.data
                            ?: emptyList()
                        ContactsScreen(
                            contactsState = contactsState,
                            favorites = favList,
                            searchQuery = contactsSearch,
                            onSearchQueryChange = contactsViewModel::updateSearchQuery,
                            onRequestContactsPermission = {
                                readContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            onOpenAppSettings = { PermissionHelper.openAppSettings(context) },
                            onRetry = contactsViewModel::refreshContacts,
                            onOpenContactDetail = { id ->
                                contactsViewModel.loadContactDetail(id)
                                navController.navigate(ContactDetailsRoute(id))
                            },
                            onCallNumber = { number -> initiateCall(number, null) },
                            onAddNewContact = { editorDraft = ContactDraft() },
                            onSeeAllFavorites = { navigateToTopLevel(FavoritesRoute) },
                            onOpenBlockedNumbers = { navController.navigate(BlockedNumbersRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onOpenDefaultDialerStatus = {
                                navController.navigate(DefaultDialerStatusRoute)
                            }
                        )
                    }

                    composable<KeypadRoute> {
                        KeypadScreen(
                            rawDigits = rawDigits,
                            formattedDigits = formattedDigits,
                            t9Matches = t9Matches,
                            availableSims = availableSims,
                            selectedSimSlot = selectedSimSlot,
                            vibrationEnabled = preferences.vibrationEnabled,
                            onDigitPress = keypadViewModel::appendDigit,
                            onBackspace = keypadViewModel::backspace,
                            onClearAll = keypadViewModel::clearAll,
                            onSelectSim = keypadViewModel::selectSim,
                            onPlaceCall = initiateCall,
                            onAddContactWithNumber = { num ->
                                editorDraft = ContactDraft(phoneNumber = num)
                            },
                            onOpenContactDetail = { id ->
                                contactsViewModel.loadContactDetail(id)
                                navController.navigate(ContactDetailsRoute(id))
                            },
                            onOpenBlockedNumbers = { navController.navigate(BlockedNumbersRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onOpenDefaultDialerStatus = {
                                navController.navigate(DefaultDialerStatusRoute)
                            }
                        )
                    }

                    composable<ContactDetailsRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<ContactDetailsRoute>()
                        LaunchedEffect(route.contactId) {
                            contactsViewModel.loadContactDetail(route.contactId)
                        }
                        val detailContact = (selectedDetailState as? com.example.smartdialer.data.model.UiState.Success)?.data
                        val isBlocked = remember(detailContact, blockedNumbers) {
                            detailContact != null && blockedNumbers.any { entry ->
                                BlocklistMatcher.isMatch(detailContact.primaryNumber, entry.normalizedNumber)
                            }
                        }
                        ContactDetailsScreen(
                            detailState = selectedDetailState,
                            localNote = selectedContactNote,
                            isBlocked = isBlocked,
                            onBack = { navController.popBackStack() },
                            onCallNumber = { number -> initiateCall(number, null) },
                            onMessageNumber = { number ->
                                DialerTelecomManager.openSmsApp(context, number)
                            },
                            onToggleFavorite = contactsViewModel::toggleFavorite,
                            onToggleBlock = { contact, shouldBlock ->
                                if (shouldBlock) {
                                    blocklistViewModel.blockNumber(contact.primaryNumber, contact.displayName)
                                } else {
                                    blocklistViewModel.unblockNumber(contact.primaryNumber)
                                }
                            },
                            onEditContact = { contact ->
                                editorDraft = ContactDraft(
                                    contactId = contact.id,
                                    displayName = contact.displayName,
                                    phoneNumber = contact.primaryNumber,
                                    email = contact.emails.firstOrNull().orEmpty(),
                                    organization = contact.organization.orEmpty(),
                                    photoUri = contact.photoUri
                                )
                            },
                            onDeleteContactConfirmed = deleteContactWithConsent,
                            onSaveNote = { lookupKey, note ->
                                contactsViewModel.saveNote(lookupKey, note)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.contact_saved_success)
                                    )
                                }
                            },
                            onRetry = { contactsViewModel.loadContactDetail(route.contactId) },
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(
                            preferences = preferences,
                            defaultDialerStatus = defaultDialerStatus,
                            blockedCount = blockedNumbers.size,
                            isCallScreeningActive = isCallScreeningActive,
                            isExportingContacts = isExportingContacts,
                            grantedPermissionsCount = grantedPermissionsCount,
                            totalPermissionsCount = PermissionHelper.ALL_RUNTIME_PERMISSIONS.size,
                            signedInAccountLabel = signedInUser?.email ?: signedInUser?.displayName,
                            onBack = { navController.popBackStack() },
                            onThemeModeChange = settingsViewModel::setThemeMode,
                            onAccentColorChange = settingsViewModel::setAccentColor,
                            onFontSizeChange = settingsViewModel::setFontSizeScale,
                            onVibrationChange = settingsViewModel::setVibrationEnabled,
                            onLanguageChange = settingsViewModel::setLanguage,
                            onAutoCallRecordingChange = settingsViewModel::setAutoCallRecordingEnabled,
                            onCallRecordingScopeChange = settingsViewModel::setCallRecordingScope,
                            onExportContacts = { format ->
                                if (PermissionHelper.hasContactsReadPermission(context)) {
                                    launchExportDocumentPicker(format)
                                } else {
                                    showExportPermissionRationale = format
                                }
                            },
                            onOpenCloudSync = { navController.navigate(CloudSyncRoute) },
                            onOpenAppPermissions = { navController.navigate(AppPermissionsRoute) },
                            onOpenBlockedNumbers = { navController.navigate(BlockedNumbersRoute) },
                            onOpenPrivacyInfo = { navController.navigate(PrivacyInfoRoute) },
                            onOpenDefaultDialerStatus = {
                                navController.navigate(DefaultDialerStatusRoute)
                            }
                        )
                    }

                    composable<CloudSyncRoute> {
                        CloudSyncScreen(
                            onBack = { navController.popBackStack() },
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                    }

                    composable<AppPermissionsRoute> {
                        AppPermissionsScreen(
                            defaultDialerStatus = defaultDialerStatus,
                            isCallScreeningActive = isCallScreeningActive,
                            onBack = { navController.popBackStack() },
                            onPermissionsUpdated = { grantedAll ->
                                grantedPermissionsCount =
                                    PermissionHelper.grantedRuntimePermissionCount(context)
                                if (PermissionHelper.hasContactsReadPermission(context)) {
                                    contactsViewModel.onReadPermissionResult(true, false)
                                }
                                if (PermissionHelper.hasCallLogPermission(context)) {
                                    recentsViewModel.onPermissionResult(true, false)
                                }
                                keypadViewModel.refreshSims()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(
                                            if (grantedAll) {
                                                R.string.permissions_all_granted_snackbar
                                            } else {
                                                R.string.permissions_partial_denied_snackbar
                                            }
                                        )
                                    )
                                }
                            },
                            onRefreshRoles = {
                                settingsViewModel.refreshDefaultDialerStatus()
                                blocklistViewModel.refreshScreeningRoleStatus()
                            }
                        )
                    }

                    composable<BlockedNumbersRoute> {
                        BlockedNumbersScreen(
                            blockedNumbers = blockedNumbers,
                            isCallScreeningActive = isCallScreeningActive,
                            availableContacts = allContacts,
                            onRefreshRoleStatus = blocklistViewModel::refreshScreeningRoleStatus,
                            onAddBlockedNumber = blocklistViewModel::blockNumber,
                            onUnblock = blocklistViewModel::unblockById,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<PrivacyInfoRoute> {
                        PrivacyInfoScreen(
                            onBack = { navController.popBackStack() },
                            onOpenAppPermissions = { navController.navigate(AppPermissionsRoute) }
                        )
                    }

                    composable<DefaultDialerStatusRoute> {
                        DefaultDialerStatusScreen(
                            status = defaultDialerStatus,
                            onRefreshStatus = settingsViewModel::refreshDefaultDialerStatus,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Contact Dialog
    editorDraft?.let { draft ->
        ContactEditorDialog(
            initialDraft = draft,
            onSave = { updated ->
                editorDraft = null
                saveContactWithConsent(updated)
            },
            onDismiss = { editorDraft = null }
        )
    }

    // Call Permission Pre-Explanation Dialog
    showCallPermissionRationale?.let { (number, sim) ->
        PermissionExplainDialog(
            title = stringResource(R.string.keypad_call_permission_title),
            message = stringResource(R.string.keypad_call_permission_rationale),
            primaryLabel = stringResource(R.string.action_grant_permission),
            secondaryLabel = stringResource(R.string.action_open_system_dialer),
            onPrimaryConfirm = {
                showCallPermissionRationale = null
                pendingCallRequest = number to sim
                callPhoneLauncher.launch(Manifest.permission.CALL_PHONE)
            },
            onSecondaryAction = {
                showCallPermissionRationale = null
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
                runCatching { context.startActivity(dialIntent) }
            },
            onDismiss = { showCallPermissionRationale = null }
        )
    }

    // Write Contacts Rationale Dialog (Create / Update)
    showWriteRationaleForDraft?.let { draft ->
        PermissionExplainDialog(
            title = stringResource(R.string.contacts_write_permission_title),
            message = stringResource(R.string.contacts_write_permission_rationale),
            primaryLabel = stringResource(R.string.action_grant_permission),
            secondaryLabel = stringResource(R.string.contacts_use_system_app),
            onPrimaryConfirm = {
                showWriteRationaleForDraft = null
                pendingWriteDraft = draft
                writeContactsLauncher.launch(Manifest.permission.WRITE_CONTACTS)
            },
            onSecondaryAction = {
                showWriteRationaleForDraft = null
                DialerTelecomManager.openSystemInsertContact(context, draft.phoneNumber)
            },
            onDismiss = { showWriteRationaleForDraft = null }
        )
    }

    // Write Contacts Rationale Dialog (Delete)
    showWriteRationaleForDelete?.let { contact ->
        PermissionExplainDialog(
            title = stringResource(R.string.contacts_write_permission_title),
            message = stringResource(R.string.contacts_write_permission_rationale),
            primaryLabel = stringResource(R.string.action_grant_permission),
            onPrimaryConfirm = {
                showWriteRationaleForDelete = null
                pendingDeleteContact = contact
                writeContactsLauncher.launch(Manifest.permission.WRITE_CONTACTS)
            },
            onDismiss = { showWriteRationaleForDelete = null }
        )
    }

    // Read Contacts Rationale Dialog for Local Backup & Export
    showExportPermissionRationale?.let { format ->
        PermissionExplainDialog(
            title = stringResource(R.string.settings_export_permission_title),
            message = stringResource(R.string.settings_export_permission_rationale),
            primaryLabel = stringResource(R.string.action_grant_permission),
            onPrimaryConfirm = {
                showExportPermissionRationale = null
                pendingExportFormat = format
                exportContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            onDismiss = { showExportPermissionRationale = null }
        )
    }
}
