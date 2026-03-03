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
package com.android.contactspicker.viewmodel

import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.SectionKey
import com.android.contactspicker.data.model.SectionKey.EmojiSection
import com.android.contactspicker.data.model.SectionKey.FavoriteSection
import com.android.contactspicker.data.model.SectionKey.LetterKey
import java.util.TreeMap

/** Helper class for grouping contacts into sections to be displayed on UI */
class ContactsGrouper(private val contacts: List<Contact>) {

    /**
     * The contacts grouped into a [Map] by their [SectionKey] ordered in correct sequence to
     * display on UI
     *
     * Favorites are placed in a special [FavoriteSection]. Non-favorites are grouped by their
     * display name's initial letter or [EmojiSection] if no valid letter is found.
     */
    val availableContactsGroups: Map<SectionKey, List<Contact>> by lazy {
        val favorites = mutableListOf<Contact>()
        val groups = TreeMap<SectionKey, MutableList<Contact>>(SectionKey.COMPARATOR)

        contacts.forEach { contact ->
            if (contact.isFavorite) {
                favorites.add(contact)
            }
            val key = contact.getDefaultSectionKey()
            groups.getOrPut(key) { mutableListOf() }.add(contact)
        }

        if (favorites.isNotEmpty()) {
            groups[FavoriteSection] = favorites
        }

        groups
    }

    /**
     * Returns the primary [SectionKey] where this contact belongs in the main list (e.g., a letter
     * or an emoji).
     *
     * Note: All contacts, including favorites, are assigned a default section key. Favorites will
     * appear in the section returned by this method in addition to being placed in the
     * [FavoriteSection].
     */
    internal fun Contact.getDefaultSectionKey(): SectionKey {
        val initial = getDisplayNameInitialLetter()
        return if (initial != null) {
            LetterKey(initial)
        } else {
            EmojiSection
        }
    }
}
