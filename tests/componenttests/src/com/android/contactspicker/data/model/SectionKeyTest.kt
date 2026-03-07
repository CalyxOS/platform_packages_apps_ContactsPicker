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
package com.android.contactspicker.data.model

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.data.model.SectionKey.EmojiSection
import com.android.contactspicker.data.model.SectionKey.FavoriteSection
import com.android.contactspicker.data.model.SectionKey.LetterKey
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
        val letterKey = LetterKey('A')

        assertThat(SectionKey.COMPARATOR.compare(FavoriteSection, EmojiSection)).isLessThan(0)
        assertThat(SectionKey.COMPARATOR.compare(FavoriteSection, letterKey)).isLessThan(0)
        assertThat(SectionKey.COMPARATOR.compare(EmojiSection, letterKey)).isLessThan(0)
    }

    @Test
    fun equality_isConsistent() {
        assertThat(SectionKey.COMPARATOR.compare(FavoriteSection, FavoriteSection)).isEqualTo(0)
        assertThat(SectionKey.COMPARATOR.compare(EmojiSection, EmojiSection)).isEqualTo(0)

        val letterKey1 = LetterKey('A')
        val letterKey2 = LetterKey('A')
        assertThat(SectionKey.COMPARATOR.compare(letterKey1, letterKey2)).isEqualTo(0)
    }

    @Test
    fun letterKeys_areSorted_alphabetically() {
        val letterKey1 = LetterKey('A')
        val letterKey2 = LetterKey('B')
        assertThat(SectionKey.COMPARATOR.compare(letterKey1, letterKey2)).isLessThan(0)
    }
}
