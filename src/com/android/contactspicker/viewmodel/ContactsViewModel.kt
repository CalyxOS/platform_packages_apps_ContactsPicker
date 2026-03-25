/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contactspicker.viewmodel

import android.content.ClipData
import android.content.ContentProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Trace
import android.os.UserHandle
import android.util.Log
import androidx.annotation.OpenForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.ContactsPreviewState
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.Flags
import com.android.contactspicker.PrivacyDetailsState
import com.android.contactspicker.R
import com.android.contactspicker.SearchState
import com.android.contactspicker.config.ContactsPickerAction
import com.android.contactspicker.config.ContactsPickerConfigError
import com.android.contactspicker.config.ContactsPickerRequestConfig
import com.android.contactspicker.config.ContactsQueryMode
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.PausedReason
import com.android.contactspicker.data.model.PickerUserState
import com.android.contactspicker.data.model.ProfileBlockedDialogData
import com.android.contactspicker.data.model.SelectionSource
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.model.UserType
import com.android.contactspicker.data.model.emptyContactsSelection
import com.android.contactspicker.data.repository.ContactsPickerSessionProviderRepository
import com.android.contactspicker.data.repository.ContactsRepository
import com.android.contactspicker.data.repository.PrivacyBannerRepository
import com.android.contactspicker.logging.ContactsPickerLogger
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ContactsViewModel"
internal const val SEARCH_DEBOUNCE_MS = 300L

// The default selection limit when multi select is enabled. Can be overridden by passing the
// [ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT] extra in the client intent.
internal const val DEFAULT_SELECTION_LIMIT = 50
// Maximum allowed selection limit. If the value passed by the calling app in the
// [ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT] intent extra is higher an
// exception is thrown.
internal const val MAX_ALLOWED_SELECTION_LIMIT = 100

/**
 * The minimum target SDK of the caller app that will be handled by the app. Intents from callers
 * with target SDK below will be resent to the system with explicitly excluding the current activity
 * to prevent loops.
 */
internal const val ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD = 37

/** Events sent from the ViewModel to the UI to show a Snackbar. */
sealed interface SnackbarEvent {
    data class ShowSelectionLimitReached(val limit: Int) : SnackbarEvent
}

/** Events for the Activity Result. */
sealed interface PickerResultEvent {
    data class SetResultAndFinish(val intent: Intent) : PickerResultEvent

    data object CancelAndFinish : PickerResultEvent
}

/**
 * ViewModel for the Contacts Picker screen.
 *
 * This class is responsible for loading and preparing the contacts data to be displayed by the UI.
 */
