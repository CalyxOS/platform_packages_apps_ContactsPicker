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

/** Defines the possible states for the Contacts Picker screen. */
sealed interface ContactsUiState {
    /** The screen is currently loading data. */
    object Loading : ContactsUiState

    /** An error occurred. */
    data class Error(val message: String) : ContactsUiState

    /**
     * The data was loaded successfully.
     *
     * The UI should inspect the type of contacts in this list (e.g., BasicContact, PhoneContact) to
     * determine how to render them.
     *
     * @param contacts The list of contacts to display.
     */
    data class Success(val contacts: List<Contact>) : ContactsUiState
}
