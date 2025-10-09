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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.data.repository.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Contacts Picker screen.
 *
 * This class is responsible for fetching and preparing the contacts data to be displayed by the UI.
 */
@HiltViewModel
class ContactsViewModel @Inject constructor(private val contactsRepository: ContactsRepository) :
    ViewModel() {

    companion object {
        private const val TAG = "ContactsViewModel"
    }

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Loading)

    val uiState: StateFlow<ContactsUiState> = _uiState

    /**
     * Determines the display mode based on the intent. Should only be called from the Activity to
     * trigger the ViewModel's logic, as it changes the [ContactsUiState].
     */
    fun processIntent(intentAction: String?, intentType: String?) {
        viewModelScope.launch {
            try {
                val contacts = contactsRepository.fetchContacts(intentAction, intentType)
                // TODO(b/444459883): check and handle empty list
                _uiState.value = ContactsUiState.Success(contacts)
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
