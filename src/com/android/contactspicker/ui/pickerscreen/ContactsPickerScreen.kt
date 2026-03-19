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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.ContactsPreviewState
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.PrivacyDetailsState
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.PickerUserState
import com.android.contactspicker.data.model.SelectionSource
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.ui.components.ContactsListContent
import com.android.contactspicker.ui.privacydetails.PrivacyDetailsScreen

internal const val CONTACTS_PICKER_SCREEN_LOADING_INDICATOR_TEST_TAG =
    "contacts_picker_screen_loading_indicator"
internal const val CONTACTS_PICKER_SCREEN_TEST_TAG = "contacts_picker_screen"

@Composable
fun ContactsPickerScreen(
    modifier: Modifier,
    uiState: State<ContactsUiState>,
    userState: PickerUserState,
    onToggleContactSelection: (Contact, SelectionSource) -> Unit,
    onToggleEntrySelection: (Long, Long, SelectionSource) -> Unit,
    onPrivacyDetailsBannerClicked: () -> Unit,
    onPrivacyDetailsOverflowMenuClicked: () -> Unit,
    onBackFromPrivacyDetails: () -> Unit,
    onPrivacyBannerDismissRequest: () -> Unit,
    onExpandRequest: () -> Unit,
    onQueryChange: (String) -> Unit,
    onExitSearch: () -> Unit,
    onBackFromPreview: () -> Unit,
    onProfileClicked: (UserProfile) -> Unit,
    onDismissProfileBlockedDialog: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().testTag(CONTACTS_PICKER_SCREEN_TEST_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val uiStateValue = uiState.value) {
            is ContactsPreviewState -> {
                PreviewScreen(
                    onBackPressed = onBackFromPreview,
                    uiState = uiStateValue,
                    onToggleContactSelection = onToggleContactSelection,
                    onToggleEntrySelection = onToggleEntrySelection,
                )
            }

            is PrivacyDetailsState -> {
                PrivacyDetailsScreen(
                    onBackPressed = onBackFromPrivacyDetails,
                    uiState = uiStateValue,
                )
            }

            else -> {
                ContactsPickerTopBar(
                    uiState = uiState,
                    userState = userState,
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
                    onPrivacyDetailsOverflowMenuClicked = onPrivacyDetailsOverflowMenuClicked,
                    onProfileClicked = onProfileClicked,
                )

                if (uiStateValue is ContactsListState) {
                    ContactsListContent(
                        uiState = uiStateValue,
                        onPrivacyBannerMoreDetails = onPrivacyDetailsBannerClicked,
                        onPrivacyBannerDismissRequest = onPrivacyBannerDismissRequest,
                        onToggleContactSelection = onToggleContactSelection,
                        onToggleEntrySelection = onToggleEntrySelection,
                    )
                }

                if (
                    userState is PickerUserState.Success &&
                        userState.profileBlockedDialogData != null
                ) {
                    ProfileBlockedDialog(
                        data = userState.profileBlockedDialogData,
                        onDismissRequest = onDismissProfileBlockedDialog,
                    )
                }
            }
        }
    }
}
