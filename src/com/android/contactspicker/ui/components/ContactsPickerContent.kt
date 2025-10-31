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

package com.android.contactspicker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.SearchState
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.ui.pickerscreen.CONTACTS_PICKER_SCREEN_LOADING_INDICATOR_TEST_TAG
import com.android.contactspicker.ui.pickerscreen.ContactsPickerBody

/**
 * Displays the content of the Contacts Picker based on the current [ContactsUiState].
 *
 * This component is used as the main browsing screen for the contacts list and the contacts search
 * results screen, to provide a consistent way of handling loading, error, and success states before
 * displaying the core contact list.
 */
@Composable
fun ContactsPickerContent(
    uiState: State<ContactsUiState>,
    onPrivacyBannerMoreDetails: () -> Unit,
    onPrivacyBannerDismissRequest: () -> Unit,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (contactId: Long, entryId: Long) -> Unit,
) {
    val uiStateValue = uiState.value
    when (uiStateValue) {
        is ContactsListState ->
            ContactsListContent(
                uiState = uiStateValue,
                onPrivacyBannerMoreDetails = onPrivacyBannerMoreDetails,
                onPrivacyBannerDismissRequest = onPrivacyBannerDismissRequest,
                onToggleContactSelection = onToggleContactSelection,
                onToggleEntrySelection = onToggleEntrySelection,
            )

        is SearchState -> {
            // TODO(b/443023416): Implement search states UI
        }
    }
}

@Composable
private fun ContactsListContent(
    uiState: ContactsListState,
    onPrivacyBannerMoreDetails: () -> Unit,
    onPrivacyBannerDismissRequest: () -> Unit,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (contactId: Long, entryId: Long) -> Unit,
) {
    when (uiState) {
        is ContactsListState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(CONTACTS_PICKER_SCREEN_LOADING_INDICATOR_TEST_TAG)
                )
            }
        }

        is ContactsListState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        is ContactsListState.Success -> {
            ContactsPickerBody(
                contacts = uiState.availableContacts,
                callingAppName = uiState.callingAppName,
                selectedContacts = uiState.selectedContacts,
                isMultiSelectEnabled = uiState.isMultiSelectEnabled,
                onToggleContactSelection = onToggleContactSelection,
                onToggleEntrySelection = onToggleEntrySelection,
                onPrivacyBannerMoreDetails = onPrivacyBannerMoreDetails,
                onPrivacyBannerDismissRequest = onPrivacyBannerDismissRequest,
            )
        }
    }
}
