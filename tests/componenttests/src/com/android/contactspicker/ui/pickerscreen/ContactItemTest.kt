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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.PhoneContact
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

    private val testPhoneContact =
        PhoneContact(id = 1, displayName = "Alice Wonderland", phone = "111-222-3333")

    private val testEmailContact =
        EmailContact(id = 1, displayName = "Alice Wonderland", email = "alice@wonderland.org")

    @Test
    fun contactItem_inContactMode_showsName() {
        composeTestRule.setContent { ContactItem(contact = testDisplayNameContact) }

        composeTestRule.onNodeWithText(testDisplayNameContact.displayName).assertIsDisplayed()
    }

    @Test
    fun contactItem_inEmailMode_showsNameAndEmailAddress() {
        composeTestRule.setContent { ContactItem(contact = testEmailContact) }

        composeTestRule.onNodeWithText(testEmailContact.displayName).assertIsDisplayed()
        composeTestRule.onNodeWithText(testEmailContact.email).assertIsDisplayed()
    }

    @Test
    fun contactItem_inPhoneMode_showsNameAndPhoneNumber() {
        composeTestRule.setContent { ContactItem(contact = testPhoneContact) }

        composeTestRule.onNodeWithText(testPhoneContact.displayName).assertIsDisplayed()
        composeTestRule.onNodeWithText(testPhoneContact.phone).assertIsDisplayed()
    }
}
