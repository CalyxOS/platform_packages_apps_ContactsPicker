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

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ui.pickerscreen.SectionKey.EmojiIconKey
import com.android.contactspicker.ui.pickerscreen.SectionKey.FavoriteIconKey
import com.android.contactspicker.ui.pickerscreen.SectionKey.LetterKey
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class SectionKeyTest {

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun sortingOrder_isCorrect() {
        val favKey = FavoriteIconKey
        val emojiKey = EmojiIconKey
        val letterKey = LetterKey('A')

        assertThat(favKey < emojiKey).isTrue()
        assertThat(favKey < letterKey).isTrue()
        assertThat(emojiKey < letterKey).isTrue()
    }

    @Test
    fun equality_isConsistent() {
        assertThat(FavoriteIconKey.compareTo(FavoriteIconKey)).isEqualTo(0)
        assertThat(EmojiIconKey.compareTo(EmojiIconKey)).isEqualTo(0)

        val letterKey1 = LetterKey('A')
        val letterKey2 = LetterKey('A')
        assertThat(letterKey1.compareTo(letterKey2)).isEqualTo(0)
    }

    @Test
    fun letterKeys_areSorted_alphabetically() {
        val letterKey1 = LetterKey('A')
        val letterKey2 = LetterKey('B')
        assertThat(letterKey1 < letterKey2).isTrue()
    }
}
