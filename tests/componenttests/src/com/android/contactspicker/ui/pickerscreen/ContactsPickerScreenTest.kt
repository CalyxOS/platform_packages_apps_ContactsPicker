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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.R
import com.android.contactspicker.SearchState
import com.android.contactspicker.data.model.emptyContactsSelection
import com.android.contactspicker.testdata.ContactTestDataFactory
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

    private val testContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT

    @Test
    fun whenStateIsLoading_showsLoadingIndicator() {

        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState = mutableStateOf(ContactsListState.Loading),
                onNavigateToPrivacyDetails = {},
                onExpandRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                onQueryChange = {},
                onExitSearch = {},
                onBackFromPreview = {},
                onPrivacyBannerDismissRequest = {},
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
                uiState = mutableStateOf(ContactsListState.Error(errorMessage)),
                onNavigateToPrivacyDetails = {},
                onExpandRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                onQueryChange = {},
                onExitSearch = {},
                onBackFromPreview = {},
                onPrivacyBannerDismissRequest = {},
            )
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun whenStateIsSuccess_showsContact() {
        setContentWithDefaultSuccessState()
        composeTestRule.onNodeWithText(testContact.displayName).assertIsDisplayed()
    }

    @Test
    fun whenStateIsSuccess_showsSearchBox() {
        setContentWithDefaultSuccessState()
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
            )
            .assertIsDisplayed()
    }

    @Test
    fun whenStateIsError_showsSearchBox() {
        val errorMessage = "Failed to load contacts."

        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState = mutableStateOf(ContactsListState.Error(errorMessage)),
                onNavigateToPrivacyDetails = {},
                onExpandRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                onQueryChange = {},
                onExitSearch = {},
                onBackFromPreview = {},
                onPrivacyBannerDismissRequest = {},
            )
        }
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
            )
            .assertIsDisplayed()
    }

    @Test
    fun whenStateIsLoading_showsSearchBox() {
        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState = mutableStateOf(ContactsListState.Loading),
                onNavigateToPrivacyDetails = {},
                onExpandRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                onQueryChange = {},
                onExitSearch = {},
                onBackFromPreview = {},
                onPrivacyBannerDismissRequest = {},
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
        setContentWithDefaultSuccessState()

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.profile_switcher_content_description)
            )
            .assertIsDisplayed()
    }

    @Test
    fun whenStateIsSuccess_showsMoreVerticalIconInTopBar() {
        setContentWithDefaultSuccessState()
        composeTestRule
            .onNodeWithTag(CONTACTS_PICKER_TOP_BAR_MORE_VERTICAL_ICON_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun whenSearchBarIsToggledToExpand_invokesOnExpandRequest() {
        val mockOnExpandRequest: () -> Unit = mock()

        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState =
                    mutableStateOf(
                        ContactsListState.Success(
                            availableContacts = emptyList(),
                            selectedContacts = emptyContactsSelection(),
                            isMultiSelectEnabled = false,
                            callingAppName = null,
                            showPrivacyBanner = false,
                            requestedMimeTypes = emptyList(),
                        )
                    ),
                onNavigateToPrivacyDetails = {},
                onExpandRequest = mockOnExpandRequest,
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                onQueryChange = {},
                onExitSearch = {},
                onBackFromPreview = {},
                onPrivacyBannerDismissRequest = {},
            )
        }

        val searchHint = context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
        composeTestRule.onNodeWithText(searchHint).performClick()

        verify(mockOnExpandRequest).invoke()
    }

    @Test
    fun pickerScreen_initialState_showsContactsList() {
        setContentWithDefaultSuccessState()

        // Initially, the contact list should be visible
        composeTestRule.onNodeWithTag(CONTACTS_LIST_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(testContact.displayName).assertIsDisplayed()
    }

    @Test
    fun pickerScreen_clickSearch_hidesContactsList() {
        val uiState =
            mutableStateOf<ContactsUiState>(
                ContactsListState.Success(
                    availableContacts = listOf(testContact),
                    selectedContacts = emptyContactsSelection(),
                    isMultiSelectEnabled = false,
                    callingAppName = null,
                    showPrivacyBanner = false,
                    requestedMimeTypes = emptyList(),
                )
            )
        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState = uiState,
                onNavigateToPrivacyDetails = {},
                onExpandRequest = {
                    uiState.value = SearchState.Success("", emptyList(), emptyContactsSelection())
                },
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                onQueryChange = {},
                onExitSearch = {},
                onBackFromPreview = {},
                onPrivacyBannerDismissRequest = {},
            )
        }

        // Initially, the contact list should be visible
        composeTestRule.onNodeWithTag(CONTACTS_LIST_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(testContact.displayName).assertIsDisplayed()

        // Click the search bar to expand
        val searchHint = context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
        composeTestRule.onNodeWithText(searchHint).performClick()

        // The list should now be hidden
        composeTestRule.onNodeWithTag(CONTACTS_LIST_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText(testContact.displayName).assertDoesNotExist()
    }

    @Test
    fun contactsPickerScreen_hasNoVerticalPadding() {
        setContentWithDefaultSuccessState()

        // The root Column of the screen should not have any vertical padding.
        // This is to verify the fix for the extra space above the search bar.
        composeTestRule
            .onNodeWithTag(CONTACTS_PICKER_SCREEN_TEST_TAG)
            .assertTopPositionInRootIsEqualTo(0.dp)
    }

    private fun setContentWithDefaultSuccessState() {
        composeTestRule.setContent {
            ContactsPickerScreen(
                uiState =
                    mutableStateOf(
                        ContactsListState.Success(
                            availableContacts = listOf(testContact),
                            selectedContacts = emptyContactsSelection(),
                            isMultiSelectEnabled = false,
                            callingAppName = null,
                            showPrivacyBanner = false,
                            requestedMimeTypes = emptyList(),
                        )
                    ),
                onNavigateToPrivacyDetails = {},
                onExpandRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                onQueryChange = {},
                onExitSearch = {},
                onBackFromPreview = {},
                onPrivacyBannerDismissRequest = {},
            )
        }
    }
}
