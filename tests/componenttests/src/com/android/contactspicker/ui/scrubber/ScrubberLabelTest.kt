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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ui.pickerscreen.SectionKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ScrubberLabelTest {

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun scrubberLabel_whenSectionKeyIsNull_nothingIsDisplayed() {
        composeTestRule.setContent { ScrubberLabel(sectionKey = null) }
        assertLabelContent(visibleTag = null)
    }

    @Test
    fun scrubberLabel_whenSectionKeyIsLetter_displaysText() {
        val letterKey = SectionKey.LetterKey('A')
        composeTestRule.setContent { ScrubberLabel(sectionKey = letterKey) }
        assertLabelContent(visibleTag = SCRUBBER_LABEL_TEXT_TEST_TAG)
        composeTestRule.onNodeWithTag(SCRUBBER_LABEL_TEXT_TEST_TAG).assertTextEquals("A")
    }

    @Test
    fun scrubberLabel_whenSectionKeyIsFavorite_displaysFavoriteIcon() {
        val favoriteKey = SectionKey.FavoriteIconKey
        composeTestRule.setContent { ScrubberLabel(sectionKey = favoriteKey) }
        assertLabelContent(visibleTag = SCRUBBER_LABEL_FAVORITE_ICON_TEST_TAG)
    }

    @Test
    fun scrubberLabel_whenSectionKeyIsEmoji_displaysEmojiIcon() {
        val emojiKey = SectionKey.EmojiIconKey
        composeTestRule.setContent { ScrubberLabel(sectionKey = emojiKey) }
        assertLabelContent(visibleTag = SCRUBBER_LABEL_EMOJI_ICON_TEST_TAG)
    }

    /**
     * Asserts the correct visibility of scrubber label content based on the provided visibleTag.
     *
     * @param visibleTag The test tag of the composable that should be visible. If null, asserts
     *   that all content tags are absent.
     */
    private fun assertLabelContent(visibleTag: String?) {
        val allTags =
            listOf(
                SCRUBBER_LABEL_TEXT_TEST_TAG,
                SCRUBBER_LABEL_FAVORITE_ICON_TEST_TAG,
                SCRUBBER_LABEL_EMOJI_ICON_TEST_TAG,
            )

        allTags.forEach { tag ->
            if (tag == visibleTag) {
                composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
            } else {
                composeTestRule.onNodeWithTag(tag).assertDoesNotExist()
            }
        }
    }
}
