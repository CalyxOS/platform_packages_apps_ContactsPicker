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
package com.android.contactspicker.ui.pickerscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.ui.components.ContactsPickerContent

internal const val CONTACTS_PICKER_SCREEN_LOADING_INDICATOR_TEST_TAG =
    "contacts_picker_screen_loading_indicator"
internal const val CONTACTS_PICKER_SCREEN_TEST_TAG = "contacts_picker_screen"

@Composable
fun ContactsPickerScreen(
    uiState: State<ContactsUiState>,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (Long, Long) -> Unit,
    onMoreDetails: () -> Unit,
    onExpandRequest: () -> Unit,
) {
    var isSearchBarExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(vertical = 8.dp)
                .testTag(CONTACTS_PICKER_SCREEN_TEST_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContactsPickerTopBar(
            onSearchBarToggled = { isExpanded ->
                if (isExpanded) {
                    onExpandRequest()
                }
                isSearchBarExpanded = isExpanded
            }
        )

        // TODO(b/449172596): Handle dismissal logic of privacy banner
        if (!isSearchBarExpanded) {
            ContactsPickerContent(
                uiState = uiState,
                onPrivacyBannerMoreDetails = onMoreDetails,
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = onToggleContactSelection,
                onToggleEntrySelection = onToggleEntrySelection,
            )
        }
    }
}
