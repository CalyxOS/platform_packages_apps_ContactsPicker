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

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.data.model.emptyContactsSelection
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.android.contactspicker.ui.pickerscreen.CONTACTS_LIST_TEST_TAG
import com.android.contactspicker.ui.pickerscreen.CONTACTS_PICKER_SCREEN_LOADING_INDICATOR_TEST_TAG
import com.android.contactspicker.ui.pickerscreen.PRIVACY_BANNER_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsListContentTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun pickerContent_loadingState_showsLoadingIndicator() {
        composeTestRule.setContent {
            ContactsListContent(
                uiState = ContactsListState.Loading,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }
        composeTestRule
            .onNodeWithTag(CONTACTS_PICKER_SCREEN_LOADING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun pickerContent_errorState_showsErrorMessage() {
        val errorMessage = "Test Error Message"
        composeTestRule.setContent {
            ContactsListContent(
                uiState = ContactsListState.Error(errorMessage),
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun pickerContent_successState_showsListAndBanner() {
        val testContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        composeTestRule.setContent {
            ContactsListContent(
                uiState =
                    ContactsListState.Success(
                        availableContacts = listOf(testContact),
                        selectedContacts = emptyContactsSelection(),
                        isMultiSelectEnabled = false,
                        callingAppName = null,
                        requestedMimeTypes = emptyList(),
                        showPrivacyBanner = true,
                        showProfileSwitcher = false,
                    ),
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }
        composeTestRule.onNodeWithTag(CONTACTS_LIST_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(testContact.displayName).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PRIVACY_BANNER_TEST_TAG).assertIsDisplayed()
    }
}
