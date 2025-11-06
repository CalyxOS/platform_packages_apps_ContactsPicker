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

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.annotation.OpenForTesting
import androidx.annotation.VisibleForTesting
import androidx.collection.LongObjectMap
import androidx.collection.buildLongObjectMap
import androidx.collection.longObjectMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.SearchState
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.data.repository.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ContactsViewModel"

/**
 * ViewModel for the Contacts Picker screen.
 *
 * This class is responsible for loading and preparing the contacts data to be displayed by the UI.
 */
@OpenForTesting
@HiltViewModel
open class ContactsViewModel
@Inject
constructor(private val contactsRepository: ContactsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsListState.Loading)
    open val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private var initialContacts: List<Contact> = emptyList()
    private var isMultiSelectEnabled: Boolean = false

    private var requestedMimeTypes: List<String> = emptyList()

    /**
     * Toggles the selection state for an entire contact.
     *
     * If [uiState.isMultiSelectEnabled()] is true, this toggles all entries for the contact. In
     * single-select mode, this selects or deselects the contact, replacing any existing selection.
     * An attempt to select a multi-entry contact in single-select will select only its first entry.
     */
    fun toggleContactSelection(contact: Contact) {
        _uiState.update { currentState ->
            val selectedContacts =
                when (currentState) {
                    is ContactsListState.Success -> currentState.selectedContacts
                    is SearchState.Success -> currentState.selectedContacts
                    else ->
                        return@update currentState // Not in a state where selection can be toggled
                }

            val existingEntryIds = selectedContacts[contact.id] ?: emptySet()
            val isAlreadyFullySelected = contact.isFullySelected(existingEntryIds)

            val newSelection = buildLongObjectMap {
                if (isMultiSelectEnabled) {
                    putAll(selectedContacts)
                    if (isAlreadyFullySelected) {
                        remove(contact.id)
                    } else {
                        put(
                            contact.id,
                            contact.getEntryIdsForSelection(isMultiSelectEnabled = true),
                        )
                    }
                } else {
                    if (!isAlreadyFullySelected) {
                        put(
                            contact.id,
                            contact.getEntryIdsForSelection(isMultiSelectEnabled = false),
                        )
                    }
                    // deselecting in single-select, leave an empty map
                }
            }

            // Return a copy of the state object to trigger UI recomposition.
            when (currentState) {
                is ContactsListState.Success -> currentState.copy(selectedContacts = newSelection)
                is SearchState.Success -> currentState.copy(selectedContacts = newSelection)
                else ->
                    throw IllegalStateException("Cannot toggle selection in state $currentState")
            }
        }
    }

    /**
     * Toggles the selection state for a single contact entry (e.g., one email or one phone number).
     *
     * If the entry is already selected, it will be deselected. If it is not selected, it will be
     * added to the selection. In single-select mode it will replace any previously selected contact
     * or entry and become the sole selected entry.
     *
     * If this action results in no entries being selected for a contact, the contact's ID is
     * completely removed from the selection map.
     */
    fun toggleEntrySelection(contactId: Long, entryId: Long) {
        _uiState.update { currentState ->
            val selectedContacts =
                when (currentState) {
                    is ContactsListState.Success -> currentState.selectedContacts
                    is SearchState.Success -> currentState.selectedContacts
                    else -> return@update currentState
                }

            val alreadySelectedEntriesForCurrentContact = selectedContacts[contactId] ?: emptySet()
            val isEntryAlreadySelected = entryId in alreadySelectedEntriesForCurrentContact

            val newSelection =
                if (isMultiSelectEnabled) {
                    handleMultiSelectEntryToggle(
                        selectedContacts,
                        contactId,
                        entryId,
                        alreadySelectedEntriesForCurrentContact,
                        isEntryAlreadySelected,
                    )
                } else {
                    handleSingleSelectEntryToggle(contactId, entryId, isEntryAlreadySelected)
                }
            // Return a copy of the state object to trigger UI recomposition.
            when (currentState) {
                is ContactsListState.Success -> currentState.copy(selectedContacts = newSelection)
                is SearchState.Success -> currentState.copy(selectedContacts = newSelection)
                else ->
                    throw IllegalStateException(
                        "Cannot toggle entry selection in state $currentState"
                    )
            }
        }
    }

    /**
     * Handles toggling a single entry in multi-select mode.
     *
     * Update the [contactId] mapping to either add or remove [entryId] (if remove, if no entries
     * remain for [contactId], remove the mapping). Add all other contact mappings to the result map
     */
    private fun handleMultiSelectEntryToggle(
        currentSelection: LongObjectMap<Set<Long>>,
        contactId: Long,
        entryId: Long,
        alreadySelectedEntriesForCurrentContact: Set<Long>,
        isCurrentEntryAlreadySelected: Boolean,
    ): LongObjectMap<Set<Long>> = buildLongObjectMap {
        putAll(currentSelection)
        if (!isCurrentEntryAlreadySelected) {
            val newEntryIds = alreadySelectedEntriesForCurrentContact + entryId
            put(contactId, newEntryIds)
        } else {
            val newEntryIds = alreadySelectedEntriesForCurrentContact - entryId
            if (newEntryIds.isEmpty()) {
                remove(contactId)
            } else {
                put(contactId, newEntryIds)
            }
        }
    }

    /**
     * Handles toggling a single entry in single-select mode.
     *
     * Selecting an entry makes it the *only* selected item. Deselecting an entry results in an
     * empty selection.
     */
    private fun handleSingleSelectEntryToggle(
        contactId: Long,
        entryId: Long,
        isEntryAlreadySelected: Boolean,
    ): LongObjectMap<Set<Long>> = buildLongObjectMap {
        if (!isEntryAlreadySelected) {
            put(contactId, setOf(entryId))
        }
    }

    /** Clears all currently selected contacts. */
    fun clearSelection() {
        _uiState.update { currentState ->
            when (currentState) {
                is ContactsListState.Success ->
                    currentState.copy(selectedContacts = longObjectMapOf())
                is SearchState.Success -> currentState.copy(selectedContacts = longObjectMapOf())
                else -> currentState
            }
        }
    }

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
    ) {
        viewModelScope.launch {
            try {
                initialContacts = contactsRepository.getContactsForIntent(intentAction, intentType)
                isMultiSelectEnabled =
                    intentExtras?.getBoolean(Intent.EXTRA_ALLOW_MULTIPLE, false) ?: false
                requestedMimeTypes = getRequestedMimeTypesForIntent(intentAction, intentType)

                // TODO(b/444459883): check and handle empty list
                _uiState.value =
                    ContactsListState.Success(
                        availableContacts = initialContacts,
                        selectedContacts = longObjectMapOf(),
                        isMultiSelectEnabled = isMultiSelectEnabled,
                        callingAppName = callingAppName,
                        requestedMimeTypes = requestedMimeTypes,
                    )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "An invalid intent was passed.", e)
                // TODO(b/444459883): iterate on error handling and error messages
                _uiState.value = ContactsListState.Error(e.message ?: "Invalid intent.")
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected error occurred.", e)
                _uiState.value = ContactsListState.Error("An unexpected error occurred.")
            }
        }
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

    /**
     * Converts the current selection map into a final list of content URIs.
     *
     * @return A list of [Uri]s for the selected items, or an empty list.
     */
    @OpenForTesting
    open fun prepareSelectionResult(): List<Uri> {
        val currentState = _uiState.value
        val (contacts, selectedContacts) =
            when (currentState) {
                is ContactsListState.Success ->
                    Pair(currentState.availableContacts, currentState.selectedContacts)
                is SearchState.Success ->
                    Pair(currentState.searchResults, currentState.selectedContacts)
                else -> {
                    Log.w(TAG, "prepareSelectionResult called while not in a Success state.")
                    return emptyList()
                }
            }

        try {
            val finalUris = mutableListOf<Uri>()
            val contactsById = contacts.associateBy { it.id }

            selectedContacts.forEach { contactId, entryIds ->
                val contact = contactsById[contactId]
                if (contact == null) {
                    Log.w(TAG, "Selected contact with ID $contactId not found in available list.")
                    return@forEach
                }

                when (contact) {
                    is DisplayNameContact -> {
                        finalUris.add(
                            ContactsContract.Contacts.getLookupUri(contact.id, contact.lookupKey)
                        )
                    }
                    is EmailContact,
                    is PhoneContact -> {
                        entryIds.forEach { entryId ->
                            finalUris.add(
                                ContentUris.withAppendedId(
                                    ContactsContract.Data.CONTENT_URI,
                                    entryId,
                                )
                            )
                        }
                    }
                }
            }
            return finalUris
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing selection result", e)
            _uiState.value = ContactsListState.Error("Error preparing result: ${e.message}")
            return emptyList()
        }
    }
}

/**
 * Helper extension function to get the IDs for given contact and the selection mode. If called in
 * single selection mode for contacts with multiple emails or phones it will return only first
 * element and log a warning.
 */
private fun Contact.getEntryIdsForSelection(isMultiSelectEnabled: Boolean): Set<Long> {
    return when (this) {
        is DisplayNameContact -> setOf(id)
        is EmailContact -> {
            if (isMultiSelectEnabled) {
                emails.map { it.id }.toSet()
            } else {
                if (emails.size > 1) {
                    Log.w(
                        TAG,
                        "toggleContactSelection called on multi-email contact in " +
                            "single-select mode. Selecting first email.",
                    )
                }
                setOf(emails.first().id)
            }
        }
        is PhoneContact -> {
            if (isMultiSelectEnabled) {
                phones.map { it.id }.toSet()
            } else {
                if (phones.size > 1) {
                    Log.w(
                        TAG,
                        "toggleContactSelection called on multi-phone contact in " +
                            "single-select mode. Selecting first phone.",
                    )
                }
                setOf(phones.first().id)
            }
        }
    }
}
