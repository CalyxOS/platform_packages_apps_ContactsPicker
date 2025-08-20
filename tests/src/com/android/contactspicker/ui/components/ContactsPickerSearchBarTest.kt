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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.contactspicker.ContactsPickerActivity
import com.android.contactspicker.R
import org.junit.Rule
import org.junit.Test

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
class ContactsPickerSearchBarTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ContactsPickerActivity>()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun whenSearchBoxCollapsed() {
        val placeholderText =
            composeTestRule.activity.getString(R.string.top_bar_search_placeholder_hint)
        val clearTextContentDescription =
            composeTestRule.activity.getString(
                R.string.top_bar_search_clear_text_content_description
            )

        composeTestRule.onNodeWithContentDescription(placeholderText).assertExists()
        composeTestRule
            .onNodeWithContentDescription(clearTextContentDescription)
            .assertDoesNotExist()
    }

    @Test
    fun whenSearchBoxTapped_searchBoxIsExpanded() {
        val placeholderText =
            composeTestRule.activity.getString(R.string.top_bar_search_placeholder_hint)
        val clearTextContentDescription =
            composeTestRule.activity.getString(
                R.string.top_bar_search_clear_text_content_description
            )

        composeTestRule.onNodeWithText(placeholderText).performClick()

        composeTestRule.onNodeWithContentDescription(clearTextContentDescription).assertExists()
    }

    @Test
    fun whenInputEntered_queryIsDisplayed() {
        val placeholderText =
            composeTestRule.activity.getString(R.string.top_bar_search_placeholder_hint)
        composeTestRule.onNodeWithText(placeholderText).performClick()

        val testQuery = "Test Query"
        composeTestRule.onNodeWithText(placeholderText).performTextInput(testQuery)

        composeTestRule.onNodeWithText(testQuery).assertExists()
    }

    @Test
    fun whenClearButtonIsTapped_queryIsCleared() {
        val placeholderText =
            composeTestRule.activity.getString(R.string.top_bar_search_placeholder_hint)
        val clearTextContentDescription =
            composeTestRule.activity.getString(
                R.string.top_bar_search_clear_text_content_description
            )
        composeTestRule.onNodeWithText(placeholderText).performClick()
        val testQuery = "Test Query"
        composeTestRule.onNodeWithText(placeholderText).performTextInput(testQuery)
        composeTestRule.onNodeWithText(testQuery).assertExists()

        composeTestRule.onNodeWithContentDescription(clearTextContentDescription).performClick()

        composeTestRule.onNodeWithText(testQuery).assertDoesNotExist()
        composeTestRule.onNodeWithText(placeholderText).assertExists()
    }
}
