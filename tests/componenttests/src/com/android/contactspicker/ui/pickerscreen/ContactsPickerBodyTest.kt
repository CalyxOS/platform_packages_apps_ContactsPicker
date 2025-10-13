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
import androidx.collection.longObjectMapOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.ui.components.AVATAR_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsPickerBodyTest {

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun contactsList_displaysHeadersAndContacts() {
        val contacts =
            listOf(
                DisplayNameContact(id = 1L, displayName = "Alpha"),
                DisplayNameContact(id = 3L, displayName = "Beta"),
                DisplayNameContact(id = 2L, displayName = "Gamma"),
            )

        composeTestRule.setContent {
            ContactsPickerBody(
                contacts = contacts,
                selectedContacts = longObjectMapOf(),
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }

        composeTestRule
            .onNode(hasTestTag(CONTACTS_LIST_SECTION_HEADER_TEST_TAG) and hasText("A"))
            .assertIsDisplayed()
        composeTestRule
            .onNode(hasTestTag(CONTACTS_LIST_SECTION_HEADER_TEST_TAG) and hasText("B"))
            .assertIsDisplayed()
        composeTestRule
            .onNode(hasTestTag(CONTACTS_LIST_SECTION_HEADER_TEST_TAG) and hasText("G"))
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("Beta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gamma").assertIsDisplayed()

        composeTestRule
            .onNode(
                hasTestTag(AVATAR_TEST_TAG) and hasAnyDescendant(hasText("A")),
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
        composeTestRule
            .onNode(
                hasTestTag(AVATAR_TEST_TAG) and hasAnyDescendant(hasText("B")),
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
        composeTestRule
            .onNode(
                hasTestTag(AVATAR_TEST_TAG) and hasAnyDescendant(hasText("G")),
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
    }

    @Test
    fun privacyBanner_isDisplayed() {
        composeTestRule.setContent {
            ContactsPickerBody(
                contacts = listOf(DisplayNameContact(id = 1L, displayName = "Alpha")),
                selectedContacts = longObjectMapOf(),
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }
        composeTestRule.onNode(hasTestTag(PRIVACY_BANNER_TEST_TAG)).assertIsDisplayed()
    }

    @Test
    fun privacyBanner_isNotDisplayed_afterScrollingTheContactList() {
        val contacts =
            List(30) { i -> DisplayNameContact(id = i.toLong(), displayName = "Contact $i") }

        composeTestRule.setContent {
            ContactsPickerBody(
                contacts = contacts,
                selectedContacts = longObjectMapOf(),
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }

        composeTestRule.onNode(hasTestTag(PRIVACY_BANNER_TEST_TAG)).assertIsDisplayed()

        composeTestRule
            .onNode(hasTestTag(CONTACTS_LIST_TEST_TAG))
            .performScrollToIndex(contacts.size - 1)

        composeTestRule.onNode(hasTestTag(PRIVACY_BANNER_TEST_TAG)).assertDoesNotExist()
    }
}
