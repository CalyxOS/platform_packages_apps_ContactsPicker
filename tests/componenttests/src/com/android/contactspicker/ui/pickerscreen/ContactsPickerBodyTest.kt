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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mood
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.R
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.android.contactspicker.ui.components.AVATAR_TEST_TAG
import com.google.common.truth.Truth.assertThat
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
                showPrivacyBanner = false,
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
                showPrivacyBanner = true,
                onToggleEntrySelection = { _, _ -> },
                callingAppName = null,
            )
        }
        composeTestRule.onNode(hasTestTag(PRIVACY_BANNER_TEST_TAG)).assertIsDisplayed()
    }

    @Test
    fun privacyBanner_isNotDisplayed() {
        composeTestRule.setContent {
            ContactsPickerBody(
                contacts = listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT),
                selectedContacts = longObjectMapOf(),
                isMultiSelectEnabled = false,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = {},
                showPrivacyBanner = false,
                onToggleEntrySelection = { _, _ -> },
                callingAppName = null,
            )
        }
        composeTestRule.onNode(hasTestTag(PRIVACY_BANNER_TEST_TAG)).assertIsNotDisplayed()
    }

    @Test
    fun privacyBanner_isNotDisplayed_afterScrollingTheContactList() {
        val contacts = ContactTestDataFactory.createContactList(30)

        setContentWithContactsPickerBody(contacts)

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

        setContentWithContactsPickerBody(listOf(favContact, nonFavContact))

        val favoritesHeader = context.getString(R.string.contacts_picker_favorites_header)
        composeTestRule.onNodeWithText(favoritesHeader, substring = true).assertExists()

        // favorite contact should appear twice, non-fav only once
        composeTestRule.onAllNodesWithText(favContact.displayName).assertCountEquals(2)
        composeTestRule.onAllNodesWithText(nonFavContact.displayName).assertCountEquals(1)
    }

    @Test
    fun favoritesSection_doesNotExist_whenNoFavoritesExist() {
        setContentWithContactsPickerBody(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST)

        val favoritesHeader = context.getString(R.string.contacts_picker_favorites_header)
        composeTestRule.onNodeWithText(favoritesHeader).assertDoesNotExist()
    }

    @Test
    fun contactsPickerBody_withSpecialCharacterDisplayName_displaysEmojiHeader() {
        val regularContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        val contacts =
            listOf(
                // Contacts that should be under an icon
                ContactTestDataFactory.createDisplayNameContact(
                    id = 111,
                    displayName = "#Favorite Contact",
                ),
                // Emoji ":D" for the display name
                ContactTestDataFactory.createDisplayNameContact(
                    id = 123,
                    displayName = "\uD83D\uDE00",
                ),

                // Contact that should be under letters
                regularContact,
            )

        setContentWithContactsPickerBody(contacts)

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.emoji_header_icon_content_description),
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
        composeTestRule
            .onNode(
                hasTestTag(CONTACTS_LIST_SECTION_HEADER_TEST_TAG) and
                    hasText(regularContact.displayName.first().toString())
            )
            .assertIsDisplayed()
    }

    @Test
    fun contactsPickerBody_noSpecialCharacterDisplayName_doesNotDisplayEmojiHeader() {
        setContentWithContactsPickerBody(
            listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT)
        )

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.emoji_header_icon_content_description),
                useUnmergedTree = true,
            )
            .assertDoesNotExist()
    }

    private fun setContentWithContactsPickerBody(contacts: List<Contact>) {
        composeTestRule.setContent {
            ContactsPickerBody(
                contacts = contacts,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                selectedContacts = longObjectMapOf(),
                isMultiSelectEnabled = false,
                showPrivacyBanner = true,
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                callingAppName = null,
            )
        }
    }

    @Test
    fun sectionKey_iconKey_isLessThan_letterKey() {
        val iconKey = SectionKey.IconKey(Icons.Default.Mood, "Content Description")
        val letterKey = SectionKey.LetterKey('A')
        assertThat(iconKey < letterKey).isTrue()
    }

    @Test
    fun sectionKey_letterKeys_areSorted_alphabetically() {
        val letterKey1 = SectionKey.LetterKey('A')
        val letterKey2 = SectionKey.LetterKey('B')
        assertThat(letterKey1 < letterKey2).isTrue()
    }
}
