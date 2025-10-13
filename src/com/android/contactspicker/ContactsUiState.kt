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
sealed interface ContactsUiState {
    /** The state indicating that data is being loaded. */
    data object Loading : ContactsUiState

    /**
     * The state representing a successful fetch of contacts.
     *
     * @param availableContacts The complete list of all contacts to be displayed.
     * @param selectedContacts A map representing the current selection, where the key is the
     *   contact ID and the value is a set of selected entry IDs. For a [DisplayNameContact] that
     *   has no entries, its own contact.id is used.
     */
    data class Success(
        val availableContacts: List<Contact>,
        val selectedContacts: LongObjectMap<Set<Long>>,
    ) : ContactsUiState

    /**
     * The state representing an error that occurred while loading contacts.
     *
     * @param message A description of the error.
     */
    data class Error(val message: String) : ContactsUiState
}
