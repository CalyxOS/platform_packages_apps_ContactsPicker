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

import androidx.collection.LongObjectMap
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact

/** Represents the different states for the Contacts Picker screen. */
sealed interface ContactsUiState

/** Represents the states for when the user is browsing the full list of contacts. */
sealed interface ContactsListState : ContactsUiState {

    /** The state indicating that data is being loaded. */
    data object Loading : ContactsListState

    /**
     * The state representing a successful fetch of the main contacts list.
     *
     * @param availableContacts The complete list of all contacts to be displayed.
     * @param selectedContacts A map representing the current selection, where the key is the
     *   contact ID and the value is a set of selected entry IDs. For a [DisplayNameContact] that
     *   has no entries, its own contact.id is used.
     * @param isMultiSelectEnabled True if multiple contacts can be selected.
     */
    data class Success(
        val availableContacts: List<Contact>,
        val selectedContacts: LongObjectMap<Set<Long>>,
        val isMultiSelectEnabled: Boolean,
        val callingAppName: String?,
        val requestedMimeTypes: List<String>,
    ) : ContactsListState

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
     * @param selectedContacts A map representing the current selection, where the key is the
     *   contact ID and the value is a set of selected entry IDs. For a [DisplayNameContact] that
     *   has no entries, its own contact.id is used.
     */
    data class Success(
        val query: String,
        val searchResults: List<Contact>,
        val selectedContacts: LongObjectMap<Set<Long>>,
    ) : SearchState

    /**
     * The state representing an error that occurred during the search.
     *
     * @param message A description of the error.
     */
    data class Error(val message: String) : SearchState
}
