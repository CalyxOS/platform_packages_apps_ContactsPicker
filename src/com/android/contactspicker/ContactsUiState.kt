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
package com.android.contactspicker

import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.ContactsSelection
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.MimeType
import com.android.contactspicker.data.model.SectionKey

/** Represents the different states for the Contacts Picker screen. */
sealed interface ContactsUiState

/** Represents the states for when the user is browsing the full list of contacts. */
sealed interface ContactsListState : ContactsUiState {

    /** The state indicating that data is being loaded. */
    data object Loading : ContactsListState

    /**
     * The state representing a successful fetch of the main contacts list.
     *
     * @param availableContactsGroups The contacts to be displayed, grouped by section headers. It
     *   also contains the favorite contacts list, and it is ordered as expected in UI.
     * @param selectedContacts A map representing the current selection, where the key is the
     *   contact ID and the value is a set of selected entry IDs. For a [DisplayNameContact] that
     *   has no entries, its own contact.id is used.
     * @param isMultiSelectEnabled True if multiple contacts can be selected.
     */
    data class Success(
        val availableContactsGroups: Map<SectionKey, List<Contact>>,
        val selectedContacts: ContactsSelection,
        val isMultiSelectEnabled: Boolean,
        val showPrivacyBanner: Boolean,
        val callingAppName: String?,
        val requestedMimeTypes: List<MimeType>,
    ) : ContactsListState

    data class NoResults(val titleText: String, val descriptionText: String? = null) :
        ContactsListState

    /**
     * The state representing an error that occurred while loading the main contacts list.
     *
     * @param message A description of the error.
     */
    data class Error(val message: String) : ContactsListState
}

/** Represents the different states for a search operation. */
sealed interface SearchState : ContactsUiState {

    /**
     * The state representing a successful search.
     *
     * @param query The search query that produced these results.
     * @param searchResults The list of contacts that match the query.
     * @param selectedContacts The current selected contacts.
     */
    data class Success(
        val query: String,
        val searchResults: List<Contact>,
        val selectedContacts: ContactsSelection,
    ) : SearchState

    /**
     * The state representing an error that occurred during the search.
     *
     * @param message A description of the error.
     */
    data class Error(val message: String) : SearchState
}

/**
 * Represents the state for displaying a preview of selected contacts.
 *
 * @property contactsToDisplay The list of contacts to show in the preview.
 * @property selectedContacts The current selected contacts.
 * @param isMultiSelectEnabled True if multiple contacts can be selected.
 */
data class ContactsPreviewState(
    val contactsToDisplay: List<Contact>,
    val selectedContacts: ContactsSelection,
    val isMultiSelectEnabled: Boolean,
) : ContactsUiState

/**
 * Represents the state for displaying privacy details.
 *
 * @param callingAppName The name of the app that opened the picker.
 * @param requestedMimeTypes The list of contacts to show in the preview.
 */
data class PrivacyDetailsState(
    val callingAppName: String?,
    val requestedMimeTypes: List<MimeType>,
) : ContactsUiState
