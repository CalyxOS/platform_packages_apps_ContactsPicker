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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.contactspicker.ContactsPreviewState
import com.android.contactspicker.R
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.ui.components.TitleTopBar

internal const val PREVIEW_SCREEN_TEST_TAG = "preview_screen"
private val TOPBAR_PADDING = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
private val PREVIEW_BODY_PADDING = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
internal const val PREVIEW_SCREEN_TOP_BAR_TEST_TAG = "preview_screen_top_bar"

/**
 * A screen that displays a preview of the selected contacts.
 *
 * This screen is shown after the user has made a selection and before the selection is returned to
 * the calling app. It allows the user to review their selection and either confirm or cancel.
 *
 * @param uiState The [ContactsPreviewState] to display.
 * @param onBackPressed The callback to be invoked when the user presses the back button.
 * @param onToggleContactSelection The callback to be invoked when a contact's selection state is
 *   toggled.
 * @param onToggleEntrySelection The callback to be invoked when a specific entry within a contact
 *   is toggled.
 */
@Composable
fun PreviewScreen(
    uiState: ContactsPreviewState,
    onBackPressed: () -> Unit,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (Long, Long) -> Unit,
) {
    val selectedContacts = uiState.selectedContacts
    val contactsToDisplay = uiState.contactsToDisplay

    Column(modifier = Modifier.fillMaxSize().testTag(PREVIEW_SCREEN_TEST_TAG)) {
        BackHandler(onBack = onBackPressed)
        TitleTopBar(
            onBackPressed = onBackPressed,
            title = stringResource(id = R.string.preview_screen_title),
            modifier = Modifier.padding(TOPBAR_PADDING).testTag(PREVIEW_SCREEN_TOP_BAR_TEST_TAG),
        )

        LazyColumn(modifier = Modifier.padding(PREVIEW_BODY_PADDING)) {
            items(items = contactsToDisplay, key = { it.id }) { contact ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ContactItem(
                        contact = contact,
                        position = ItemPosition.ONLY,
                        selectedEntries = selectedContacts[contact.id],
                        isMultiSelectEnabled = uiState.isMultiSelectEnabled,
                        onToggleContactSelection = onToggleContactSelection,
                        onToggleEntrySelection = onToggleEntrySelection,
                    )
                }
            }
        }
    }
}