@OpenForTesting
@HiltViewModel
open class ContactsViewModel
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val contactsPickerSessionProviderRepository: ContactsPickerSessionProviderRepository,
    private val privacyBannerRepository: PrivacyBannerRepository,
    private val profileSelectionHandler: Lazy<ProfileSelectionHandler>,
    private val selectionHandlerFactory: ContactsSelectionHandler.Factory,
    private val contactsPickerLogger: ContactsPickerLogger,
) : ViewModel() {

    private val contentResolver = context.contentResolver

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsListState.Loading)
    open val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _userState = MutableStateFlow<PickerUserState>(PickerUserState.Loading)
    open val userState: StateFlow<PickerUserState> = _userState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>()
    open val snackbarEvents: Flow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    private val _pickerResultEvents = Channel<PickerResultEvent>(Channel.BUFFERED)
    open val pickerResultEvents = _pickerResultEvents.receiveAsFlow()

    private var selectionHandler: ContactsSelectionHandler? = null
    private var selectionCollectorJob: Job? = null
    private var userStateCollectorJob: Job? = null

    private var contactsGrouper: ContactsGrouper? = null
    private var initialContacts: List<Contact> = emptyList()
    private var callingAppName: String? = null
    private var callingPackageName: String? = null
    private var searchJob: Job? = null
    private var loadContactsJob: Job? = null
    private var cachedStateBeforeNavigation: ContactsUiState? = null
    private var callingAppUid: Int = -1
    private val callingUserId: Int
        get() = UserHandle.getUserId(callingAppUid)

    private var pickerConfig: ContactsPickerRequestConfig? = null

    private var showPrivacyBanner = false
    private var privacyBannerVisibilityEvaluated = false

    /**
     * Toggles the selection state for an entire contact.
     *
     * Delegates logic to [ContactsSelectionHandler].
     */
    fun toggleContactSelection(contact: Contact, selectionSource: SelectionSource) =
        checkNotNull(selectionHandler).toggleContactSelection(contact, selectionSource)

    /**
     * Toggles the selection state for a single contact entry (e.g., one email or one phone number).
     *
     * Delegates logic to [ContactsSelectionHandler].
     */
    fun toggleEntrySelection(contactId: Long, entryId: Long, selectionSource: SelectionSource) =
        checkNotNull(selectionHandler).toggleEntrySelection(contactId, entryId, selectionSource)

    /** Clears all currently selected contacts. */
    fun clearSelection() = checkNotNull(selectionHandler).clearSelection()

    /**
     * Processes the intent fields and, if handled internally, initializes the session and starts
     * data loading.
     *
     * @return true if handled internally; false if the intent should be forwarded.
     */
    @OpenForTesting
    open fun handleIntent(
        intentAction: String?,
        intentType: String?,
        intentExtras: Bundle?,
        callingAppName: String?,
        callingPackageName: String?,
        callingAppUid: Int,
        callingAppTargetSdk: Int,
    ): Boolean {
        this.callingAppName = callingAppName
        this.callingPackageName = callingPackageName
        this.callingAppUid = callingAppUid

        // reset to ensure it's evaluated for the new intent
        privacyBannerVisibilityEvaluated = false

        val useSystemContactsPicker =
            intentExtras?.getBoolean(Intent.EXTRA_USE_SYSTEM_CONTACTS_PICKER, false) ?: false

        val result = ContactsPickerRequestConfig.create(intentAction, intentType, intentExtras)

        when (result) {
            is ContactsPickerConfigError -> {
                Log.e(TAG, "Invalid intent configuration: ${result.message}")

                // any valid partial data will be logged
                contactsPickerLogger.logContactsPickerSessionStarted(
                    callingAppUid = callingAppUid,
                    callingAppTargetSdk = callingAppTargetSdk,
                    pickerIntentAction = result.parsedAction,
                    requestedMimeTypes = result.parsedMimeTypes,
                    useSystemContactsPicker = useSystemContactsPicker,
                    matchAllRequestedMimeTypes = result.parsedMatchAll,
                )

                contactsPickerLogger.logContactsPickerSessionFailed(result.errorType)

                viewModelScope.launch {
                    _pickerResultEvents.send(PickerResultEvent.CancelAndFinish)
                }
                return true
            }

            is ContactsPickerRequestConfig -> {
                this.pickerConfig = result

                contactsPickerLogger.logContactsPickerSessionStarted(
                    callingAppUid = callingAppUid,
                    callingAppTargetSdk = callingAppTargetSdk,
                    pickerIntentAction = result.pickerAction,
                    requestedMimeTypes = result.requestedMimeTypes,
                    useSystemContactsPicker = useSystemContactsPicker,
                    matchAllRequestedMimeTypes = result.matchAllRequestedMimeTypes,
                )

                if (!shouldHandleIntent(callingAppTargetSdk, useSystemContactsPicker)) {
                    contactsPickerLogger.logContactsPickerSessionForwarded()
                    return false
                }

                // TODO(b/479454402): Refactor isUserSwitchingEnabled into
                // ContactsPickerRequestConfig
                // to decouple from ACTION_PICK_CONTACTS
                val isUserSwitchingEnabled =
                    result.pickerAction == ContactsPickerAction.ACTION_PICK_CONTACTS

                if (isUserSwitchingEnabled) {
                    profileSelectionHandler.get().clearSelectedUser()
                    // reset to ensure it's evaluated for a new intent
                    _userState.value = PickerUserState.Loading
                    startObservingUserState(result)
                } else {
                    val defaultState =
                        PickerUserState.Success(
                            userIdToAvailableUsersMap = emptyMap(),
                            selectedUserId = UserHandle.myUserId(),
                        )
                    _userState.value = defaultState
                    loadContactsListData(result, defaultState)
                }

                selectionHandler =
                    selectionHandlerFactory.create(
                        result.isMultiSelectEnabled,
                        result.maxSelectionLimit,
                    ) { event ->
                        viewModelScope.launch { _snackbarEvents.emit(event) }
                    }

                startObservingSelection()
                return true
            }
        }
    }

    /** Returns true if the intent is eligible for internal handling based on SDK and flags. */
    private fun shouldHandleIntent(targetSdk: Int, useSystemContactsPicker: Boolean): Boolean {
        return targetSdk >= ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD ||
            useSystemContactsPicker ||
            Flags.enableActionPickTakeoverInDroidfood()
    }

    private fun startObservingSelection() {
        selectionCollectorJob?.cancel()
        selectionCollectorJob =
            viewModelScope.launch {
                checkNotNull(selectionHandler).selectedContacts.collect { newSelection ->
                    _uiState.update { currentState ->
                        when (currentState) {
                            is ContactsListState.Success ->
                                currentState.copy(selectedContacts = newSelection)
                            is SearchState.Success ->
                                currentState.copy(selectedContacts = newSelection)
                            is ContactsPreviewState ->
                                if (newSelection.isEmpty()) {
                                    onBackFromPreview()
                                    currentState
                                } else {
                                    currentState.copy(
                                        selectedContacts = newSelection,
                                        contactsToDisplay =
                                            currentState.contactsToDisplay.filter { contact ->
                                                newSelection.containsKey(contact.id)
                                            },
                                    )
                                }
                            else -> currentState
                        }
                    }
                }
            }
    }

    private fun startObservingUserState(config: ContactsPickerRequestConfig) {
        userStateCollectorJob?.cancel()
        userStateCollectorJob =
            viewModelScope.launch {
                profileSelectionHandler
                    .get()
                    .getUserStateFlow(callingPackageName, UserHandle.getUserId(callingAppUid))
                    .collect { userState ->
                        if (userState !is PickerUserState.Success) {
                            _userState.value = userState
                            return@collect
                        }

                        val lastSelectedUserId =
                            (_userState.value as? PickerUserState.Success)?.selectedUserId
                        val currentSelectedUserId = userState.selectedUserId

                        _userState.value = userState

                        if (lastSelectedUserId == currentSelectedUserId) {
                            return@collect
                        }

                        // User switched. Clear selection (if not initial load) and reload.
                        if (lastSelectedUserId != null) {
                            clearSelection()
                        }
                        loadContactsListData(config, userState)
                    }
            }
    }

    private fun loadContactsListData(
        config: ContactsPickerRequestConfig,
        userState: PickerUserState.Success,
    ) {
        contactsPickerLogger.allContactsLoadingStarted()
        loadContactsJob?.cancel()
        loadContactsJob =
            viewModelScope.launch {
                _uiState.value = ContactsListState.Loading
                try {
                    Trace.beginSection("$TAG#loadContactsListData")
                    // load contacts data
                    Trace.beginSection("$TAG#contactsRepository.getContacts")
                    val (loadedContacts, initialContactGroupingMetadata) =
                        contactsRepository.getContacts(config.queryMode, userState.selectedUserId)
                    initialContacts = loadedContacts

                    val availableContactsGroups =
                        ContactsGrouper(initialContacts, initialContactGroupingMetadata)
                            .also { contactsGrouper = it }
                            .availableContactsGroups

                    contactsPickerLogger.allContactsLoadingFinished()
                    Trace.endSection()
                    if (initialContacts.isNotEmpty()) {
                        // Only show the privacy banner if user hasn't seen it before for this
                        // combination of uid and MIME types. Evaluate only once per picker session.
                        if (!privacyBannerVisibilityEvaluated) {
                            showPrivacyBanner =
                                !privacyBannerRepository.wasPrivacyBannerShown(
                                    callingAppUid,
                                    config.requestedMimeTypes,
                                )
                            privacyBannerVisibilityEvaluated = true
                        }

                        _uiState.value =
                            ContactsListState.Success(
                                availableContactsGroups = availableContactsGroups,
                                selectedContacts =
                                    checkNotNull(selectionHandler).selectedContacts.value,
                                isMultiSelectEnabled = config.isMultiSelectEnabled,
                                callingAppName = callingAppName,
                                requestedMimeTypes = config.requestedMimeTypes,
                                showPrivacyBanner = showPrivacyBanner,
                            )
                    } else {
                        val (noContactsTitleText, noContactsDescriptionText) =
                            when (config.queryMode) {
                                ContactsQueryMode.EmailsOnly ->
                                    context.getString(R.string.no_email_contacts_title) to null
                                ContactsQueryMode.PhonesOnly ->
                                    context.getString(R.string.no_phone_contacts_title) to null
                                ContactsQueryMode.DisplayNamesOnly ->
                                    context.getString(R.string.no_contacts_title) to
                                        context.getString(R.string.no_contacts_description)
                                is ContactsQueryMode.Custom -> {
                                    if (contactsRepository.hasAnyContacts(userState.selectedUserId))
                                        context.getString(
                                            R.string.no_custom_details_contacts_title
                                        ) to null
                                    else
                                        context.getString(R.string.no_contacts_title) to
                                            context.getString(R.string.no_contacts_description)
                                }
                            }
                        _uiState.value =
                            ContactsListState.NoResults(
                                titleText = noContactsTitleText,
                                descriptionText = noContactsDescriptionText,
                            )
                    }
                } catch (e: Exception) {
                    // TODO(b/444459883): iterate on error handling and error messages
                    if (e is kotlinx.coroutines.CancellationException) {
                        Log.i(TAG, "Contacts loading cancelled.")
                        return@launch
                    }
                    Log.e(TAG, "An unexpected error occurred during load.", e)
                    _uiState.value =
                        ContactsListState.Error(e.message ?: "An unexpected error occurred.")
                } finally {
                    Trace.endSection()
                }
            }
    }

    /** Hides the privacy banner for the current session. */
    fun hidePrivacyBanner() {
        showPrivacyBanner = false
        contactsPickerLogger.privacyBannerDismissedByUser()
        _uiState.update { currentState ->
            if (currentState is ContactsListState.Success) {
                currentState.copy(showPrivacyBanner = showPrivacyBanner)
            } else {
                currentState
            }
        }
    }

    /**
     * Called when the user clicks the "Done" button. Acts as the single entry point for finishing
     * the selection process.
     */
    @OpenForTesting
    open fun onDoneClicked() {
        val config = checkNotNull(pickerConfig)
        val currentState = _uiState.value
        check(
            currentState is ContactsListState.Success ||
                currentState is SearchState.Success ||
                currentState is ContactsPreviewState
        ) {
            "onDoneClicked called while not in a Success state."
        }
        val handler = checkNotNull(selectionHandler)
        viewModelScope.launch {
            if (handler.selectedContacts.value.isEmpty()) {
                contactsPickerLogger.logContactsPickerSessionCancelled()
                _pickerResultEvents.send(PickerResultEvent.CancelAndFinish)
                return@launch
            }

            val (resultIntent, numContactsSelected) =
                when (config.pickerAction) {
                    ContactsPickerAction.ACTION_PICK -> {
                        val finalUris = handler.resolveSelectedUris(initialContacts)
                        createActionPickResult(finalUris, config.isMultiSelectEnabled) to
                            finalUris.size
                    }
                    ContactsPickerAction.ACTION_PICK_CONTACTS -> {
                        try {
                            Trace.beginSection("$TAG#finishingPickerSession")
                            // TODO(b/37307800): consider setting _uiState.update {
                            // it.copy(isLoading = true) }
                            val selectedIds = handler.getSelectedIds()

                            val userId =
                                (_userState.value as? PickerUserState.Success)?.selectedUserId
                                    ?: UserHandle.getUserId(callingAppUid)
                            createActionPickContactsResult(selectedIds, config.queryMode, userId)
                        } finally {
                            Trace.endSection()
                        }
                    }
                }

            if (resultIntent != null) {
                contactsPickerLogger.logContactsPickerSessionFinishedSuccessfully(
                    numContactsSelected = numContactsSelected,
                    contactsSelectedFromFavorites =
                        handler.wasSelectedFrom(SelectionSource.FAVORITES),
                    contactsSelectedFromSearch = handler.wasSelectedFrom(SelectionSource.SEARCH),
                )
                _pickerResultEvents.send(PickerResultEvent.SetResultAndFinish(resultIntent))
            } else {
                // TODO(b/441483549): Log cancelled event with correct error code
                _pickerResultEvents.send(PickerResultEvent.CancelAndFinish)
            }
        }
    }

    private fun createActionPickResult(uris: List<Uri>, isMultiSelectEnabled: Boolean): Intent? {
        if (uris.isEmpty()) {
            return null
        }

        val resultUris = uris.map { ContentProvider.maybeAddUserId(it, callingUserId) }

        return Intent().apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (isMultiSelectEnabled) {
                clipData =
                    ClipData.newUri(contentResolver, "uri", resultUris.first()).apply {
                        resultUris.drop(1).forEach { addItem(ClipData.Item(it)) }
                    }
            } else {
                data = resultUris.first()
            }
        }
    }

    private suspend fun createActionPickContactsResult(
        ids: List<Long>,
        queryMode: ContactsQueryMode,
        userId: Int,
    ): Pair<Intent?, Int> {
        if (ids.isEmpty()) {
            return null to 0
        }

        val finalIds =
            when (queryMode) {
                is ContactsQueryMode.EmailsOnly,
                is ContactsQueryMode.PhonesOnly -> ids
                is ContactsQueryMode.Custom ->
                    contactsRepository.getDataRowIds(ids, queryMode.mimetypes, userId)

                is ContactsQueryMode.DisplayNamesOnly ->
                    throw IllegalStateException("Wrong query mode for ACTION_PICK_CONTACTS")
            }

        return getActionPickContactsIntent(finalIds, userId) to finalIds.size
    }

    private suspend fun getActionPickContactsIntent(dataIds: List<Long>, userId: Int): Intent {
        return Intent().apply {
            data =
                contactsPickerSessionProviderRepository.createSession(
                    dataIds,
                    callingAppUid,
                    userId,
                )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Handles profile selection.
     *
     * @param userId The ID of the selected user profile.
     */
    fun onProfileSelected(userId: Int) {
        val userState = _userState.value as? PickerUserState.Success ?: return
        if (userId == userState.selectedUserId) return

        profileSelectionHandler.get().setSelectedUser(userId)
    }

    /**
     * Handles clicks on a profile in the profile switcher.
     *
     * @param userProfile The clicked user profile.
     */
    fun onProfileClicked(userProfile: UserProfile) {
        if (userProfile.pausedInfo != null) {
            showProfilePausedDialog(userProfile)
            return
        }

        if (userProfile.switchableInfo != null) {
            onProfileSelected(userProfile.userId)
        }
    }

    private fun showProfilePausedDialog(userProfile: UserProfile, reason: PausedReason? = null) {
        val dialogData = createProfilePausedDialogData(userProfile, reason)
        _userState.update { currentState ->
            if (currentState is PickerUserState.Success) {
                currentState.copy(profileBlockedDialogData = dialogData)
            } else {
                currentState
            }
        }
    }

    private fun createProfilePausedDialogData(
        userProfile: UserProfile,
        reason: PausedReason? = null,
    ): ProfileBlockedDialogData? {
        val actualReason =
            reason ?: userProfile.pausedInfo?.pausedReason ?: PausedReason.UNKNOWN_REASON
        return when (actualReason) {
            PausedReason.MANAGED_PROFILE_CONTACTS_BLOCKED ->
                ProfileBlockedDialogData(
                    title = context.getString(R.string.picker_profile_blocked_admin_title),
                    message = context.getString(R.string.picker_profile_blocked_admin_msg),
                )
            PausedReason.QUIET_MODE ->
                if (userProfile.userType == UserType.WORK) {
                    ProfileBlockedDialogData(
                        title = context.getString(R.string.picker_work_profile_paused_title),
                        message = context.getString(R.string.picker_work_profile_paused_msg),
                    )
                } else {
                    null
                }
            PausedReason.UNKNOWN_REASON -> null
        }
    }

    /**
     * Dismisses the profile blocked dialog.
     *
     * This simply hides the dialog overlay. The user remains on the currently selected profile
     * (even if it is paused/blocked).
     */
    fun dismissProfileBlockedDialog() {
        _userState.update { currentState ->
            if (currentState is PickerUserState.Success) {
                currentState.copy(profileBlockedDialogData = null)
            } else {
                currentState
            }
        }
    }

    /**
     * Handles changes in the search query by updating the searchQuery flow.
     *
     * @param query The search query.
     */
    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            // If the user clears the search, go to empty search state
            _uiState.update { _ ->
                SearchState.Success(
                    query = "",
                    searchResults = emptyList(),
                    selectedContacts = checkNotNull(selectionHandler).selectedContacts.value,
                )
            }
            return
        }

        // Debounce for non-blank queries
        searchJob =
            viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                performSearch(query)
            }
    }

    /** Executes the search against the repository and updates the UI state. */
    private suspend fun performSearch(query: String) {
        val config = checkNotNull(pickerConfig)
        contactsPickerLogger.searchUsed()
        try {
            Trace.beginSection("$TAG#searchingContacts")

            val userState = _userState.filterIsInstance<PickerUserState.Success>().first()
            val selectedUserId = userState.selectedUserId

            val results = contactsRepository.searchContacts(query, config.queryMode, selectedUserId)
            _uiState.update { currentState ->
                val selectedContacts =
                    when (currentState) {
                        is ContactsListState.Success -> currentState.selectedContacts
                        is SearchState.Success -> currentState.selectedContacts
                        else -> emptyContactsSelection()
                    }
                SearchState.Success(
                    query = query,
                    searchResults = results,
                    selectedContacts = selectedContacts,
                )
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "An invalid intent was passed during search.", e)
            _uiState.value = SearchState.Error(e.message ?: "Invalid intent for search.")
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                return
            }
            Log.e(TAG, "An unexpected error occurred during search.", e)
            _uiState.value = SearchState.Error("An unexpected error occurred during search.")
        } finally {
            Trace.endSection()
        }
    }

    /**
     * Reverts the UI state from SearchState back to ContactsListState.Success, preserving the
     * current selection.
     */
    fun exitSearch() {
        val config = checkNotNull(pickerConfig)
        _uiState.update { currentState ->
            if (currentState is SearchState) {
                val selectedContacts =
                    when (currentState) {
                        is SearchState.Success -> currentState.selectedContacts
                        else ->
                            emptyContactsSelection() // Should not happen if exiting from Success
                    }
                val availableContactsGroups = checkNotNull(contactsGrouper).availableContactsGroups
                ContactsListState.Success(
                    availableContactsGroups = availableContactsGroups,
                    selectedContacts = selectedContacts,
                    isMultiSelectEnabled = config.isMultiSelectEnabled,
                    callingAppName = callingAppName,
                    requestedMimeTypes = config.requestedMimeTypes,
                    showPrivacyBanner = showPrivacyBanner,
                )
            } else {
                currentState
            }
        }
    }

    fun onPreviewClicked() {
        val config = checkNotNull(pickerConfig)
        val currentState = _uiState.value
        require(currentState is ContactsListState.Success || currentState is SearchState.Success) {
            "onPreviewClicked called from unexpected state: $currentState"
        }

        cachedStateBeforeNavigation = currentState

        val (selectedIds, isMultiSelectEnabled) =
            when (currentState) {
                is ContactsListState.Success ->
                    currentState.selectedContacts to currentState.isMultiSelectEnabled
                is SearchState.Success -> {
                    currentState.selectedContacts to config.isMultiSelectEnabled
                }
                else -> return
            }

        contactsPickerLogger.previewOpened()

        val previewList = initialContacts.filter { contact -> selectedIds.containsKey(contact.id) }

        _uiState.value =
            ContactsPreviewState(
                contactsToDisplay = previewList,
                selectedContacts = selectedIds,
                isMultiSelectEnabled = isMultiSelectEnabled,
            )
    }

    fun onBackFromPreview() {
        val currentState = _uiState.value
        val currentCachedState = cachedStateBeforeNavigation
        require(currentState is ContactsPreviewState && currentCachedState != null) {
            "onBackFromPreview called from unexpected state: $currentState, or no previous state found"
        }
        // update selected contacts in cached state
        _uiState.value =
            when (currentCachedState) {
                is ContactsListState.Success ->
                    currentCachedState.copy(selectedContacts = currentState.selectedContacts)
                is SearchState.Success ->
                    currentCachedState.copy(selectedContacts = currentState.selectedContacts)
                else -> currentCachedState
            }
        cachedStateBeforeNavigation = null
    }

    @OpenForTesting
    open fun onPrivacyDetailsBannerClicked() {
        contactsPickerLogger.privacyDetailsBannerOpened()
        navigateToPrivacyDetails()
    }

    fun onPrivacyDetailsOverflowMenuClicked() {
        contactsPickerLogger.privacyDetailsOverflowMenuOpened()
        navigateToPrivacyDetails()
    }

    private fun navigateToPrivacyDetails() {
        val currentState = _uiState.value
        require(currentState is ContactsListState.Success) {
            "onPrivacyDetailsClicked called from unexpected state: $currentState"
        }

        cachedStateBeforeNavigation = currentState

        _uiState.value =
            PrivacyDetailsState(
                callingAppName = currentState.callingAppName,
                requestedMimeTypes = currentState.requestedMimeTypes,
            )
    }

    @OpenForTesting
    open fun onBackFromPrivacyDetails() {
        val currentState = _uiState.value
        val currentCachedState = cachedStateBeforeNavigation
        require(currentState is PrivacyDetailsState && currentCachedState != null) {
            "onBackFromPrivacyDetails called from unexpected state: $currentState, or no previous state found"
        }
        _uiState.value = currentCachedState
        cachedStateBeforeNavigation = null
    }

    /** Override the ViewModel lifecycle method to catch when the view model is destroyed */
    override fun onCleared() {
        // Always send a signal to the logger. If the session end was logged as success/failure
        // earlier, the session data will be null and this call will be a no-op. It will log if the
        // view model is destroyed because the user exited the app with an unfinished session.
        contactsPickerLogger.logContactsPickerSessionCancelled()
        super.onCleared()
    }
}
