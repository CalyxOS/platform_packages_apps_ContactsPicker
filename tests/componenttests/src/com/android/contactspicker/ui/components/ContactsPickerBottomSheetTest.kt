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
import androidx.collection.LongObjectMap
import androidx.collection.longObjectMapOf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.android.contactspicker.R
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.ui.theme.ContactsPickerAppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalMaterial3Api::class)
@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsPickerBottomSheetTest {

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private var selectedContacts by mutableStateOf<LongObjectMap<Set<Long>>>(longObjectMapOf())

    private val testContact =
        DisplayNameContact(
            id = 1,
            displayName = "Contacty Contact",
            lookupKey = "contacty_contact_lookup",
        )
    private val testSuccessState =
        ContactsUiState.Success(
            availableContacts = listOf(testContact),
            selectedContacts = selectedContacts,
            isMultiSelectEnabled = false,
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun initialState_peekHeightIsCorrect() {
        setupBottomSheet()

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
        setupBottomSheet()

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
        setupBottomSheet(onDismissRequest = mockOnDismissRequest)

        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()

        verify(mockOnDismissRequest, times(1)).invoke()
    }

    @Test
    fun scrim_isDisplayed_onlyWhenSheetIsVisible() {
        setupBottomSheet()

        composeTestRule.onNodeWithTag(SCRIM_TEST_TAG).assertIsDisplayed()

        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(SCRIM_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun selectionBar_inLoadingState_isNotVisible() {
        setupBottomSheet()
        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )
            .assertDoesNotExist()
    }

    @Test
    fun selectionBar_inSuccessState_isNotInitiallyVisible() {
        setupBottomSheet(uiState = testSuccessState)
        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )
            .assertDoesNotExist()
    }

    @Test
    fun selectionBar_isVisible_whenAContactIsSelected() {
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                ContactsPickerBottomSheet(
                    onDismissRequest = {},
                    uiState =
                        mutableStateOf(
                            ContactsUiState.Success(
                                availableContacts = listOf(testContact),
                                selectedContacts = selectedContacts,
                                isMultiSelectEnabled = false,
                            )
                        ),
                    onToggleEntrySelection = { _, _ -> },
                    onToggleContactSelection = {},
                    onClearSelection = {},
                    onDoneClicked = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )
            .assertDoesNotExist()

        selectedContacts = longObjectMapOf(testContact.id, setOf(testContact.id))

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedContacts.size.toString()).assertIsDisplayed()
    }

    @Test
    fun selectionBar_isNotVisible_afterSelectionIsCleared() {
        selectedContacts = longObjectMapOf(testContact.id, setOf(testContact.id))
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                ContactsPickerBottomSheet(
                    onDismissRequest = {},
                    uiState =
                        mutableStateOf(
                            ContactsUiState.Success(
                                availableContacts = listOf(testContact),
                                selectedContacts = selectedContacts,
                                isMultiSelectEnabled = false,
                            )
                        ),
                    onToggleEntrySelection = { _, _ -> },
                    onToggleContactSelection = {},
                    onClearSelection = {},
                    onDoneClicked = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )
            .assertIsDisplayed()

        selectedContacts = longObjectMapOf()

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )
            .assertDoesNotExist()
    }

    private fun setupBottomSheet(
        onDismissRequest: () -> Unit = {},
        uiState: ContactsUiState = ContactsUiState.Loading,
    ) {
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                ContactsPickerBottomSheet(
                    onDismissRequest = onDismissRequest,
                    uiState = mutableStateOf(uiState),
                    onToggleEntrySelection = { _, _ -> },
                    onToggleContactSelection = {},
                    onClearSelection = {},
                    onDoneClicked = {},
                )
            }
        }
    }
}
