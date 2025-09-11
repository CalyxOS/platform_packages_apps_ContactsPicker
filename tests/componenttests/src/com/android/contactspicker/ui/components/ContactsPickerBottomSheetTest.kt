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

import android.content.Context
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.DisplayMode
import com.android.contactspicker.R
import com.android.contactspicker.contact.Contact
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsPickerBottomSheetTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val testContact =
        Contact(
            id = 1,
            displayName = "Jon Snow",
            phone = "111-222-3333",
            email = "jon.snow@thewall.org",
        )

    @Test
    fun whenStateIsLoading_showsLoadingIndicator() {
        composeTestRule.setContent {
            ContactsPickerBottomSheet(onDismissRequest = {}, uiState = ContactsUiState.Loading)
        }

        composeTestRule.onNodeWithTag(BOTTOM_SHEET_LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun whenStateIsError_showsErrorMessage() {
        val errorMessage = "Failed to load contacts."
        composeTestRule.setContent {
            ContactsPickerBottomSheet(
                onDismissRequest = {},
                uiState = ContactsUiState.Error(errorMessage),
            )
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun whenStateIsSuccess_showsContact() {
        composeTestRule.setContent {
            ContactsPickerBottomSheet(
                onDismissRequest = {},
                uiState =
                    ContactsUiState.Success(
                        displayMode = DisplayMode.CONTACT_SELECTION,
                        contacts = listOf(testContact),
                    ),
            )
        }

        composeTestRule.onNodeWithText(testContact.displayName).assertIsDisplayed()
    }

    @Test
    fun whenStateIsSuccess_showsSearchBox() {
        composeTestRule.setContent {
            ContactsPickerBottomSheet(
                onDismissRequest = {},
                uiState =
                    ContactsUiState.Success(
                        displayMode = DisplayMode.CONTACT_SELECTION,
                        contacts = listOf(testContact),
                    ),
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.top_bar_search_placeholder_hint))
            .assertIsDisplayed()
    }

    @Test
    fun whenStateIsSuccess_showsProfileSelector() {
        composeTestRule.setContent {
            ContactsPickerBottomSheet(
                onDismissRequest = {},
                uiState =
                    ContactsUiState.Success(
                        displayMode = DisplayMode.CONTACT_SELECTION,
                        contacts = listOf(testContact),
                    ),
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
            ContactsPickerBottomSheet(
                onDismissRequest = {},
                uiState =
                    ContactsUiState.Success(
                        displayMode = DisplayMode.CONTACT_SELECTION,
                        contacts = listOf(testContact),
                    ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.privacy_info_content_description)
            )
            .assertIsDisplayed()
    }

    @Test
    fun initialState_peekHeightIsCorrect() {
        composeTestRule.setContent {
            ContactsPickerBottomSheet(
                onDismissRequest = {},
                uiState =
                    ContactsUiState.Success(
                        displayMode = DisplayMode.CONTACT_SELECTION,
                        contacts = listOf(testContact),
                    ),
            )
        }

        val sheetBounds =
            composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).getUnclippedBoundsInRoot()
        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val tolerance = 0.1f // 1% tolerance

        val visibleHeight = rootBounds.height - sheetBounds.top
        val bottomSheetHeightRatio = visibleHeight.value / rootBounds.height.value

        assertThat(bottomSheetHeightRatio).isWithin(tolerance).of(BOTTOM_SHEET_PEEK_HEIGHT_RATIO)
    }

    @Test
    fun whenSheetIsSwipedUp_expands() {
        composeTestRule.setContent {
            ContactsPickerBottomSheet(
                onDismissRequest = {},
                uiState =
                    ContactsUiState.Success(
                        displayMode = DisplayMode.CONTACT_SELECTION,
                        contacts = listOf(testContact),
                    ),
            )
        }

        val sheetNode = composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG)
        val initialBounds = sheetNode.getUnclippedBoundsInRoot()

        sheetNode.performTouchInput { swipeUp() }

        composeTestRule.waitForIdle()
        val expandedBounds = sheetNode.getUnclippedBoundsInRoot()
        sheetNode.assertIsDisplayed()
        assert(expandedBounds.top < initialBounds.top)
    }

    @Test
    fun whenSheetIsSwipedDown_onDismissIsCalled() {
        val mockOnDismissRequest: () -> Unit = mock()

        composeTestRule.setContent {
            ContactsPickerBottomSheet(
                onDismissRequest = mockOnDismissRequest,
                uiState =
                    ContactsUiState.Success(
                        displayMode = DisplayMode.CONTACT_SELECTION,
                        contacts = listOf(testContact),
                    ),
            )
        }

        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()

        verify(mockOnDismissRequest).invoke()
    }
}
