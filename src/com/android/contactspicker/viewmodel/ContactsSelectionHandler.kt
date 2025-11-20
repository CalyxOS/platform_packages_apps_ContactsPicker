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
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.collection.LongObjectMap
import androidx.collection.buildLongObjectMap
import androidx.collection.longObjectMapOf
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.util.totalElementCount
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "ContactsSelectionHandle"

/** Manages the state and logic for selecting contacts and specific contact entries. */
class ContactsSelectionHandler
@AssistedInject
constructor(
    @Assisted private val isMultiSelectEnabled: Boolean,
    @Assisted private val maxSelectionLimit: Int,
    @Assisted private val eventListener: (SnackbarEvent) -> Unit,
) {

    @AssistedFactory
    fun interface Factory {
        fun create(
            isMultiSelectEnabled: Boolean,
            maxSelectionLimit: Int,
            eventListener: (SnackbarEvent) -> Unit,
        ): ContactsSelectionHandler
    }

    private val _selectedContacts = MutableStateFlow<LongObjectMap<Set<Long>>>(longObjectMapOf())
    val selectedContacts: StateFlow<LongObjectMap<Set<Long>>> = _selectedContacts.asStateFlow()

    /** Clears all currently selected contacts. */
    fun clearSelection() {
        _selectedContacts.value = longObjectMapOf()
    }

    /**
     * Toggles the selection state for an entire contact.
     *
     * If [isMultiSelectEnabled] is true, this toggles all entries for the contact. In single-select
     * mode, this selects or deselects the contact, replacing any existing selection. An attempt to
     * select a multi-entry contact in single-select will select only its first entry.
     */
    fun toggleContactSelection(contact: Contact) {
        _selectedContacts.update { currentSelection ->
            val existingEntryIds = currentSelection[contact.id] ?: emptySet()
            val isAlreadyFullySelected = contact.isFullySelected(existingEntryIds)
            val entryIdsForSelection = contact.getEntryIdsForSelection(isMultiSelectEnabled)

            // Check for the selection limit
            val currentSelectionCount = currentSelection.totalElementCount()
            if (!isAlreadyFullySelected && isMultiSelectEnabled) {
                val newEntriesToAdd = entryIdsForSelection.subtract(existingEntryIds).size
                if (currentSelectionCount + newEntriesToAdd > maxSelectionLimit) {
                    eventListener.invoke(SnackbarEvent.ShowSelectionLimitReached(maxSelectionLimit))
                    return@update currentSelection
                }
            }

            buildLongObjectMap {
                if (isMultiSelectEnabled) {
                    putAll(currentSelection)
                    if (isAlreadyFullySelected) {
                        remove(contact.id)
                    } else {
                        put(contact.id, entryIdsForSelection)
                    }
                } else {
                    if (!isAlreadyFullySelected) {
                        put(contact.id, entryIdsForSelection)
                    }
                    // deselecting in single-select, leave an empty map (implicit by creating new
                    // map)
                }
            }
        }
    }

    /**
     * Toggles the selection state for an entire contact.
     *
     * If [isMultiSelectEnabled] is true, this toggles all entries for the contact. In single-select
     * mode, this selects or deselects the contact, replacing any existing selection. An attempt to
     * select a multi-entry contact in single-select will select only its first entry.
     */
    fun toggleEntrySelection(contactId: Long, entryId: Long) {
        _selectedContacts.update { currentSelection ->
            val alreadySelectedEntriesForCurrentContact = currentSelection[contactId] ?: emptySet()
            val isEntryAlreadySelected = entryId in alreadySelectedEntriesForCurrentContact

            // check for the selection limit
            val currentSelectionCount = currentSelection.totalElementCount()
            if (
                !isEntryAlreadySelected &&
                    isMultiSelectEnabled &&
                    currentSelectionCount >= maxSelectionLimit
            ) {
                eventListener.invoke(SnackbarEvent.ShowSelectionLimitReached(maxSelectionLimit))
                return@update currentSelection
            }

            buildLongObjectMap {
                if (isMultiSelectEnabled) {
                    putAll(currentSelection)
                    if (!isEntryAlreadySelected) {
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
                } else {
                    if (!isEntryAlreadySelected) {
                        put(contactId, setOf(entryId))
                    }
                }
            }
        }
    }

    /**
     * Resolves the currently selected IDs into a list of URIs based on the available contacts data.
     */
    fun resolveSelectedUris(availableContacts: List<Contact>): List<Uri> {
        val finalUris = mutableListOf<Uri>()
        val contactsById = availableContacts.associateBy { it.id }
        val currentSelection = _selectedContacts.value

        currentSelection.forEach { contactId, entryIds ->
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
                            ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, entryId)
                        )
                    }
                }
            }
        }
        return finalUris
    }

    /**
     * Helper extension function to get the IDs for given contact and the selection mode. If called
     * in single selection mode for contacts with multiple emails or phones it will return only
     * first element and log a warning.
     */
    private fun Contact.getEntryIdsForSelection(isMultiSelectEnabled: Boolean): Set<Long> =
        when (this) {
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
