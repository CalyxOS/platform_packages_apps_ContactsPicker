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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.contactspicker.ContactsUiState

internal const val CONTACTS_PICKER_SCREEN_LOADING_INDICATOR_TEST_TAG =
    "contacts_picker_screen_loading_indicator"
internal const val CONTACTS_PICKER_SCREEN_TEST_TAG = "contacts_picker_screen"

@Composable
fun ContactsPickerScreen(
    uiState: ContactsUiState,
    onMoreDetails: () -> Unit,
    onExpandRequest: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(vertical = 8.dp)
                .testTag(CONTACTS_PICKER_SCREEN_TEST_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val state = uiState) {
            is ContactsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.testTag(CONTACTS_PICKER_SCREEN_LOADING_INDICATOR_TEST_TAG)
                    )
                }
            }

            is ContactsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            is ContactsUiState.Success -> {
                ContactsPickerTopBar(
                    onSearchBarToggled = { isExpanded ->
                        if (isExpanded) {
                            onExpandRequest()
                        }
                    }
                )
                // TODO(b/449172596): Handle dismissal logic of privacy banner

                ContactsPickerBody(
                    contacts = state.contacts,
                    onPrivacyBannerMoreDetails = onMoreDetails,
                    onPrivacyBannerDismissRequest = {},
                )
            }
        }
    }
}
