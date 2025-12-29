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
import com.android.contactspicker.SearchState
import com.android.contactspicker.config.ContactsPickerAction
import com.android.contactspicker.config.ContactsPickerRequestConfig
import com.android.contactspicker.config.ContactsQueryMode
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.data.model.emptyContactsSelection
import com.android.contactspicker.data.repository.ContactsPickerSessionProviderRepository
import com.android.contactspicker.data.repository.ContactsRepository
import com.android.contactspicker.data.repository.PrivacyBannerRepository
import com.android.contactspicker.data.repository.UserRepository
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
    @ApplicationContext context: Context,
    private val contactsRepository: ContactsRepository,
    private val contactsPickerSessionProviderRepository: ContactsPickerSessionProviderRepository,
    private val privacyBannerRepository: PrivacyBannerRepository,
    private val userRepository: Lazy<UserRepository>,
    private val selectionHandlerFactory: ContactsSelectionHandler.Factory,
) : ViewModel() {

    private val contentResolver = context.contentResolver

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsListState.Loading)
    open val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>()
    open val snackbarEvents: Flow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    private val _pickerResultEvents = Channel<PickerResultEvent>(Channel.BUFFERED)
    open val pickerResultEvents = _pickerResultEvents.receiveAsFlow()

    private var selectionHandler: ContactsSelectionHandler? = null
    private var selectionCollectorJob: Job? = null

    private var initialContacts: List<Contact> = emptyList()
    private var callingAppName: String? = null
    private var searchJob: Job? = null
    private var loadContactsJob: Job? = null
    private var cachedStateBeforePreview: ContactsUiState? = null
    private var callingAppUid: Int = -1

    private var pickerConfig: ContactsPickerRequestConfig? = null

    private var showPrivacyBanner = false

    /**
     * Toggles the selection state for an entire contact.
     *
     * Delegates logic to [ContactsSelectionHandler].
     */
    fun toggleContactSelection(contact: Contact) =
        checkNotNull(selectionHandler).toggleContactSelection(contact)

    /**
     * Toggles the selection state for a single contact entry (e.g., one email or one phone number).
     *
     * Delegates logic to [ContactsSelectionHandler].
     */
    fun toggleEntrySelection(contactId: Long, entryId: Long) =
        checkNotNull(selectionHandler).toggleEntrySelection(contactId, entryId)

    /** Clears all currently selected contacts. */
    fun clearSelection() = checkNotNull(selectionHandler).clearSelection()

    /**
     * Determines the display mode based on the intent. Should only be called from the Activity to
     * trigger the ViewModel's logic, as it changes the [ContactsUiState].
     */
    @OpenForTesting
    open fun processIntent(
        intentAction: String?,
        intentType: String?,
        intentExtras: Bundle?,
        callingAppName: String?,
        callingAppUid: Int,
    ) {
        this.callingAppName = callingAppName
        this.callingAppUid = callingAppUid

        val config =
            ContactsPickerRequestConfig.create(intentAction, intentType, intentExtras).also {
                pickerConfig = it
            }

        // TODO(b/479454402): Refactor isUserSwitchingEnabled into ContactsPickerRequestConfig
        // to decouple from ACTION_PICK_CONTACTS
        val isUserSwitchingEnabled =
            config.pickerAction == ContactsPickerAction.ACTION_PICK_CONTACTS
        if (isUserSwitchingEnabled) {
            viewModelScope.launch { userRepository.get().clearSelectedUser() }
        }

        selectionHandler =
            selectionHandlerFactory.create(config.isMultiSelectEnabled, config.maxSelectionLimit) {
                event ->
                viewModelScope.launch { _snackbarEvents.emit(event) }
            }
        startObservingSelection()
        loadContactsListData(config)
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

    private fun loadContactsListData(config: ContactsPickerRequestConfig) {
        loadContactsJob?.cancel()
        loadContactsJob =
            viewModelScope.launch {
                _uiState.value = ContactsListState.Loading
                try {
                    Trace.beginSection("$TAG#loadingContacts")
                    // load contacts data
                    initialContacts =
                        contactsRepository.getContacts(
                            config.queryMode,
                            // TODO(b/449960997): Use selected user ID from UserStates
                            UserHandle.myUserId(),
                        )
                    // Only show the privacy banner if user hasn't seen it before for this
                    // combination of uid and MIME types.
                    Trace.endSection()
                    showPrivacyBanner =
                        !privacyBannerRepository.wasPrivacyBannerShown(
                            callingAppUid,
                            config.requestedMimeTypes,
                        )

                    // TODO(b/444459883): check and handle empty list
                    _uiState.value =
                        ContactsListState.Success(
                            availableContacts = initialContacts,
                            selectedContacts =
                                checkNotNull(selectionHandler).selectedContacts.value,
                            isMultiSelectEnabled = config.isMultiSelectEnabled,
                            callingAppName = callingAppName,
                            requestedMimeTypes = config.requestedMimeTypes,
                            showPrivacyBanner = showPrivacyBanner,
                        )
                } catch (e: Exception) {
                    // TODO(b/444459883): iterate on error handling and error messages
                    if (e is kotlinx.coroutines.CancellationException) {
                        Log.i(TAG, "Contacts loading cancelled.")
                        return@launch
                    }
                    Log.e(TAG, "An unexpected error occurred during load.", e)
                    _uiState.value =
                        ContactsListState.Error(e.message ?: "An unexpected error occurred.")
                }
            }
    }

    /** Hides the privacy banner for the current session. */
    fun hidePrivacyBanner() {
        showPrivacyBanner = false

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
        viewModelScope.launch {
            val handler = checkNotNull(selectionHandler)
            if (handler.selectedContacts.value.isEmpty()) {
                _pickerResultEvents.send(PickerResultEvent.CancelAndFinish)
                return@launch
            }

            val resultIntent: Intent? =
                when (config.pickerAction) {
                    ContactsPickerAction.ACTION_PICK -> {
                        val finalUris = handler.resolveSelectedUris(initialContacts)
                        createActionPickResult(finalUris, config.isMultiSelectEnabled)
                    }
                    ContactsPickerAction.ACTION_PICK_CONTACTS -> {
                        Trace.beginSection("$TAG#finishingPickerSession")
                        // TODO(b/37307800): consider setting _uiState.update {
                        // it.copy(isLoading = true) }
                        val selectedIds = handler.getSelectedIds()
                        val intent = createActionPickContactsResult(selectedIds, config.queryMode)
                        Trace.endSection()
                        intent
                    }
                }

            if (resultIntent != null) {
                _pickerResultEvents.send(PickerResultEvent.SetResultAndFinish(resultIntent))
            } else {
                _pickerResultEvents.send(PickerResultEvent.CancelAndFinish)
            }
        }
    }

    private fun createActionPickResult(uris: List<Uri>, isMultiSelectEnabled: Boolean): Intent? {
        if (uris.isEmpty()) {
            return null
        }

        return Intent().apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (isMultiSelectEnabled) {
                clipData =
                    ClipData.newUri(contentResolver, "uri", uris.first()).apply {
                        uris.drop(1).forEach { addItem(ClipData.Item(it)) }
                    }
            } else {
                data = uris.first()
            }
        }
    }

    private suspend fun createActionPickContactsResult(
        ids: List<Long>,
        queryMode: ContactsQueryMode,
    ): Intent? {
        if (ids.isEmpty()) {
            return null
        }

        return when (queryMode) {
            is ContactsQueryMode.EmailsOnly,
            is ContactsQueryMode.PhonesOnly -> getActionPickContactsIntent(ids)
            is ContactsQueryMode.Custom -> {
                val ids =
                    contactsRepository.getDataRowIds(
                        ids,
                        queryMode.mimetypes,
                        // TODO(b/449960997): Use selected user ID from UserStates
                        UserHandle.myUserId(),
                    )
                getActionPickContactsIntent(ids)
            }

            is ContactsQueryMode.DisplayNamesOnly ->
                throw IllegalStateException("Wrong query mode for ACTION_PICK_CONTACTS")
        }
    }

    private suspend fun getActionPickContactsIntent(dataIds: List<Long>): Intent {
        return Intent().apply {
            data =
                contactsPickerSessionProviderRepository.createSession(
                    dataIds,
                    callingAppUid,
                    // TODO(b/449960997): Use selected user ID from UserStates
                    android.os.UserHandle.myUserId(),
                )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
        try {
            Trace.beginSection("$TAG#searchingContacts")
            val results =
                contactsRepository.searchContacts(
                    query,
                    config.queryMode,
                    // TODO(b/449960997): Use selected user ID from UserStates
                    UserHandle.myUserId(),
                )
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
            Trace.endSection()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "An invalid intent was passed during search.", e)
            _uiState.value = SearchState.Error(e.message ?: "Invalid intent for search.")
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                return
            }
            Log.e(TAG, "An unexpected error occurred during search.", e)
            _uiState.value = SearchState.Error("An unexpected error occurred during search.")
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
                ContactsListState.Success(
                    availableContacts = initialContacts,
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

        cachedStateBeforePreview = currentState

        val (availableContacts, selectedIds, isMultiSelectEnabled) =
            when (currentState) {
                is ContactsListState.Success ->
                    Triple(
                        currentState.availableContacts,
                        currentState.selectedContacts,
                        currentState.isMultiSelectEnabled,
                    )

                is SearchState.Success -> {
                    val aggregatedContacts = currentState.getAggregatedContacts(config.queryMode)
                    Triple(
                        aggregatedContacts,
                        currentState.selectedContacts,
                        config.isMultiSelectEnabled,
                    )
                }
                else -> return
            }

        val previewList =
            availableContacts.filter { contact -> selectedIds.containsKey(contact.id) }

        _uiState.value =
            ContactsPreviewState(
                contactsToDisplay = previewList,
                selectedContacts = selectedIds,
                isMultiSelectEnabled = isMultiSelectEnabled,
            )
    }

    fun onBackFromPreview() {
        val currentState = _uiState.value
        val currentCachedState = cachedStateBeforePreview
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
        cachedStateBeforePreview = null
    }

    // TODO(b/12345678): remove once the permission is pregranted
    open fun onContactsPermissionGranted() {
        val config = pickerConfig ?: return
        loadContactsListData(config)
    }
}

/** Aggregates search results into a list of unique contacts, grouping entries by contact ID. */
// TODO(b/441480198): Add unit tests to verify the correctness of this aggregation.
private fun SearchState.Success.getAggregatedContacts(queryMode: ContactsQueryMode): List<Contact> =
    when (queryMode) {
        ContactsQueryMode.EmailsOnly -> {
            val emailContacts = searchResults.map { it as EmailContact }
            emailContacts
                .groupBy { it.id }
                .values
                .map { contacts ->
                    contacts
                        .first()
                        .copy(emails = contacts.flatMap { it.emails }.distinctBy { it.id })
                }
        }
        ContactsQueryMode.PhonesOnly -> {
            val phoneContacts = searchResults.map { it as PhoneContact }
            phoneContacts
                .groupBy { it.id }
                .values
                .map { contacts ->
                    contacts
                        .first()
                        .copy(phones = contacts.flatMap { it.phones }.distinctBy { it.id })
                }
        }
        ContactsQueryMode.DisplayNamesOnly,
        is ContactsQueryMode.Custom -> searchResults
    }
