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

import androidx.lifecycle.ViewModel
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.contact.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for the Contacts Picker screen.
 *
 * This class is responsible for fetching and preparing the contacts data to be displayed by the UI.
 */
@HiltViewModel
class ContactsViewModel @Inject constructor() : ViewModel() {

    // TODO(b/442966559): change the hardcoded contacts list to contacts from a CP2 query
    private val _contacts =
        MutableStateFlow(
            listOf(
                Contact(1, "Alice Wonderland", "123-456-7890", "alice@wonderland.com"),
                Contact(2, "Bob The Builder", "987-654-3210", "bobthebuilder@example.com"),
                Contact(3, "Charlie Chaplin", "555-555-5555", "charlie@chaplin.com"),
                Contact(4, "David Copperfield", "111-222-3333", "david@copperfield.com"),
                Contact(5, "Emily Dickinson", "444-444-4444", "emily@dickinson.com"),
                Contact(6, "Frank Sinatra", "777-777-7777", "frank@sinatra.com"),
                Contact(7, "Grace Hopper", "888-888-8888", "grace@hopper.com"),
                Contact(8, "Henry Ford", "999-999-9999", "henry@ford.com"),
                Contact(9, "Ivy Lee", "333-333-3333", "ivy@lee.com"),
                Contact(10, "Jack London", "666-666-6666", "jack@london.com"),
            )
        )
    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Loading)

    val uiState: StateFlow<ContactsUiState> = _uiState

    /**
     * Determines the display mode based on the intent. Should only be called from the Activity to
     * trigger the ViewModel's logic, as it changes the [ContactsUiState].
     */
    fun processIntent(intentAction: String?, intentType: String?) {
        val mode = DisplayModeResolver.resolve(intentAction, intentType)

        if (mode == null) {
            // TODO(b/444459883): iterate on error handling and error messages
            _uiState.value = ContactsUiState.Error("Invalid intent action or type.")
            return
        }

        _uiState.value = ContactsUiState.Loading
        // TODO(b/442966559): fetch contacts from a CP2 query while the _uiState is set to loading
        _uiState.value = ContactsUiState.Success(mode, _contacts.value)
    }
}
