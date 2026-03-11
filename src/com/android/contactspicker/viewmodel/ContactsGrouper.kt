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

import androidx.collection.LongObjectMap
import androidx.collection.emptyLongObjectMap
import com.android.contactspicker.Flags
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.SectionKey
import com.android.contactspicker.data.model.SectionKey.EmojiSection
import com.android.contactspicker.data.model.SectionKey.FavoriteSection
import com.android.contactspicker.data.model.SectionKey.LetterKey
import java.util.TreeMap

internal const val FALLBACK_SECTION_HEADER = "\u2026" // ellipsis

/** Helper class for grouping contacts into sections to be displayed on UI */
class ContactsGrouper(
    private val contacts: List<Contact>,
    private val contactGroupingMetadata: ContactGroupingMetadata,
) {

    /**
     * The contacts grouped into a [Map] by their [SectionKey] ordered in correct sequence to
     * display on UI
     *
     * Favorites are placed in a special [FavoriteSection]. Non-favorites are grouped by their
     * display name's initial letter or [EmojiSection] if no valid letter is found.
     */
    val availableContactsGroups: Map<SectionKey, List<Contact>> by lazy {
        if (Flags.enableContactGroupingUsingCp2()) {
            availableContactsGroupsUsingMetadata()
        } else {
            availableContactsGroupsCustom()
        }
    }

    private fun availableContactsGroupsCustom(): Map<SectionKey, List<Contact>> {
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

        return groups
    }

    private fun availableContactsGroupsUsingMetadata(): Map<SectionKey, List<Contact>> {
        val groups = LinkedHashMap<SectionKey, MutableList<Contact>>()
        groups[FavoriteSection] = mutableListOf()

        contacts.forEach { contact ->
            if (contact.isFavorite) {
                groups[FavoriteSection]?.add(contact)
            }

            val header = contactGroupingMetadata.contactIdToSectionMap[contact.id]
            val key = SectionKey.StringKey(header ?: FALLBACK_SECTION_HEADER)

            groups.getOrPut(key) { mutableListOf() }.add(contact)
        }

        if (groups[FavoriteSection].isNullOrEmpty()) {
            groups.remove(FavoriteSection)
        }

        return groups
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

/** Represents the metadata for grouping contacts, mapping each contact ID to its section header. */
data class ContactGroupingMetadata(val contactIdToSectionMap: LongObjectMap<String>) {
    companion object {
        val EMPTY = ContactGroupingMetadata(emptyLongObjectMap())
    }
}

/**
 * A container for contact data and its associated grouping information.
 *
 * @param contacts The list of contacts.
 * @param groupingMetadata The grouping information for the contacts list.
 */
data class GroupedContactsData(
    val contacts: List<Contact>,
    val groupingMetadata: ContactGroupingMetadata,
) {
    companion object {
        val EMPTY = GroupedContactsData(emptyList(), ContactGroupingMetadata.EMPTY)
    }
}
