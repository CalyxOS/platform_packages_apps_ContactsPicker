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
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.R
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsPickerTopBarTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun overflowMenu_isNotVisibleByDefault() {
        composeTestRule.setContent {
            ContactsPickerTopBar(
                uiState = mutableStateOf(ContactsListState.Loading),
                onSearchBarToggled = {},
                onQueryChange = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                onExitSearch = {},
                onShowPrivacyDetailsClick = {},
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.privacy_details_menu_label))
            .assertDoesNotExist()
    }

    @Test
    fun moreVertIcon_isDisplayed() {
        composeTestRule.setContent {
            ContactsPickerTopBar(
                uiState = mutableStateOf(ContactsListState.Loading),
                onSearchBarToggled = {},
                onQueryChange = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                onExitSearch = {},
                onShowPrivacyDetailsClick = {},
            )
        }

        composeTestRule
            .onNodeWithTag(CONTACTS_PICKER_TOP_BAR_MORE_VERTICAL_ICON_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun clickMoreVertIcon_showsOverflowMenu() {
        composeTestRule.setContent {
            ContactsPickerTopBar(
                uiState = mutableStateOf(ContactsListState.Loading),
                onSearchBarToggled = {},
                onQueryChange = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                onExitSearch = {},
                onShowPrivacyDetailsClick = {},
            )
        }

        composeTestRule
            .onNodeWithTag(CONTACTS_PICKER_TOP_BAR_MORE_VERTICAL_ICON_TEST_TAG)
            .performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.privacy_details_menu_label))
            .assertIsDisplayed()
    }

    @Test
    fun clickOverflowMenuItem_invokesCallback() {
        var privacyDetailsClicked = false
        composeTestRule.setContent {
            ContactsPickerTopBar(
                uiState = mutableStateOf(ContactsListState.Loading),
                onSearchBarToggled = {},
                onQueryChange = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                onExitSearch = {},
                onShowPrivacyDetailsClick = { privacyDetailsClicked = true },
            )
        }

        composeTestRule
            .onNodeWithTag(CONTACTS_PICKER_TOP_BAR_MORE_VERTICAL_ICON_TEST_TAG)
            .performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.privacy_details_menu_label))
            .performClick()

        assertThat(privacyDetailsClicked).isTrue()
        composeTestRule
            .onNodeWithText(context.getString(R.string.privacy_details_menu_label))
            .assertDoesNotExist()
    }

    @Test
    fun clickOutsideOverflowMenu_dismissesMenu() {
        composeTestRule.setContent {
            ContactsPickerTopBar(
                uiState = mutableStateOf(ContactsListState.Loading),
                onSearchBarToggled = {},
                onQueryChange = {},
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
                onExitSearch = {},
                onShowPrivacyDetailsClick = {},
            )
        }

        // 1. Find the MoreVert icon button and click it to open the menu.
        val overflowButton =
            composeTestRule.onNodeWithTag(CONTACTS_PICKER_TOP_BAR_MORE_VERTICAL_ICON_TEST_TAG)
        overflowButton.performClick()

        // 2. Assert that the menu is currently displayed by checking for one of its items.
        val privacyMenuItem =
            composeTestRule.onNodeWithText(context.getString(R.string.privacy_details_menu_label))
        privacyMenuItem.assertIsDisplayed()
        // 3. Click on the scrim/outside area by selecting the second root node.
        //    The DropdownMenu creates a new root node for its popup window.
        // TODO(b/461921427) : Find a more robust way to click the scrim instead of relying on the
        // root node index
        composeTestRule.onAllNodes(isRoot())[1].performClick()
        privacyMenuItem.assertDoesNotExist()
        overflowButton.assertExists()
    }
}
