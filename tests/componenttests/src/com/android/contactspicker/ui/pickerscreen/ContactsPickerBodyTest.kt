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
import androidx.collection.MutableLongObjectMap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.R
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.SelectionSource
import com.android.contactspicker.data.model.emptyContactsSelection
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.android.contactspicker.ui.components.AVATAR_TEST_TAG
import com.android.contactspicker.ui.scrubber.SCRUBBER_HANDLE_TEST_TAG
import com.android.contactspicker.ui.scrubber.SCRUBBER_LABEL_TEST_TAG
import com.android.contactspicker.ui.scrubber.SCRUBBER_VISIBILITY_TIMEOUT_MILLIS
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

    private val EMPTY_ON_TOGGLE_CONTACT_SELECTION_CALLBACK: (Contact, SelectionSource) -> Unit =
        { _, _ ->
        }
    private val EMPTY_ON_TOGGLE_ENTRY_SELECTION_CALLBACK: (Long, Long, SelectionSource) -> Unit =
        { _, _, _ ->
        }

    @Test
    fun contactsList_displaysHeadersAndContacts() {
        val contacts = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST

        composeTestRule.setContent {
            ContactsPickerBody(
                availableContactsGroups = ContactTestDataFactory.groupContactsForTest(contacts),
                selectedContacts = emptyContactsSelection(),
                isMultiSelectEnabled = false,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = EMPTY_ON_TOGGLE_CONTACT_SELECTION_CALLBACK,
                onToggleEntrySelection = EMPTY_ON_TOGGLE_ENTRY_SELECTION_CALLBACK,
                callingAppName = null,
                showPrivacyBanner = false,
            )
        }

        contacts.forEach { contact ->
            val displayName = contact.displayName
            val initial = contact.getDisplayNameInitialLetter().toString()
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
                availableContactsGroups =
                    ContactTestDataFactory.groupContactsForTest(
                        listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT)
                    ),
                selectedContacts = emptyContactsSelection(),
                isMultiSelectEnabled = false,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = EMPTY_ON_TOGGLE_CONTACT_SELECTION_CALLBACK,
                onToggleEntrySelection = EMPTY_ON_TOGGLE_ENTRY_SELECTION_CALLBACK,
                showPrivacyBanner = true,
                callingAppName = null,
            )
        }
        composeTestRule.onNode(hasTestTag(PRIVACY_BANNER_TEST_TAG)).assertIsDisplayed()
    }

    @Test
    fun privacyBanner_isNotDisplayed() {
        composeTestRule.setContent {
            ContactsPickerBody(
                availableContactsGroups =
                    ContactTestDataFactory.groupContactsForTest(
                        listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT)
                    ),
                selectedContacts = emptyContactsSelection(),
                isMultiSelectEnabled = false,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = EMPTY_ON_TOGGLE_CONTACT_SELECTION_CALLBACK,
                onToggleEntrySelection = EMPTY_ON_TOGGLE_ENTRY_SELECTION_CALLBACK,
                showPrivacyBanner = false,
                callingAppName = null,
            )
        }
        composeTestRule.onNode(hasTestTag(PRIVACY_BANNER_TEST_TAG)).assertIsNotDisplayed()
    }

    @Test
    fun privacyBanner_isNotDisplayed_afterScrollingTheContactList() {
        val contacts = ContactTestDataFactory.createContactList(30)

        setContentWithContactsPickerBody(contacts)

        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasTestTag(PRIVACY_BANNER_TEST_TAG)).assertIsDisplayed()
        composeTestRule
            .onNode(hasTestTag(CONTACTS_LIST_TEST_TAG))
            .performScrollToIndex(contacts.size - 1)
        composeTestRule.waitForIdle()
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

    @Test
    fun contactsList_rendersSections_inCorrectOrder() {
        val favContact =
            ContactTestDataFactory.createDisplayNameContact(
                id = 1,
                displayName = "Alice",
                isFavorite = true,
            )
        val emojiContact =
            ContactTestDataFactory.createDisplayNameContact(id = 2, displayName = "#Symbol")
        val letterContact =
            ContactTestDataFactory.createDisplayNameContact(id = 3, displayName = "Bob")

        setContentWithContactsPickerBody(listOf(favContact, emojiContact, letterContact))

        val favStickyHeaderTitle = context.getString(R.string.contacts_picker_favorites_header)
        val emojiDesc = context.getString(R.string.emoji_header_icon_content_description)
        val letterHeader = "B" // Initial for Bob

        val favBounds =
            composeTestRule.onNodeWithText(favStickyHeaderTitle).fetchSemanticsNode().boundsInRoot

        val emojiBounds =
            composeTestRule
                .onNodeWithContentDescription(emojiDesc, useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot

        val letterBounds =
            composeTestRule
                .onNode(hasTestTag(CONTACTS_LIST_SECTION_HEADER_TEST_TAG) and hasText(letterHeader))
                .fetchSemanticsNode()
                .boundsInRoot

        assertThat(favBounds.top).isLessThan(emojiBounds.top)
        assertThat(emojiBounds.top).isLessThan(letterBounds.top)
    }

    @Test
    fun contactsList_hasExtraBottomPadding_whenSelectionIsNotEmpty() {
        val contacts = ContactTestDataFactory.createContactList(20)
        val selectedContactsState = mutableStateOf(emptyContactsSelection())

        composeTestRule.setContent {
            ContactsPickerBody(
                availableContactsGroups = ContactTestDataFactory.groupContactsForTest(contacts),
                selectedContacts = selectedContactsState.value,
                isMultiSelectEnabled = true,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = EMPTY_ON_TOGGLE_CONTACT_SELECTION_CALLBACK,
                onToggleEntrySelection = EMPTY_ON_TOGGLE_ENTRY_SELECTION_CALLBACK,
                callingAppName = null,
                showPrivacyBanner = false,
            )
        }

        val lastContactName = contacts.last().displayName
        val listNode = composeTestRule.onNode(hasTestTag(CONTACTS_LIST_TEST_TAG))
        listNode.performScrollToIndex(contacts.size - 1)
        composeTestRule.waitForIdle()

        val listBounds = listNode.fetchSemanticsNode().boundsInRoot
        val lastItemBoundsEmpty =
            composeTestRule.onNodeWithText(lastContactName).fetchSemanticsNode().boundsInRoot
        val gapWhenEmpty = listBounds.bottom - lastItemBoundsEmpty.bottom
        val newSelection = MutableLongObjectMap<Set<Long>>()
        newSelection.put(contacts.first().id, emptySet())
        selectedContactsState.value = newSelection
        composeTestRule.waitForIdle()

        listNode.performScrollToIndex(contacts.size - 1)
        composeTestRule.waitForIdle()

        val lastItemBoundsSelected =
            composeTestRule.onNodeWithText(lastContactName).fetchSemanticsNode().boundsInRoot
        val gapWhenSelected = listBounds.bottom - lastItemBoundsSelected.bottom

        assertThat(gapWhenSelected).isGreaterThan(gapWhenEmpty)
        assertThat(gapWhenSelected - gapWhenEmpty).isAtLeast(80f)
    }

    @Test
    fun scrubberHandle_isDisplayed_whenListIsScrolled() {
        val contacts =
            ContactTestDataFactory.createContactList(MIN_CONTACTS_COUNT_FOR_SCRUBBER_ACTIVATION)
        setContentWithContactsPickerBody(contacts)
        composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG).assertIsNotDisplayed()
        // Wake up scrubber handle
        performListScroll()
        composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SCRUBBER_LABEL_TEST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun scrubberHandle_isNotDisplayed_afterScrollAndDelay() {
        val contacts =
            ContactTestDataFactory.createContactList(MIN_CONTACTS_COUNT_FOR_SCRUBBER_ACTIVATION)
        setContentWithContactsPickerBody(contacts)

        performListScroll()

        composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG).assertIsDisplayed()
        composeTestRule.mainClock.advanceTimeBy(SCRUBBER_VISIBILITY_TIMEOUT_MILLIS)
        composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun scrubberHandle_positionChanges_whenListIsScrolled() {
        val contacts =
            ContactTestDataFactory.createContactList(MIN_CONTACTS_COUNT_FOR_SCRUBBER_ACTIVATION)
        setContentWithContactsPickerBody(contacts)
        // Wake up scrubber handle
        performListScroll(duration = 1000)

        val initialHandleBounds =
            composeTestRule
                .onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG)
                .fetchSemanticsNode()
                .boundsInRoot

        // Scroll the list towards the bottom
        performListScroll(duration = 50)

        // Get the final position of the scrubber handle.
        val finalHandleBounds =
            composeTestRule
                .onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG)
                .fetchSemanticsNode()
                .boundsInRoot
        // Assert that the handle has moved down.
        assertThat(finalHandleBounds.top).isGreaterThan(initialHandleBounds.top)
    }

    @Test
    fun scrubberHandleDrag_scrollsTheList() {
        val contacts = ContactTestDataFactory.createContactList(100)
        setContentWithContactsPickerBody(contacts)
        // Initially first contact should be visible
        composeTestRule.onNodeWithText(contacts.first().displayName).assertIsDisplayed()
        val listHeight =
            composeTestRule
                .onNode(hasTestTag(CONTACTS_LIST_TEST_TAG))
                .fetchSemanticsNode()
                .boundsInRoot
                .height
        // Wake up the scrubber
        performListScroll()
        composeTestRule.onNodeWithText(contacts.first().displayName).assertIsNotDisplayed()
        composeTestRule.onNodeWithText(contacts.last().displayName).assertIsNotDisplayed()
        // Drag Scrubber handle to the bottom end
        performScrubberDrag(yDelta = listHeight)
        composeTestRule.onNodeWithText(contacts.last().displayName).assertIsDisplayed()
        composeTestRule.onNodeWithText(contacts.first().displayName).assertIsNotDisplayed()
    }

    @Test
    fun scrubberLabel_appearsDuringDrag_andDisappearsOnRelease() {
        val contacts =
            ContactTestDataFactory.createContactList(MIN_CONTACTS_COUNT_FOR_SCRUBBER_ACTIVATION)
        setContentWithContactsPickerBody(contacts)

        // Wake up scrubber handle
        performListScroll()

        // Drag the scrubber handle to make the label appear.
        performScrubberDrag(yDelta = 100f, release = false)

        // The label is displayed during the drag.
        composeTestRule.onNodeWithTag(SCRUBBER_LABEL_TEST_TAG).assertIsDisplayed()

        // Complete the gesture by releasing the scrubber handle.
        composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG).performTouchInput { up() }
        composeTestRule.waitForIdle()

        // The label is no longer displayed after the drag is released.
        composeTestRule.onNodeWithTag(SCRUBBER_LABEL_TEST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun scrubber_isNotDisplayed_whenManyFavoritesButFewUniqueContacts() {
        // Create 30 unique contacts, and make all of them favorites.
        // Total rendered items in UI = 30 (Favorites) + 30 (Main list) = 60.
        val contacts =
            (0 until 30).map {
                ContactTestDataFactory.createDisplayNameContact(
                    id = it.toLong(),
                    displayName = "Contact $it",
                    isFavorite = true,
                )
            }
        setContentWithContactsPickerBody(contacts)
        // totalContactsCount (excluding Favorites section) = 30.
        // Since 30 < 50, scrubber should not be displayed.
        composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun scrubber_isNotDisplayed_whenContactsAreLessThanMinimum() {
        val contacts =
            ContactTestDataFactory.createContactList(MIN_CONTACTS_COUNT_FOR_SCRUBBER_ACTIVATION - 1)
        setContentWithContactsPickerBody(contacts)
        composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun scrubber_isDisplayed_whenContactsAreMoreThanOrEqualToMinimum() {
        val contacts =
            ContactTestDataFactory.createContactList(MIN_CONTACTS_COUNT_FOR_SCRUBBER_ACTIVATION)
        setContentWithContactsPickerBody(contacts)
        performListScroll()
        composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun onToggleContactSelection_fromList_passesListSource() {
        var toggledSource: SelectionSource? = null
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT

        composeTestRule.setContent {
            ContactsPickerBody(
                availableContactsGroups =
                    ContactTestDataFactory.groupContactsForTest(listOf(contact)),
                selectedContacts = emptyContactsSelection(),
                isMultiSelectEnabled = false,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = { _, source -> toggledSource = source },
                onToggleEntrySelection = { _, _, _ -> },
                callingAppName = null,
                showPrivacyBanner = false,
            )
        }

        composeTestRule.onNodeWithText(contact.displayName).performClick()

        assertThat(toggledSource).isEqualTo(SelectionSource.MAIN_LIST)
    }

    @Test
    fun onToggleContactSelection_fromFavorites_passesFavoritesSource() {
        var toggledSource: SelectionSource? = null
        // Create or copy a contact that is explicitly marked as a favorite
        val favoriteContact =
            DisplayNameContact(
                id = 123L,
                displayName = "Favorite Person",
                profilePictureUri = null,
                isFavorite = true,
                lookupKey = "fav_key",
            )

        composeTestRule.setContent {
            ContactsPickerBody(
                availableContactsGroups =
                    ContactTestDataFactory.groupContactsForTest(listOf(favoriteContact)),
                selectedContacts = emptyContactsSelection(),
                isMultiSelectEnabled = false,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = { _, source -> toggledSource = source },
                onToggleEntrySelection = { _, _, _ -> },
                callingAppName = null,
                showPrivacyBanner = false,
            )
        }

        // Click the first instance of the contact, which will be in the favorites section
        composeTestRule.onAllNodesWithText(favoriteContact.displayName).onFirst().performClick()

        assertThat(toggledSource).isEqualTo(SelectionSource.FAVORITES)
    }

    @Test
    fun contactsList_whenSelectionChanges_doesNotScroll() {
        val contacts = ContactTestDataFactory.createContactList(20)
        val selectedContactsState = mutableStateOf(emptyContactsSelection())

        composeTestRule.setContent {
            ContactsPickerBody(
                availableContactsGroups = ContactTestDataFactory.groupContactsForTest(contacts),
                selectedContacts = selectedContactsState.value,
                isMultiSelectEnabled = true,
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                onToggleContactSelection = { _, _ -> },
                onToggleEntrySelection = { _, _, _ -> },
                callingAppName = null,
                showPrivacyBanner = false,
            )
        }

        val listNode = composeTestRule.onNode(hasTestTag(CONTACTS_LIST_TEST_TAG))

        // scroll down from the top
        listNode.performScrollToIndex(5)
        composeTestRule.waitForIdle()

        // capture the exact screen position of a visible item
        val visibleContactName = contacts[5].displayName
        val itemBoundsBefore =
            composeTestRule.onNodeWithText(visibleContactName).fetchSemanticsNode().boundsInRoot

        // select a contact to make the selection bar appear
        val newSelection = MutableLongObjectMap<Set<Long>>()
        newSelection.put(contacts[6].id, emptySet())
        selectedContactsState.value = newSelection
        composeTestRule.waitForIdle()

        // verify the item hasn't moved (the list did not scroll up)
        val itemBoundsWithSelectionBar =
            composeTestRule.onNodeWithText(visibleContactName).fetchSemanticsNode().boundsInRoot

        assertThat(itemBoundsWithSelectionBar.top).isEqualTo(itemBoundsBefore.top)

        // same for the selection bar disappearing
        selectedContactsState.value = MutableLongObjectMap()
        composeTestRule.waitForIdle()
        val itemBoundsAfter =
            composeTestRule.onNodeWithText(visibleContactName).fetchSemanticsNode().boundsInRoot

        assertThat(itemBoundsAfter.top).isEqualTo(itemBoundsBefore.top)
    }

    private fun setContentWithContactsPickerBody(contacts: List<Contact>) {
        composeTestRule.setContent {
            ContactsPickerBody(
                availableContactsGroups = ContactTestDataFactory.groupContactsForTest(contacts),
                onPrivacyBannerMoreDetails = {},
                onPrivacyBannerDismissRequest = {},
                selectedContacts = emptyContactsSelection(),
                isMultiSelectEnabled = false,
                showPrivacyBanner = true,
                onToggleContactSelection = EMPTY_ON_TOGGLE_CONTACT_SELECTION_CALLBACK,
                onToggleEntrySelection = EMPTY_ON_TOGGLE_ENTRY_SELECTION_CALLBACK,
                callingAppName = null,
            )
        }
    }

    /**
     * Drags the scrubber handle vertically by the specified [yDelta].
     *
     * @param release If true, lifts the finger (up) after moving. If false, keeps the pointer down
     *   (useful for checking labels visible during drag).
     */
    private fun performScrubberDrag(yDelta: Float, release: Boolean = true) {
        composeTestRule
            .onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG)
            .assertIsDisplayed()
            .performTouchInput {
                down(center)
                moveBy(Offset(x = 0f, y = yDelta))
                if (release) up()
            }

        // Only wait for idle if we released, otherwise the UI might be in a transient state
        if (release) composeTestRule.waitForIdle()
    }

    /**
     * Performs a vertical swipe up on the contacts list. Used to "wake up" the scrubber or scroll
     * the list content.
     */
    private fun performListScroll(duration: Long = 500) {
        composeTestRule.onNode(hasTestTag(CONTACTS_LIST_TEST_TAG)).performTouchInput {
            swipeUp(durationMillis = duration)
        }
        // Wait for the scroll animation to finish and for the
        // AnimatedScrubber's fade-in animation to complete.
        composeTestRule.waitForIdle()
    }
}
