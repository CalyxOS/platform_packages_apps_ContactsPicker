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
import com.android.contactspicker.contact.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for the Contacts Picker screen.
 *
 * This class is responsible for fetching and preparing the contacts data to be displayed by the UI.
 */
class ContactsViewModel : ViewModel() {

    // TODO(b/442966559): change the hardcoded contacts list to contacts from a CP2 query
    private val _contacts =
        MutableStateFlow(
            listOf(
                Contact(1, "Alice Wonderland"),
                Contact(2, "Bob The Builder"),
                Contact(3, "Charlie Chaplin"),
                Contact(4, "David Copperfield"),
                Contact(5, "Emily Dickinson"),
                Contact(6, "Frank Sinatra"),
                Contact(7, "Grace Hopper"),
                Contact(8, "Henry Ford"),
                Contact(9, "Ivy Lee"),
                Contact(10, "Jack London"),
            )
        )

    val contacts: StateFlow<List<Contact>> = _contacts
}
