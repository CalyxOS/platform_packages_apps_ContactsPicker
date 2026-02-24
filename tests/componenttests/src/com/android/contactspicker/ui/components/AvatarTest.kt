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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class AvatarTest {

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val profilePicContentDesc =
        context.getString(R.string.contact_avatar_profile_picture_content_description)
    private val initialsContentDesc =
        context.getString(R.string.contact_avatar_initial_content_description)

    @Test
    fun avatar_whenProfilePictureUriIsNull_showsInitials() {
        composeTestRule.setContent {
            Avatar(displayName = "Alice Wonderland", profilePictureUri = null)
        }

        composeTestRule.onNodeWithContentDescription(initialsContentDesc).assertExists()
        composeTestRule.onNodeWithText("A").assertExists()
        composeTestRule.onNodeWithContentDescription(profilePicContentDesc).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AVATAR_FALLBACK_PERSON_ICON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun avatar_whenProfilePictureUriIsBlank_showsInitials() {
        composeTestRule.setContent {
            Avatar(displayName = "Alice Wonderland", profilePictureUri = "")
        }

        composeTestRule.onNodeWithContentDescription(initialsContentDesc).assertExists()
        composeTestRule.onNodeWithText("A").assertExists()
        composeTestRule.onNodeWithContentDescription(profilePicContentDesc).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AVATAR_FALLBACK_PERSON_ICON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun avatar_whenProfilePictureUriExists_showsImageComposable() {
        val fakeUri = "content://fake/uri/123"
        composeTestRule.setContent {
            Avatar(displayName = "Alice Wonderland", profilePictureUri = fakeUri)
        }

        // Assert that the composable that would host the image is present.
        composeTestRule.onNodeWithContentDescription(profilePicContentDesc).assertExists()
        composeTestRule.onNodeWithContentDescription(initialsContentDesc).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AVATAR_FALLBACK_PERSON_ICON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun avatar_noProfilePictureAndDisplayNameStartsWithSmallLetter_showsCapitalizedInitial() {
        composeTestRule.setContent {
            Avatar(displayName = "alice Wonderland", profilePictureUri = "")
        }

        composeTestRule.onNodeWithContentDescription(initialsContentDesc).assertExists()
        composeTestRule.onNodeWithText("A").assertExists()
        composeTestRule.onNodeWithText("a").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(profilePicContentDesc).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AVATAR_FALLBACK_PERSON_ICON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun avatar_noProfilePictureAndDisplayNameStartsWithEmoji_showsPersonIcon() {
        composeTestRule.setContent {
            // Smile emoji
            Avatar(displayName = "\uD83D\uDE42 Alice", profilePictureUri = "")
        }

        composeTestRule.onNodeWithTag(AVATAR_FALLBACK_PERSON_ICON_TEST_TAG).assertExists()
        composeTestRule.onNodeWithContentDescription(initialsContentDesc).assertExists()
        composeTestRule.onNodeWithText("A").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(profilePicContentDesc).assertDoesNotExist()
    }

    @Test
    fun avatar_noProfilePictureAndDisplayNameStartsWithNumber_showsPersonIcon() {
        composeTestRule.setContent { Avatar(displayName = "1234", profilePictureUri = "") }

        composeTestRule.onNodeWithTag(AVATAR_FALLBACK_PERSON_ICON_TEST_TAG).assertExists()
        composeTestRule.onNodeWithContentDescription(initialsContentDesc).assertExists()
        composeTestRule.onNodeWithText("A").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(profilePicContentDesc).assertDoesNotExist()
    }

    @Test
    fun avatar_noProfilePictureAndDisplayNameStartsWithSpecialSymbol_showsPersonIcon() {
        composeTestRule.setContent { Avatar(displayName = "@Name", profilePictureUri = "") }

        composeTestRule.onNodeWithTag(AVATAR_FALLBACK_PERSON_ICON_TEST_TAG).assertExists()
        composeTestRule.onNodeWithContentDescription(initialsContentDesc).assertExists()
        composeTestRule.onNodeWithText("A").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(profilePicContentDesc).assertDoesNotExist()
    }

    @Test
    fun avatar_noProfilePictureAndDisplayNameNoName_showsPersonIcon() {
        composeTestRule.setContent {
            // "(No name)"
            Avatar(
                displayName = context.getString(R.string.no_name_placeholder),
                profilePictureUri = "",
            )
        }

        composeTestRule.onNodeWithTag(AVATAR_FALLBACK_PERSON_ICON_TEST_TAG).assertExists()
        composeTestRule.onNodeWithContentDescription(initialsContentDesc).assertExists()
        composeTestRule.onNodeWithText("A").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(profilePicContentDesc).assertDoesNotExist()
    }

    @Test
    fun avatar_noProfilePictureAndDisplayNameEmpty_showsPersonIcon() {
        composeTestRule.setContent { Avatar(displayName = "", profilePictureUri = "") }

        composeTestRule.onNodeWithTag(AVATAR_FALLBACK_PERSON_ICON_TEST_TAG).assertExists()
        composeTestRule.onNodeWithContentDescription(initialsContentDesc).assertExists()
        composeTestRule.onNodeWithText("A").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(profilePicContentDesc).assertDoesNotExist()
    }
}
