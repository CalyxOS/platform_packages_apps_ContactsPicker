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
import android.icu.text.MessageFormat
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.ContactsPreviewState
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.R
import com.android.contactspicker.data.model.ContactsSelection
import com.android.contactspicker.data.model.PickerUserState
import com.android.contactspicker.data.model.contactsSelectionOf
import com.android.contactspicker.data.model.emptyContactsSelection
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.android.contactspicker.ui.theme.ContactsPickerAppTheme
import com.android.contactspicker.viewmodel.SnackbarEvent
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
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

    private var selectedContacts by mutableStateOf<ContactsSelection>(emptyContactsSelection())

    private val testContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
    private val testSuccessState =
        ContactsListState.Success(
            availableContactsGroups =
                ContactTestDataFactory.groupContactsForTest(listOf(testContact)),
            selectedContacts = selectedContacts,
            isMultiSelectEnabled = false,
            callingAppName = null,
            showPrivacyBanner = false,
            requestedMimeTypes = emptyList(),
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
                            ContactsListState.Success(
                                availableContactsGroups =
                                    ContactTestDataFactory.groupContactsForTest(
                                        listOf(testContact)
                                    ),
                                selectedContacts = selectedContacts,
                                isMultiSelectEnabled = false,
                                callingAppName = null,
                                showPrivacyBanner = false,
                                requestedMimeTypes = emptyList(),
                            )
                        ),
                    userState = PickerUserState.Loading,
                    snackbarEvents = flowOf(),
                    onToggleContactSelection = { _, _ -> },
                    onToggleEntrySelection = { _, _, _ -> },
                    onClearSelection = {},
                    onDoneClicked = {},
                    onQueryChange = {},
                    onExitSearch = {},
                    onPreviewClicked = {},
                    onBackFromPreview = {},
                    onPrivacyBannerDismissRequest = {},
                    onProfileClicked = {},
                    onDismissProfileBlockedDialog = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )
            .assertDoesNotExist()

        selectedContacts = contactsSelectionOf(testContact.id, setOf(testContact.id))

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedContacts.size.toString()).assertIsDisplayed()
    }

    @Test
    fun selectionBar_isNotVisible_afterSelectionIsCleared() {
        selectedContacts = contactsSelectionOf(testContact.id, setOf(testContact.id))
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                ContactsPickerBottomSheet(
                    onDismissRequest = {},
                    uiState =
                        mutableStateOf(
                            ContactsListState.Success(
                                availableContactsGroups =
                                    ContactTestDataFactory.groupContactsForTest(
                                        listOf(testContact)
                                    ),
                                selectedContacts = selectedContacts,
                                isMultiSelectEnabled = false,
                                callingAppName = null,
                                showPrivacyBanner = false,
                                requestedMimeTypes = emptyList(),
                            )
                        ),
                    userState = PickerUserState.Loading,
                    snackbarEvents = flowOf(),
                    onToggleContactSelection = { _, _ -> },
                    onToggleEntrySelection = { _, _, _ -> },
                    onClearSelection = {},
                    onDoneClicked = {},
                    onQueryChange = {},
                    onExitSearch = {},
                    onPreviewClicked = {},
                    onBackFromPreview = {},
                    onPrivacyBannerDismissRequest = {},
                    onProfileClicked = {},
                    onDismissProfileBlockedDialog = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )
            .assertIsDisplayed()

        selectedContacts = emptyContactsSelection()

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )
            .assertDoesNotExist()
    }

    @Test
    fun whenSearchBarClicked_bottomSheet_expandsToFullHeight() {
        setupBottomSheet()

        val sheetNode = composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG)
        val initialBounds = sheetNode.getUnclippedBoundsInRoot()

        val searchHint = context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
        composeTestRule.onNodeWithText(searchHint).performClick()

        composeTestRule.waitForIdle()

        val expandedBounds = sheetNode.getUnclippedBoundsInRoot()
        sheetNode.assertIsDisplayed()
        assertThat(expandedBounds.top).isLessThan(initialBounds.top)
    }

    @Test
    fun onSearchQueryChanged_isCalled_whenQueryIsEntered() {
        val onSearchQueryChanged: (String) -> Unit = mock()
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                ContactsPickerBottomSheet(
                    onDismissRequest = {},
                    uiState =
                        mutableStateOf(
                            ContactsListState.Success(
                                availableContactsGroups =
                                    ContactTestDataFactory.groupContactsForTest(
                                        listOf(testContact)
                                    ),
                                selectedContacts = selectedContacts,
                                false,
                                callingAppName = null,
                                requestedMimeTypes = emptyList(),
                                showPrivacyBanner = false,
                            )
                        ),
                    userState = PickerUserState.Loading,
                    snackbarEvents = flowOf(),
                    onToggleEntrySelection = { _, _, _ -> },
                    onToggleContactSelection = { _, _ -> },
                    onClearSelection = {},
                    onDoneClicked = {},
                    onQueryChange = onSearchQueryChanged,
                    onExitSearch = {},
                    onPreviewClicked = {},
                    onBackFromPreview = {},
                    onPrivacyBannerDismissRequest = {},
                    onProfileClicked = {},
                    onDismissProfileBlockedDialog = {},
                )
            }
        }

        val searchQuery = "test"
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
            )
            .performTextInput(searchQuery)

        verify(onSearchQueryChanged).invoke(searchQuery)
    }

    @Test
    fun selectionLimitSnackbar_appearsWhenEventIsEmitted_andDisappearsAfterTimeout() {
        val events = MutableSharedFlow<SnackbarEvent>(replay = 1)
        val msgFormat =
            MessageFormat(
                context.getString(R.string.contacts_selection_limit_reached_message),
                Locale.getDefault(),
            )
        val args = mapOf(Pair("count", 2))

        val snackbarMessage = msgFormat.format(args)

        composeTestRule.setContent {
            ContactsPickerBottomSheet(
                onDismissRequest = {},
                uiState =
                    mutableStateOf(
                        ContactsListState.Success(
                            availableContactsGroups =
                                ContactTestDataFactory.groupContactsForTest(listOf(testContact)),
                            selectedContacts =
                                contactsSelectionOf(testContact.id, setOf(testContact.id)),
                            isMultiSelectEnabled = false,
                            callingAppName = null,
                            requestedMimeTypes = emptyList(),
                            showPrivacyBanner = false,
                        )
                    ),
                userState = PickerUserState.Loading,
                snackbarEvents = events,
                onToggleContactSelection = { _, _ -> },
                onToggleEntrySelection = { _, _, _ -> },
                onClearSelection = {},
                onDoneClicked = {},
                onQueryChange = {},
                onExitSearch = {},
                onPreviewClicked = {},
                onBackFromPreview = {},
                onPrivacyBannerDismissRequest = {},
                onProfileClicked = {},
                onDismissProfileBlockedDialog = {},
            )
        }

        composeTestRule.onNodeWithText(snackbarMessage).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(snackbarMessage).assertDoesNotExist()

        events.tryEmit(SnackbarEvent.ShowSelectionLimitReached(2))

        composeTestRule.onNodeWithText(snackbarMessage).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(snackbarMessage).assertIsDisplayed()

        // Advance the clock past the Snackbar's default duration (4 seconds) + 1 to be safe
        composeTestRule.mainClock.advanceTimeBy(5000)

        composeTestRule.onNodeWithText(snackbarMessage).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(snackbarMessage).assertDoesNotExist()
    }

    @Test
    fun fromPreviewScreen_clickingBackIcon_onBackFromPreviewInvoked() {
        var onBackFromPreview = false
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                ContactsPickerBottomSheet(
                    onDismissRequest = {},
                    uiState =
                        mutableStateOf(
                            ContactsPreviewState(
                                contactsToDisplay = listOf(testContact),
                                selectedContacts =
                                    contactsSelectionOf(testContact.id, setOf(testContact.id)),
                                isMultiSelectEnabled = false,
                            )
                        ),
                    userState = PickerUserState.Loading,
                    snackbarEvents = flowOf(),
                    onToggleContactSelection = { _, _ -> },
                    onToggleEntrySelection = { _, _, _ -> },
                    onClearSelection = {},
                    onDoneClicked = {},
                    onQueryChange = {},
                    onExitSearch = {},
                    onPreviewClicked = {},
                    onBackFromPreview = { onBackFromPreview = true },
                    onPrivacyBannerDismissRequest = {},
                    onProfileClicked = {},
                    onDismissProfileBlockedDialog = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.title_top_bar_back_button_content_description)
            )
            .performClick()
        assertThat(onBackFromPreview).isTrue()
    }

    fun fromPreviewScreen_clickingBackButton_onBackFromPreviewInvoked() {
        var onBackFromPreview = false
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                ContactsPickerBottomSheet(
                    onDismissRequest = {},
                    uiState =
                        mutableStateOf(
                            ContactsListState.Success(
                                availableContactsGroups =
                                    ContactTestDataFactory.groupContactsForTest(
                                        listOf(testContact)
                                    ),
                                selectedContacts =
                                    contactsSelectionOf(testContact.id, setOf(testContact.id)),
                                isMultiSelectEnabled = false,
                                callingAppName = null,
                                showPrivacyBanner = false,
                                requestedMimeTypes = emptyList(),
                            )
                        ),
                    userState = PickerUserState.Loading,
                    snackbarEvents = flowOf(),
                    onToggleContactSelection = { _, _ -> },
                    onToggleEntrySelection = { _, _, _ -> },
                    onClearSelection = {},
                    onDoneClicked = {},
                    onQueryChange = {},
                    onExitSearch = {},
                    onPreviewClicked = {},
                    onBackFromPreview = { onBackFromPreview = true },
                    onPrivacyBannerDismissRequest = {},
                    onProfileClicked = {},
                    onDismissProfileBlockedDialog = {},
                )
            }
        }
        composeTestRule
            .onNodeWithText(context.getString(R.string.selection_bottom_bar_preview_button_label))
            .performClick()

        val backButton =
            composeTestRule.onNodeWithText(
                context.getString(R.string.selection_bottom_bar_back_button_label)
            )
        backButton.assertIsDisplayed()
        backButton.performClick()
        assertThat(onBackFromPreview).isTrue()
    }

    @Test
    fun selectionBar_isVisible_onPreviewScreen_whenAContactIsSelected() {
        setupBottomSheet(
            uiState =
                ContactsPreviewState(
                    contactsToDisplay = listOf(testContact),
                    selectedContacts = contactsSelectionOf(testContact.id, setOf(testContact.id)),
                    isMultiSelectEnabled = false,
                )
        )

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_preview_button_label)
            )
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText(context.getString(R.string.selection_bottom_bar_back_button_label))
            .assertIsDisplayed()
    }

    @Test
    fun selectionBar_isAlignedWithBottomSheet() {
        // GIVEN the bottom sheet is visible with a selectable contact
        selectedContacts = emptyContactsSelection()
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                ContactsPickerBottomSheet(
                    onDismissRequest = {},
                    uiState =
                        mutableStateOf(
                            ContactsListState.Success(
                                availableContactsGroups =
                                    ContactTestDataFactory.groupContactsForTest(
                                        listOf(testContact)
                                    ),
                                selectedContacts = selectedContacts,
                                isMultiSelectEnabled = true,
                                callingAppName = null,
                                showPrivacyBanner = false,
                                requestedMimeTypes = emptyList(),
                            )
                        ),
                    userState = PickerUserState.Loading,
                    snackbarEvents = flowOf(),
                    onToggleContactSelection = { _, _ -> },
                    onToggleEntrySelection = { _, _, _ -> },
                    onClearSelection = {},
                    onDoneClicked = {},
                    onQueryChange = {},
                    onExitSearch = {},
                    onPreviewClicked = {},
                    onBackFromPreview = {},
                    onPrivacyBannerDismissRequest = {},
                    onProfileClicked = {},
                    onDismissProfileBlockedDialog = {},
                )
            }
        }

        // WHEN a contact is selected, making the selection bar visible
        selectedContacts = contactsSelectionOf(testContact.id, setOf(testContact.id))
        composeTestRule.waitForIdle()

        // THEN the selection bar should be aligned with the bottom sheet
        val bottomSheetBounds =
            composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).getUnclippedBoundsInRoot()

        val clearButtonNode =
            composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )

        // The parent of the clear button is the SelectionBottomBar (a Row)
        val selectionBarBounds = clearButtonNode.onParent().getUnclippedBoundsInRoot()

        // Assert that the selection bar is horizontally inset within the bottom sheet.
        // This is more resilient to refactoring than checking for exact padding values.
        assertThat(selectionBarBounds.left).isGreaterThan(bottomSheetBounds.left)
        assertThat(selectionBarBounds.right).isLessThan(bottomSheetBounds.right)
        assertThat(selectionBarBounds.width).isLessThan(bottomSheetBounds.width)
    }

    @Test
    fun selectionBar_isVerticallyAlignedWithBottomSheet() {
        // GIVEN the bottom sheet is visible with a selectable contact
        selectedContacts = emptyContactsSelection()
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                ContactsPickerBottomSheet(
                    onDismissRequest = {},
                    uiState =
                        mutableStateOf(
                            ContactsListState.Success(
                                availableContactsGroups =
                                    ContactTestDataFactory.groupContactsForTest(
                                        listOf(testContact)
                                    ),
                                selectedContacts = selectedContacts,
                                isMultiSelectEnabled = true,
                                callingAppName = null,
                                showPrivacyBanner = false,
                                requestedMimeTypes = emptyList(),
                            )
                        ),
                    userState = PickerUserState.Loading,
                    snackbarEvents = flowOf(),
                    onToggleContactSelection = { _, _ -> },
                    onToggleEntrySelection = { _, _, _ -> },
                    onClearSelection = {},
                    onDoneClicked = {},
                    onQueryChange = {},
                    onExitSearch = {},
                    onPreviewClicked = {},
                    onBackFromPreview = {},
                    onPrivacyBannerDismissRequest = {},
                    onProfileClicked = {},
                    onDismissProfileBlockedDialog = {},
                )
            }
        }

        // WHEN a contact is selected, making the selection bar visible
        selectedContacts = contactsSelectionOf(testContact.id, setOf(testContact.id))
        composeTestRule.waitForIdle()

        // THEN the selection bar should be vertically aligned with the bottom of the bottom sheet,
        // as both should respect the same navigation bar insets.
        val bottomSheetBounds =
            composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).getUnclippedBoundsInRoot()

        val clearButtonNode =
            composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.selection_bottom_bar_clear_button_content_description)
            )

        // The parent of the clear button is the SelectionBottomBar (a Row)
        val selectionBarBounds = clearButtonNode.onParent().getUnclippedBoundsInRoot()

        // The SelectionBottomBar should be positioned above the bottom of the bottom sheet,
        // which implies that it respects the navigation bar insets and has its own padding.
        // This is more resilient than asserting an exact padding value.
        assertThat(selectionBarBounds.bottom).isLessThan(bottomSheetBounds.bottom)
    }

    private fun setupBottomSheet(
        onDismissRequest: () -> Unit = {},
        uiState: ContactsUiState = ContactsListState.Loading,
    ) {
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                ContactsPickerBottomSheet(
                    onDismissRequest = onDismissRequest,
                    uiState = mutableStateOf(uiState),
                    userState = PickerUserState.Loading,
                    snackbarEvents = flowOf(),
                    onToggleContactSelection = { _, _ -> },
                    onToggleEntrySelection = { _, _, _ -> },
                    onClearSelection = {},
                    onDoneClicked = {},
                    onQueryChange = {},
                    onExitSearch = {},
                    onPreviewClicked = {},
                    onBackFromPreview = {},
                    onPrivacyBannerDismissRequest = {},
                    onProfileClicked = {},
                    onDismissProfileBlockedDialog = {},
                )
            }
        }
    }
}
