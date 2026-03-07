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
package com.android.contactspicker.ui.scrubber

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.SectionKey
import com.android.contactspicker.data.model.SectionKey.EmojiSection
import com.android.contactspicker.data.model.SectionKey.FavoriteSection
import com.android.contactspicker.data.model.SectionKey.LetterKey
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ScrubberControllerTest {

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var controller: ScrubberController
    private var lastScrollRequest: Int? = null

    private fun setContent(
        contactSections: Map<SectionKey, List<Contact>>,
        showPrivacyBanner: Boolean = false,
    ) {
        lastScrollRequest = null
        composeTestRule.setContent {
            controller =
                rememberScrubberController(
                    contactSections = contactSections,
                    showPrivacyBanner = showPrivacyBanner,
                )

            LaunchedEffect(controller) {
                controller.scrollRequests.collect { index -> lastScrollRequest = index }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun scrubberDrag_emitsScrollRequest_atFullDrag() {
        val contacts = ContactTestDataFactory.createContactList(25)
        val contactSections: Map<SectionKey, List<Contact>> =
            sortedMapOf(SectionKey.COMPARATOR, LetterKey('A') to contacts)
        setContent(contactSections)

        performScrubberDrag(dragFraction = 1.0f)
        // Index 24 for contacts + 1 for sticky header
        assertThat(lastScrollRequest).isEqualTo(25)
    }

    @Test
    fun scrubberDrag_emitsScrollRequest_atHalfwayDrag() {
        val contacts = ContactTestDataFactory.createContactList(25)
        val contactSections: Map<SectionKey, List<Contact>> =
            sortedMapOf(SectionKey.COMPARATOR, LetterKey('A') to contacts)
        setContent(contactSections)

        performScrubberDrag(dragFraction = 0.5f)
        // Index 12 for middle contact + 1 for sticky header
        assertThat(lastScrollRequest).isEqualTo(13)
    }

    @Test
    fun updateVerticalOffsetFraction_updatesScrubberState() {
        val contacts = ContactTestDataFactory.createContactList(25)
        val contactSections: Map<SectionKey, List<Contact>> =
            sortedMapOf(SectionKey.COMPARATOR, LetterKey('A') to contacts)
        setContent(contactSections)

        composeTestRule.runOnUiThread {
            controller.updateVerticalOffsetFraction(preciseListIndex = 10f)
        }
        composeTestRule.waitForIdle()
        // Expected: (listIndex- 1 for header) / (totalContacts - 1) = (9/24) = 0.375
        assertThat(controller.scrubberState.verticalOffsetFraction).isEqualTo(0.375f)
    }

    @Test
    fun updateVerticalOffsetFraction_doesNotUpdateScrubber_whenDragging() {
        val contacts = ContactTestDataFactory.createContactList(25)
        val contactSections: Map<SectionKey, List<Contact>> =
            sortedMapOf(SectionKey.COMPARATOR, LetterKey('A') to contacts)
        setContent(contactSections)

        // 1. Position Scrubber to Top and set Dragging = true
        startScrubberDragAt(0.0f)

        // 2. Attempt to update offset from List
        composeTestRule.runOnUiThread {
            controller.updateVerticalOffsetFraction(preciseListIndex = 100f)
        }
        composeTestRule.waitForIdle()

        // 3. Assert that Scrubber stayed at 0.0f
        assertThat(controller.scrubberState.verticalOffsetFraction).isEqualTo(0.0f)
    }

    @Test
    fun isListScrollEnabledForUser_isTrue_whenNotDragging() {
        setContent(sortedMapOf(SectionKey.COMPARATOR))
        composeTestRule.runOnUiThread { controller.scrubberState.setDragging(false) }

        assertThat(controller.isListScrollEnabledForUser).isTrue()
    }

    @Test
    fun isListScrollEnabledForUser_isFalse_whenDragging() {
        setContent(sortedMapOf(SectionKey.COMPARATOR))
        composeTestRule.runOnUiThread { controller.scrubberState.setDragging(true) }

        assertThat(controller.isListScrollEnabledForUser).isFalse()
    }

    @Test
    fun labelSectionKey_isNull_whenNotDragging() {
        setContent(sortedMapOf(SectionKey.COMPARATOR))
        composeTestRule.runOnUiThread { controller.scrubberState.setDragging(false) }
        composeTestRule.waitForIdle()

        assertThat(controller.labelSectionKey).isNull()
    }

    @Test
    fun labelSectionKey_showsFavoriteIcon_whenDraggingOverFavorites() {
        val favoriteContact =
            ContactTestDataFactory.createDisplayNameContact(
                id = 1,
                displayName = "Fav",
                isFavorite = true,
            )
        val regularContact =
            ContactTestDataFactory.createDisplayNameContact(id = 2, displayName = "Reg")
        val contactSections: Map<SectionKey, List<Contact>> =
            sortedMapOf(
                SectionKey.COMPARATOR,
                FavoriteSection to listOf(favoriteContact),
                LetterKey('R') to listOf(regularContact),
            )
        setContent(contactSections)

        // Drag to the top, which should correspond to the favorite contact
        startScrubberDragAt(0.0f)

        assertThat(controller.labelSectionKey).isEqualTo(FavoriteSection)
    }

    @Test
    fun labelSectionKey_showsLetter_whenDraggingOverLetterContact() {
        val contactA =
            ContactTestDataFactory.createDisplayNameContact(id = 1, displayName = "Alice")
        val contactSections: Map<SectionKey, List<Contact>> =
            sortedMapOf(SectionKey.COMPARATOR, LetterKey('A') to listOf(contactA))
        setContent(contactSections)

        startScrubberDragAt(0.0f)

        val label = controller.labelSectionKey
        assertThat((label as LetterKey).letter).isEqualTo('A')
    }

    @Test
    fun labelSectionKey_showsEmojiIcon_whenDraggingOverNonLetterContact() {
        val emojiContact =
            ContactTestDataFactory.createDisplayNameContact(id = 1, displayName = "#Hash")
        val contactSections: Map<SectionKey, List<Contact>> =
            sortedMapOf(SectionKey.COMPARATOR, EmojiSection to listOf(emojiContact))
        setContent(contactSections)

        startScrubberDragAt(0.0f)

        assertThat(controller.labelSectionKey).isEqualTo(EmojiSection)
    }

    /** Simulates a complete drag gesture on the scrubber. */
    private fun performScrubberDrag(dragFraction: Float) {
        composeTestRule.runOnUiThread {
            controller.scrubberState.setDragging(true)
            controller.scrubberState.onDrag(dragFraction)
            controller.scrubberState.setDragging(false)
        }
        composeTestRule.waitForIdle()
    }

    /** Simulates the start of a drag gesture at a specific fractional offset. */
    private fun startScrubberDragAt(fraction: Float) {
        composeTestRule.runOnUiThread {
            controller.scrubberState.setDragging(true)
            controller.scrubberState.setVerticalOffsetFraction(fraction)
        }
        composeTestRule.waitForIdle()
    }
}
