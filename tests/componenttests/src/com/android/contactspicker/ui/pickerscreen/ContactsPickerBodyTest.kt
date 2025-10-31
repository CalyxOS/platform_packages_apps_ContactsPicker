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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.R
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.android.contactspicker.ui.components.AVATAR_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsPickerBodyTest {

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun contactsList_displaysHeadersAndContacts() {
        val contacts = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST

        composeTestRule.setContent {
            ContactsPickerBody(
                contacts = contacts,
                selectedContacts = longObjectMapOf(),
                isMultiSelectEnabled = false,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                callingAppName = null,
            )
        }

        contacts.forEach { contact ->
            val displayName = contact.displayName
            val initial = displayName.first().toString()
            composeTestRule
                .onNode(hasTestTag(CONTACTS_LIST_SECTION_HEADER_TEST_TAG) and hasText(initial))
                .assertIsDisplayed()

            composeTestRule.onNodeWithText(displayName).assertIsDisplayed()

            composeTestRule
                .onNode(
                    hasTestTag(AVATAR_TEST_TAG) and hasAnyDescendant(hasText(initial)),
                    useUnmergedTree = true,
                )
                .assertIsDisplayed()
        }
    }

    @Test
    fun privacyBanner_isDisplayed() {
        composeTestRule.setContent {
            ContactsPickerBody(
                contacts = listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT),
                selectedContacts = longObjectMapOf(),
                isMultiSelectEnabled = false,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                callingAppName = null,
            )
        }
        composeTestRule.onNode(hasTestTag(PRIVACY_BANNER_TEST_TAG)).assertIsDisplayed()
    }

    @Test
    fun privacyBanner_isNotDisplayed_afterScrollingTheContactList() {
        val contacts = ContactTestDataFactory.createContactList(30)

        composeTestRule.setContent {
            ContactsPickerBody(
                contacts = contacts,
                selectedContacts = longObjectMapOf(),
                isMultiSelectEnabled = false,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                callingAppName = null,
            )
        }

        composeTestRule.onNode(hasTestTag(PRIVACY_BANNER_TEST_TAG)).assertIsDisplayed()

        composeTestRule
            .onNode(hasTestTag(CONTACTS_LIST_TEST_TAG))
            .performScrollToIndex(contacts.size - 1)

        composeTestRule.onNode(hasTestTag(PRIVACY_BANNER_TEST_TAG)).assertDoesNotExist()
    }

    @Test
    fun favoritesSection_appears_whenFavoritesExist() {
        val nonFavContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        val favContact =
            ContactTestDataFactory.createDisplayNameContact(
                id = 111,
                displayName = "Best friend",
                isFavorite = true,
            )
        val contacts = listOf(favContact, nonFavContact)

        composeTestRule.setContent {
            ContactsPickerBody(
                contacts = contacts,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                selectedContacts = longObjectMapOf(),
                isMultiSelectEnabled = false,
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                callingAppName = null,
            )
        }

        val favoritesHeader = context.getString(R.string.contacts_picker_favorites_header)
        composeTestRule.onNodeWithText(favoritesHeader, substring = true).assertExists()

        // favorite contact should appear twice, non-fav only once
        composeTestRule.onAllNodesWithText(favContact.displayName).assertCountEquals(2)
        composeTestRule.onAllNodesWithText(nonFavContact.displayName).assertCountEquals(1)
    }

    @Test
    fun favoritesSection_doesNotExist_whenNoFavoritesExist() {
        composeTestRule.setContent {
            ContactsPickerBody(
                contacts = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                selectedContacts = longObjectMapOf(),
                isMultiSelectEnabled = false,
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                callingAppName = null,
            )
        }

        val favoritesHeader = context.getString(R.string.contacts_picker_favorites_header)
        composeTestRule.onNodeWithText(favoritesHeader).assertDoesNotExist()
    }
}
