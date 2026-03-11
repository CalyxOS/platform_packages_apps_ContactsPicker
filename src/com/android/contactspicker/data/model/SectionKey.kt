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

/** A sealed class to represent the key for each section of the contacts list. */
sealed interface SectionKey {

    /** Unique ID used for LazyColumn keys to prevent collisions */
    val uniqueId: String

    /** Represents the favorite section of the contacts list. Comes at the top of the list. */
    data object FavoriteSection : SectionKey {
        override val uniqueId = "fav"
    }

    /**
     * Represents the emoji section of the contacts list and holds contacts starting with an emoji
     * or special characters. Comes after the favorite section and before any letter section.
     */
    data object EmojiSection : SectionKey {
        override val uniqueId = "emoji"
    }

    /** Represents a section of the contacts list that starts with a given letter. */
    @JvmInline
    value class LetterKey(val letter: Char) : SectionKey {
        override val uniqueId
            get() = "letter_$letter"
    }

    /** Represents a section of the contacts list with a string header. */
    @JvmInline
    value class StringKey(val header: String) : SectionKey {
        override val uniqueId
            get() = "string_$header"
    }

    // TODO(b/491763328): Remove COMPARATOR during enable_contact_grouping_using_cp2 flag cleanup
    companion object {
        val COMPARATOR =
            Comparator<SectionKey> { k1, k2 ->
                if (k1 is StringKey || k2 is StringKey) {
                    throw IllegalArgumentException("StringKey is not supported in COMPARATOR")
                }
                if (k1 == k2) return@Comparator 0
                val p1 = k1.toPriority()
                val p2 = k2.toPriority()
                if (p1 != p2) {
                    p1 - p2
                } else if (k1 is LetterKey && k2 is LetterKey) {
                    k1.letter.compareTo(k2.letter)
                } else {
                    0
                }
            }

        private fun SectionKey.toPriority(): Int =
            when (this) {
                is FavoriteSection -> 0
                is EmojiSection -> 1
                is LetterKey -> 2
                is StringKey ->
                    throw IllegalArgumentException("StringKey does not have a defined priority")
            }
    }
}
