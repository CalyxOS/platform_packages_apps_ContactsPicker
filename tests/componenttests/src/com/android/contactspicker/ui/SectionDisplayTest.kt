/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.contactspicker.ui

import android.content.Context
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.R
import com.android.contactspicker.data.model.SectionKey.EmojiSection
import com.android.contactspicker.data.model.SectionKey.FavoriteSection
import com.android.contactspicker.data.model.SectionKey.LetterKey
import com.android.contactspicker.data.model.SectionKey.StringKey
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class SectionDisplayTest {

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getSectionDisplay_forLetterKey_returnsText() {
        val key = LetterKey('Z')
        var display: SectionDisplay? = null
        composeTestRule.setContent { display = key.getSectionDisplay() }

        assertThat(display).isInstanceOf(SectionDisplay.Text::class.java)
        assertThat((display as SectionDisplay.Text).text).isEqualTo("Z")
    }

    @Test
    fun getSectionDisplay_forStringKey_returnsText() {
        val key = StringKey("Other")
        var display: SectionDisplay? = null
        composeTestRule.setContent { display = key.getSectionDisplay() }

        assertThat(display).isInstanceOf(SectionDisplay.Text::class.java)
        assertThat((display as SectionDisplay.Text).text).isEqualTo("Other")
    }

    @Test
    fun getSectionDisplay_forFavoriteSection_returnsIconWithText() {
        val key = FavoriteSection
        var display: SectionDisplay? = null
        composeTestRule.setContent { display = key.getSectionDisplay() }

        assertThat(display).isInstanceOf(SectionDisplay.Icon::class.java)
        val iconDisplay = display as SectionDisplay.Icon
        assertThat(iconDisplay.icon).isEqualTo(Icons.Filled.Star)
        assertThat(iconDisplay.iconContentDescription)
            .isEqualTo(context.getString(R.string.favorites_header_icon_content_description))
        assertThat(iconDisplay.text)
            .isEqualTo(context.getString(R.string.contacts_picker_favorites_header))
    }

    @Test
    fun getSectionDisplay_forEmojiSection_returnsIconWithoutText() {
        val key = EmojiSection
        var display: SectionDisplay? = null
        composeTestRule.setContent { display = key.getSectionDisplay() }

        assertThat(display).isInstanceOf(SectionDisplay.Icon::class.java)
        val iconDisplay = display as SectionDisplay.Icon
        assertThat(iconDisplay.icon).isEqualTo(Icons.Filled.Mood)
        assertThat(iconDisplay.iconContentDescription)
            .isEqualTo(context.getString(R.string.emoji_header_icon_content_description))
        assertThat(iconDisplay.text).isNull()
    }
}
