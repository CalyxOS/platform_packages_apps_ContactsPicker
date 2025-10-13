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

import android.content.Context
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.collection.longObjectMapOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.R
import com.android.contactspicker.data.model.DisplayNameContact
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsPickerScreenTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val testContact = DisplayNameContact(id = 1, displayName = "Jon Snow")

    @Test
    fun whenStateIsLoading_showsLoadingIndicator() {

        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState = ContactsUiState.Loading,
                onMoreDetails = {},
                onExpandRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }
        composeTestRule
            .onNodeWithTag(CONTACTS_PICKER_SCREEN_LOADING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun whenStateIsError_showsErrorMessage() {
        val errorMessage = "Failed to load contacts."

        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState = ContactsUiState.Error(errorMessage),
                onMoreDetails = {},
                onExpandRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun whenStateIsSuccess_showsContact() {
        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState = ContactsUiState.Success(listOf(testContact), longObjectMapOf()),
                onMoreDetails = {},
                onExpandRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }
        composeTestRule.onNodeWithText(testContact.displayName).assertIsDisplayed()
    }

    @Test
    fun whenStateIsSuccess_showsSearchBox() {
        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState = ContactsUiState.Success(listOf(testContact), longObjectMapOf()),
                onMoreDetails = {},
                onExpandRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
            )
            .assertIsDisplayed()
    }

    @Test
    fun whenStateIsSuccess_showsProfileSelector() {
        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState = ContactsUiState.Success(listOf(testContact), longObjectMapOf()),
                onMoreDetails = {},
                onExpandRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.profile_switcher_content_description)
            )
            .assertIsDisplayed()
    }

    @Test
    fun whenStateIsSuccess_showsPrivacyButton() {
        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState = ContactsUiState.Success(listOf(testContact), longObjectMapOf()),
                onMoreDetails = {},
                onExpandRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }
        composeTestRule
            .onNodeWithTag(CONTACTS_PICKER_TOP_BAR_PRIVACY_ICON_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun whenSearchBarIsToggledToExpand_invokesOnExpandRequest() {
        val mockOnExpandRequest: () -> Unit = mock()

        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState = ContactsUiState.Success(emptyList(), longObjectMapOf()),
                onMoreDetails = {},
                onExpandRequest = mockOnExpandRequest,
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }

        val searchHint = context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
        composeTestRule.onNodeWithText(searchHint).performClick()

        verify(mockOnExpandRequest).invoke()
    }
}
