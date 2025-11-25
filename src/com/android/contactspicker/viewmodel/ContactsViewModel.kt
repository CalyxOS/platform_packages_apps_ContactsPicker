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
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsPickerSessionContract
import android.util.Log
import androidx.annotation.OpenForTesting
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.ContactsPreviewState
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.SearchState
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.data.model.emptyContactsSelection
import com.android.contactspicker.data.repository.ContactsRepository
import com.android.contactspicker.data.repository.PrivacyBannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val privacyBannerRepository: PrivacyBannerRepository,
    private val selectionHandlerFactory: ContactsSelectionHandler.Factory,
) : ViewModel() {

    private val contentResolver = context.contentResolver

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsListState.Loading)
    open val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>()
    open val snackbarEvents: Flow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    private var selectionHandler: ContactsSelectionHandler? = null
    private var selectionCollectorJob: Job? = null

    private var initialContacts: List<Contact> = emptyList()
    private var isMultiSelectEnabled: Boolean = false
    private var maxSelectionLimit: Int = DEFAULT_SELECTION_LIMIT
    private var intentAction: String? = null
    private var intentType: String? = null
    private var callingAppName: String? = null
    private var searchJob: Job? = null
    private var loadContactsJob: Job? = null
    private var cachedStateBeforePreview: ContactsUiState? = null

    private var callingAppUid: Int = -1
    private var requestedMimeTypes: List<String> = emptyList()

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
        this.intentAction = intentAction
        this.intentType = intentType
        this.callingAppName = callingAppName
        this.callingAppUid = callingAppUid
        this.isMultiSelectEnabled =
            intentExtras?.getBoolean(Intent.EXTRA_ALLOW_MULTIPLE, false) ?: false
        try {
            this.requestedMimeTypes = getRequestedMimeTypesForIntent(intentAction, intentType)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "An invalid intent was passed.", e)
            _uiState.value = ContactsListState.Error(e.message ?: "Invalid intent.")
            return
        }
        maxSelectionLimit =
            if (
                isMultiSelectEnabled &&
                    intentExtras?.containsKey(
                        ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT
                    ) == true
            ) {
                val limit =
                    intentExtras.getInt(
                        ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT,
                        DEFAULT_SELECTION_LIMIT,
                    )
                if (limit <= 0) {
                    throw IllegalArgumentException(
                        "Selection limit must be a positive number. Received $limit."
                    )
                }
                if (limit > MAX_ALLOWED_SELECTION_LIMIT) {
                    throw IllegalArgumentException(
                        "Selection limit cannot exceed $MAX_ALLOWED_SELECTION_LIMIT. " +
                            "Received $limit."
                    )
                }
                limit
            } else {
                DEFAULT_SELECTION_LIMIT
            }
        selectionHandler =
            selectionHandlerFactory.create(isMultiSelectEnabled, maxSelectionLimit) { event ->
                viewModelScope.launch { _snackbarEvents.emit(event) }
            }
        startObservingSelection()
        loadContactsListData()
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

    private fun loadContactsListData() {
        loadContactsJob?.cancel()
        loadContactsJob =
            viewModelScope.launch {
                _uiState.value = ContactsListState.Loading
                try {
                    Log.d(TAG, "Loading contacts for action: $intentAction, type: $intentType")
                    loadContactsData()
                    loadPrivacyBannerState()

                    // TODO(b/444459883): check and handle empty list
                    _uiState.value =
                        ContactsListState.Success(
                            availableContacts = initialContacts,
                            selectedContacts =
                                checkNotNull(selectionHandler).selectedContacts.value,
                            isMultiSelectEnabled = isMultiSelectEnabled,
                            callingAppName = callingAppName,
                            requestedMimeTypes = requestedMimeTypes,
                            showPrivacyBanner = showPrivacyBanner,
                        )
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "An invalid intent was passed.", e)
                    // TODO(b/444459883): iterate on error handling and error messages
                    _uiState.value = ContactsListState.Error(e.message ?: "Invalid intent.")
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) {
                        Log.i(TAG, "Contacts loading cancelled.")
                        return@launch
                    }
                    Log.e(TAG, "An unexpected error occurred during load.", e)
                    _uiState.value = ContactsListState.Error("An unexpected error occurred.")
                }
            }
    }

    private suspend fun loadContactsData() {
        Log.d(TAG, "Loading contacts for action: $intentAction, type: $intentType")
        initialContacts = contactsRepository.getContactsForIntent(intentAction, intentType)
    }

    private suspend fun loadPrivacyBannerState() {
        Log.d(
            TAG,
            "Loading privacy banner state for appUid: $callingAppUid, mimeTypes: $requestedMimeTypes",
        )
        // Only show the privacy banner if user hasn't seen it before for this combination of uid
        // and MIME types.
        showPrivacyBanner =
            !privacyBannerRepository.wasPrivacyBannerShown(callingAppUid, requestedMimeTypes)
    }

    @VisibleForTesting
    internal fun getRequestedMimeTypesForIntent(
        intentAction: String?,
        intentType: String?,
    ): List<String> {
        return when (intentAction) {
            Intent.ACTION_PICK ->
                if (intentType != null) listOf(intentType)
                else throw IllegalArgumentException("Unsupported intent type: $intentType")

            else -> throw IllegalArgumentException("Unsupported intent action: $intentAction")
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
     * Converts the current selection map into a final result intent.
     *
     * @return A ready-to-use result [Intent], or null if selection is empty/error occurred.
     * @throws [IllegalStateException] if called in non success ui state.
     */
    @OpenForTesting
    open fun prepareSelectionResult(): Intent? {
        val currentState = _uiState.value
        check(
            currentState is ContactsListState.Success ||
                currentState is SearchState.Success ||
                currentState is ContactsPreviewState
        ) {
            "prepareSelectionResult called while not in a Success state."
        }

        val finalUris = checkNotNull(selectionHandler).resolveSelectedUris(initialContacts)
        if (finalUris.isEmpty()) return null

        return when (intentAction) {
            Intent.ACTION_PICK -> createActionPickResult(finalUris)
            else -> null
        }
    }

    private fun createActionPickResult(uris: List<Uri>): Intent? {
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
        try {
            val results = contactsRepository.searchContacts(query, intentAction, intentType)
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
        }
    }

    /**
     * Reverts the UI state from SearchState back to ContactsListState.Success, preserving the
     * current selection.
     */
    fun exitSearch() {
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
                    isMultiSelectEnabled = isMultiSelectEnabled,
                    callingAppName = callingAppName,
                    requestedMimeTypes = requestedMimeTypes,
                    showPrivacyBanner = showPrivacyBanner,
                )
            } else {
                currentState
            }
        }
    }

    fun onPreviewClicked() {
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
                    val aggregatedContacts = currentState.getAggregatedContacts(intentType)
                    Triple(
                        aggregatedContacts,
                        currentState.selectedContacts,
                        this.isMultiSelectEnabled,
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
        loadContactsListData()
    }
}

/** Aggregates search results into a list of unique contacts, grouping entries by contact ID. */
private fun SearchState.Success.getAggregatedContacts(intentType: String?): List<Contact> {
    return when (intentType) {
        Email.CONTENT_ITEM_TYPE,
        Email.CONTENT_TYPE -> {
            val emailContacts = searchResults as List<EmailContact>
            emailContacts
                .groupBy { it.id }
                .values
                .map { contacts ->
                    contacts
                        .first()
                        .copy(emails = contacts.flatMap { it.emails }.distinctBy { it.id })
                }
        }
        Phone.CONTENT_ITEM_TYPE,
        Phone.CONTENT_TYPE -> {
            val phoneContacts = searchResults as List<PhoneContact>
            phoneContacts
                .groupBy { it.id }
                .values
                .map { contacts ->
                    contacts
                        .first()
                        .copy(phones = contacts.flatMap { it.phones }.distinctBy { it.id })
                }
        }
        Contacts.CONTENT_TYPE,
        Contacts.CONTENT_ITEM_TYPE -> searchResults
        else -> throw IllegalArgumentException("Unsupported intent type: $intentType")
    }
}
