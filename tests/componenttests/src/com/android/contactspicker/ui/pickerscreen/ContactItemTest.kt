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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.R
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.EmailEntry
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.data.model.PhoneEntry
import com.android.contactspicker.ui.components.AVATAR_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactItemTest {

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val testDisplayNameContact =
        DisplayNameContact(id = 1, displayName = "Alice Wonderland")

    private val testSinglePhoneContact =
        PhoneContact(
            id = 1,
            displayName = "Alice Wonderland",
            phones = listOf(PhoneEntry(id = 10L, number = "111-222-3333", label = "Mobile")),
        )

    private val testSingleEmailContact =
        EmailContact(
            id = 1,
            displayName = "Alice Wonderland",
            emails = listOf(EmailEntry(id = 11L, address = "alice@wonderland.org", label = "Home")),
        )

    private val testMultiPhoneContact =
        PhoneContact(
            id = 1,
            displayName = "Bob The Builder",
            phones =
                listOf(
                    PhoneEntry(id = 12L, number = "111-222-3333", label = "Mobile"),
                    PhoneEntry(id = 13L, number = "444-555-6666", label = "Work"),
                ),
        )

    private val testMultiEmailContact =
        EmailContact(
            id = 1,
            displayName = "Charlie Chaplin",
            emails =
                listOf(
                    EmailEntry(id = 14L, address = "charlie@chaplin.org", label = "Home"),
                    EmailEntry(id = 15L, address = "cc@hollywood.com", label = "Work"),
                ),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun contactItem_withDisplayNameContact_showsNameOnly() {
        composeTestRule.setContent { ContactItem(contact = testDisplayNameContact) }

        composeTestRule.onNodeWithText(testDisplayNameContact.displayName).assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.contact_item_expand_button_content_description)
            )
            .assertDoesNotExist()
    }

    @Test
    fun contactItem_withSingleEmail_showsNameAndEmail() {
        composeTestRule.setContent { ContactItem(contact = testSingleEmailContact) }

        composeTestRule.onNodeWithText(testSingleEmailContact.displayName).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(testSingleEmailContact.emails.first().address)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.contact_item_expand_button_content_description)
            )
            .assertDoesNotExist()
    }

    @Test
    fun contactItem_withSinglePhone_showsNameAndPhoneNumber() {
        composeTestRule.setContent { ContactItem(contact = testSinglePhoneContact) }

        composeTestRule.onNodeWithText(testSinglePhoneContact.displayName).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(testSinglePhoneContact.phones.first().number)
            .assertIsDisplayed()
        // The content description for the expand button should not exist.
        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.contact_item_expand_button_content_description)
            )
            .assertDoesNotExist()
    }

    @Test
    fun contactItem_withMultipleEmails_showsEmailCountAndIsExpandable() {
        composeTestRule.setContent { ContactItem(contact = testMultiEmailContact) }

        composeTestRule.onNodeWithText(testMultiEmailContact.displayName).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                context.getString(
                    R.string.contact_item_emails_count,
                    testMultiEmailContact.emails.size,
                )
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.contact_item_expand_button_content_description)
            )
            .assertIsDisplayed()
        // Emails should not be visible initially
        composeTestRule
            .onNodeWithText(testMultiEmailContact.emails.first().address)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(testMultiEmailContact.emails.last().address)
            .assertDoesNotExist()
    }

    @Test
    fun contactItem_withMultiplePhones_showsPhoneCountAndIsExpandable() {
        composeTestRule.setContent { ContactItem(contact = testMultiPhoneContact) }

        composeTestRule.onNodeWithText(testMultiPhoneContact.displayName).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                context.getString(
                    R.string.contact_item_phones_count,
                    testMultiPhoneContact.phones.size,
                )
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.contact_item_expand_button_content_description)
            )
            .assertIsDisplayed()
        // Phones should not be visible initially
        composeTestRule
            .onNodeWithText(testMultiPhoneContact.phones.first().number)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(testMultiPhoneContact.phones.last().number)
            .assertDoesNotExist()
    }

    @Test
    fun contactItem_withMultipleEmails_expandsAndCollapsesOnClick() {
        composeTestRule.setContent { ContactItem(contact = testMultiEmailContact) }

        // Expand
        composeTestRule.onNodeWithText(testMultiEmailContact.displayName).performClick()

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.contact_item_collapse_button_content_description)
            )
            .assertIsDisplayed()
        testMultiEmailContact.emails.forEach { email ->
            composeTestRule.onNodeWithText(email.address).assertIsDisplayed()
            composeTestRule.onNodeWithText(email.label).assertIsDisplayed()
        }

        // Collapse
        composeTestRule.onNodeWithText(testMultiEmailContact.displayName).performClick()

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.contact_item_expand_button_content_description)
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(testMultiEmailContact.emails.first().address)
            .assertDoesNotExist()
    }

    @Test
    fun contactItem_withMultiplePhones_expandsAndCollapsesOnClick() {
        composeTestRule.setContent { ContactItem(contact = testMultiPhoneContact) }

        // Expand
        composeTestRule.onNodeWithText(testMultiPhoneContact.displayName).performClick()

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.contact_item_collapse_button_content_description)
            )
            .assertIsDisplayed()
        testMultiPhoneContact.phones.forEach { phone ->
            composeTestRule.onNodeWithText(phone.number).assertIsDisplayed()
            composeTestRule.onNodeWithText(phone.label).assertIsDisplayed()
        }

        // Collapse
        composeTestRule.onNodeWithText(testMultiPhoneContact.displayName).performClick()

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.contact_item_expand_button_content_description)
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(testMultiPhoneContact.phones.first().number)
            .assertDoesNotExist()
    }

    @Test
    fun contactItem_avatarForAlice_displaysInitialA() {
        composeTestRule.setContent { ContactItem(contact = testDisplayNameContact) }

        composeTestRule
            .onNode(hasTestTag(AVATAR_TEST_TAG) and hasAnyDescendant(hasText("A")))
            .assertIsDisplayed()
    }

    @Test
    fun expandedItem_canToggleCheckbox() {
        composeTestRule.setContent { ContactItem(contact = testMultiEmailContact) }

        // Expand the item
        composeTestRule.onNodeWithText(testMultiEmailContact.displayName).performClick()

        val firstEmail = testMultiEmailContact.emails.first().address
        // useUnmergedTree = true is needed because the Row containing the checkbox and text
        // might merge their semantics, making the checkbox difficult to find.
        val checkbox =
            composeTestRule.onNode(
                hasAnySibling(hasText(firstEmail)) and isToggleable(),
                useUnmergedTree = true,
            )

        checkbox.assertIsOff()
        checkbox.performClick()
        checkbox.assertIsOn()
    }
}
