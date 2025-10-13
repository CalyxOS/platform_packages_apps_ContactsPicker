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

import android.util.Log
import androidx.collection.buildLongObjectMap
import androidx.collection.longObjectMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.contactspicker.ContactsUiState
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

/**
 * ViewModel for the Contacts Picker screen.
 *
 * This class is responsible for loading and preparing the contacts data to be displayed by the UI.
 */
@HiltViewModel
class ContactsViewModel @Inject constructor(private val contactsRepository: ContactsRepository) :
    ViewModel() {

    companion object {
        private const val TAG = "ContactsViewModel"
    }

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Loading)

    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    /**
     * Toggles the selection state for an entire contact, triggered by tapping the avatar.
     * - For DisplayNameContact, it toggles its single selection state.
     * - For contacts with entries, it selects all entries if not all are already selected, or
     *   deselects all if they are.
     */
    fun toggleContactSelection(contact: Contact) {
        _uiState.update { currentState ->
            if (currentState !is ContactsUiState.Success) return@update currentState

            val existingEntryIds = currentState.selectedContacts[contact.id] ?: emptySet()
            val newContacts = buildLongObjectMap {
                putAll(currentState.selectedContacts)
                if (contact.isFullySelected(existingEntryIds)) {
                    remove(contact.id)
                } else {
                    val allEntryIds =
                        when (contact) {
                            is DisplayNameContact -> setOf(contact.id)
                            is EmailContact -> contact.emails.map { it.id }.toSet()
                            is PhoneContact -> contact.phones.map { it.id }.toSet()
                        }
                    put(contact.id, allEntryIds)
                }
            }

            // Return a copy of the state object to trigger UI recomposition.
            currentState.copy(selectedContacts = newContacts)
        }
    }

    /**
     * Toggles the selection state for a single contact entry (e.g., one email or one phone number).
     *
     * If the entry is already selected, it will be deselected. If it is not selected, it will be
     * added to the selection. If this action results in no entries being selected for a contact,
     * the contact's ID is completely removed from the selection map.
     */
    fun toggleEntrySelection(contactId: Long, entryId: Long) {
        _uiState.update { currentState ->
            if (currentState !is ContactsUiState.Success) return@update currentState

            val alreadySelectedEntries = currentState.selectedContacts[contactId] ?: emptySet()

            // Toggle the presence of the entryId in the set
            val newEntryIds =
                if (entryId in alreadySelectedEntries) {
                    alreadySelectedEntries - entryId
                } else {
                    alreadySelectedEntries + entryId
                }

            // If the resulting set is empty, remove the contact's entry from the map.
            // Otherwise, update the map with the new set of entry IDs.
            val newSelection = buildLongObjectMap {
                putAll(currentState.selectedContacts)
                if (newEntryIds.isEmpty()) {
                    remove(contactId)
                } else {
                    put(contactId, newEntryIds)
                }
            }
            currentState.copy(selectedContacts = newSelection)
        }
    }

    /** Clears all currently selected contacts. */
    fun clearSelection() {
        _uiState.update { currentState ->
            if (currentState !is ContactsUiState.Success) return@update currentState
            currentState.copy(selectedContacts = longObjectMapOf())
        }
    }

    /**
     * Determines the display mode based on the intent. Should only be called from the Activity to
     * trigger the ViewModel's logic, as it changes the [ContactsUiState].
     */
    fun processIntent(intentAction: String?, intentType: String?) {
        viewModelScope.launch {
            try {
                val contacts = contactsRepository.getContactsForIntent(intentAction, intentType)
                // TODO(b/444459883): check and handle empty list
                _uiState.value = ContactsUiState.Success(contacts, longObjectMapOf())
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "An invalid intent was passed.", e)
                // TODO(b/444459883): iterate on error handling and error messages
                _uiState.value = ContactsUiState.Error(e.message ?: "Invalid intent.")
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected error occurred.", e)
                _uiState.value = ContactsUiState.Error("An unexpected error occurred.")
            }
        }
    }
}
