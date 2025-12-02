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

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Star
import com.android.contactspicker.R

/**
 * A sealed class to represent the key for each section of the contacts list. It implements
 * Comparable to define a custom sorting order.
 */
sealed interface SectionKey : Comparable<SectionKey> {

    /** Unique ID used for LazyColumn keys to prevent collisions */
    val uniqueId: String

    /**
     * Internal priority for sorting. FavoriteIconKey comes first, followed by EmojiIconKey, and
     * then alphabetical.
     */
    val sortPriority: Int

    /** Represents the favorite icon section of the contacts list. Comes at the top of the list. */
    data object FavoriteIconKey : SectionKey {
        override val uniqueId = "fav"
        override val sortPriority = 0

        val icon = Icons.Filled.Star
        @StringRes val titleRes = R.string.contacts_picker_favorites_header
        @StringRes val contentDescriptionRes = R.string.favorites_header_icon_content_description
    }

    /**
     * Represents the emoji icon section of the contacts list and holds contacts starting with an
     * emoji or special characters. Comes after the favorite section and before any letter section.
     */
    data object EmojiIconKey : SectionKey {
        override val uniqueId = "emoji"
        override val sortPriority = 1

        val icon = Icons.Filled.Mood
        @StringRes val contentDescriptionRes = R.string.emoji_header_icon_content_description
    }

    /** Represents a section of the contacts list that starts with a given letter. */
    @JvmInline
    value class LetterKey(val letter: Char) : SectionKey {
        override val uniqueId
            get() = "letter_$letter"

        override val sortPriority
            get() = 2
    }

    override fun compareTo(other: SectionKey): Int {
        if (this.sortPriority != other.sortPriority) {
            return this.sortPriority - other.sortPriority
        }

        if (this is LetterKey && other is LetterKey) {
            return this.letter.compareTo(other.letter)
        }

        return 0
    }
}
