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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.ContactsPreviewState
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.ui.components.ContactsListContent

internal const val CONTACTS_PICKER_SCREEN_LOADING_INDICATOR_TEST_TAG =
    "contacts_picker_screen_loading_indicator"
internal const val CONTACTS_PICKER_SCREEN_TEST_TAG = "contacts_picker_screen"

@Composable
fun ContactsPickerScreen(
    uiState: State<ContactsUiState>,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (Long, Long) -> Unit,
    onNavigateToPrivacyDetails: () -> Unit,
    onPrivacyBannerDismissRequest: () -> Unit,
    onExpandRequest: () -> Unit,
    onQueryChange: (String) -> Unit,
    onExitSearch: () -> Unit,
    onBackFromPreview: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(vertical = 8.dp)
                .testTag(CONTACTS_PICKER_SCREEN_TEST_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val uiStateValue = uiState.value
        if (uiStateValue is ContactsPreviewState) {
            PreviewScreen(
                onBackPressed = onBackFromPreview,
                uiState = uiStateValue,
                onToggleContactSelection = onToggleContactSelection,
                onToggleEntrySelection = onToggleEntrySelection,
            )
        } else {
            ContactsPickerTopBar(
                uiState = uiState,
                onSearchBarToggled = { isExpanded ->
                    if (isExpanded) {
                        onExpandRequest()
                        onQueryChange("")
                    } else {
                        onExitSearch()
                    }
                },
                onQueryChange = onQueryChange,
                onToggleContactSelection = onToggleContactSelection,
                onToggleEntrySelection = onToggleEntrySelection,
                onExitSearch = onExitSearch,
                onShowPrivacyDetailsClick = onNavigateToPrivacyDetails,
            )

            // TODO(b/449172596): Handle dismissal logic of privacy banner

            if (uiStateValue is ContactsListState) {
                ContactsListContent(
                    uiState = uiStateValue,
                    onPrivacyBannerMoreDetails = onNavigateToPrivacyDetails,
                    onPrivacyBannerDismissRequest = onPrivacyBannerDismissRequest,
                    onToggleContactSelection = onToggleContactSelection,
                    onToggleEntrySelection = onToggleEntrySelection,
                )
            }
        }
    }
}
