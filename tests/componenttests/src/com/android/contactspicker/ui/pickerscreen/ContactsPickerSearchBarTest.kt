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

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.SearchState
import com.android.contactspicker.data.model.SelectionSource
import com.android.contactspicker.data.model.emptyContactsSelection
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.android.contactspicker.ui.components.AVATAR_TEST_TAG
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsPickerSearchBarTest {

    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun onToggleContactSelection_fromSearch_passesSearchSource() {
        var toggledSource: SelectionSource? = null
        val contact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT

        // Setup search state with results
        val searchState =
            SearchState.Success(
                query = "test",
                searchResults = listOf(contact),
                selectedContacts = emptyContactsSelection(),
            )

        composeTestRule.setContent {
            ContactsPickerSearchBar(
                modifier = Modifier,
                expanded = true,
                uiState = mutableStateOf(searchState),
                onExpandedChange = {},
                onQueryChange = {},
                onToggleContactSelection = { _, _ -> },
                onToggleEntrySelection = { _, _, source -> toggledSource = source },
                onExitSearch = {},
            )
        }

        // Click the contact in search results
        composeTestRule.onNodeWithText(contact.displayName).performClick()
        val initial = contact.displayName.first()
        composeTestRule
            .onNode(
                hasTestTag(AVATAR_TEST_TAG) and hasAnyDescendant(hasText(initial.toString())),
                useUnmergedTree = true,
            )
            .performClick()

        assertThat(toggledSource).isEqualTo(SelectionSource.SEARCH)
    }
}
