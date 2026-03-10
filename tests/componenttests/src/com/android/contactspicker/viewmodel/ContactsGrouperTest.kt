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

import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.collection.mutableLongObjectMapOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.Flags
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.SectionKey.EmojiSection
import com.android.contactspicker.data.model.SectionKey.FavoriteSection
import com.android.contactspicker.data.model.SectionKey.LetterKey
import com.android.contactspicker.data.model.SectionKey.StringKey
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(android.content.flags.Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsGrouperTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_CONTACT_GROUPING_USING_CP2)
    fun availableContactsGroups_emptyList_returnsEmptyMap() {
        val result =
            ContactsGrouper(emptyList(), ContactGroupingMetadata.EMPTY).availableContactsGroups
        assertThat(result).isEmpty()
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_CONTACT_GROUPING_USING_CP2)
    fun availableContactsGroups_alphabeticalContacts_groupsByLetter() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice")
        val bob = ContactTestDataFactory.createDisplayNameContact(2, "Bob")
        val charlie = ContactTestDataFactory.createDisplayNameContact(3, "Charlie")

        val contacts = listOf(alice, bob, charlie)
        val contactsGrouper = ContactsGrouper(contacts, ContactGroupingMetadata.EMPTY)
        val result = contactsGrouper.availableContactsGroups

        assertThat(result).hasSize(3)
        assertThat(result[LetterKey('A')]).containsExactly(alice)
        assertThat(result[LetterKey('B')]).containsExactly(bob)
        assertThat(result[LetterKey('C')]).containsExactly(charlie)
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_CONTACT_GROUPING_USING_CP2)
    fun availableContactsGroups_sameInitial_groupsInSameSection() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice")
        val andrew = ContactTestDataFactory.createDisplayNameContact(2, "Andrew")

        val result =
            ContactsGrouper(listOf(alice, andrew), ContactGroupingMetadata.EMPTY)
                .availableContactsGroups

        assertThat(result).hasSize(1)
        assertThat(result[LetterKey('A')]).containsExactly(alice, andrew).inOrder()
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_CONTACT_GROUPING_USING_CP2)
    fun availableContactsGroups_favoriteContacts_appearsInFavoriteAndLetterSections() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice", isFavorite = true)
        val bob = ContactTestDataFactory.createDisplayNameContact(2, "Bob", isFavorite = false)

        val contacts = listOf(alice, bob)
        val contactsGrouper = ContactsGrouper(contacts, ContactGroupingMetadata.EMPTY)
        val result = contactsGrouper.availableContactsGroups

        assertThat(result).hasSize(3) // Favorite, A, B
        assertThat(result[FavoriteSection]).containsExactly(alice)
        assertThat(result[LetterKey('A')]).containsExactly(alice)
        assertThat(result[LetterKey('B')]).containsExactly(bob)
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_CONTACT_GROUPING_USING_CP2)
    fun availableContactsGroups_emojiAndSpecialChars_groupsInEmojiSection() {
        val emojiContact = ContactTestDataFactory.createDisplayNameContact(1, "😀 Emoji")
        val specialCharContact = ContactTestDataFactory.createDisplayNameContact(2, "# Special")

        val result =
            ContactsGrouper(listOf(emojiContact, specialCharContact), ContactGroupingMetadata.EMPTY)
                .availableContactsGroups

        assertThat(result).hasSize(1)
        assertThat(result[EmojiSection]).containsExactly(emojiContact, specialCharContact).inOrder()
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_CONTACT_GROUPING_USING_CP2)
    fun availableContactsGroups_mixedContacts_maintainsOrderAndSections() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice", isFavorite = true)
        val bob = ContactTestDataFactory.createDisplayNameContact(2, "Bob")
        val emoji = ContactTestDataFactory.createDisplayNameContact(3, "😀")

        val result =
            ContactsGrouper(listOf(alice, bob, emoji), ContactGroupingMetadata.EMPTY)
                .availableContactsGroups

        assertThat(result.keys.toList())
            .containsExactly(FavoriteSection, EmojiSection, LetterKey('A'), LetterKey('B'))
            .inOrder()

        assertThat(result[FavoriteSection]).containsExactly(alice)
        assertThat(result[EmojiSection]).containsExactly(emoji)
        assertThat(result[LetterKey('A')]).containsExactly(alice)
        assertThat(result[LetterKey('B')]).containsExactly(bob)
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_CONTACT_GROUPING_USING_CP2)
    fun availableContactsGroups_noFavorites_favoriteSectionNotPresent() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice", isFavorite = false)
        val contacts = listOf(alice)
        val contactsGrouper = ContactsGrouper(contacts, ContactGroupingMetadata.EMPTY)
        val result = contactsGrouper.availableContactsGroups

        assertThat(result.keys).doesNotContain(FavoriteSection)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_CONTACT_GROUPING_USING_CP2)
    fun availableContactsGroups_flagEnabled_noFavorites_favoriteSectionNotPresent() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice", isFavorite = false)
        val metadata =
            ContactGroupingMetadata(mutableLongObjectMapOf<String>().apply { put(1L, "Section 1") })
        val contacts = listOf(alice)
        val contactsGrouper = ContactsGrouper(contacts, metadata)
        val result = contactsGrouper.availableContactsGroups

        assertThat(result.keys).doesNotContain(FavoriteSection)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_CONTACT_GROUPING_USING_CP2)
    fun availableContactsGroups_flagEnabled_groupsUsingMetadata() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice")
        val bob = ContactTestDataFactory.createDisplayNameContact(2, "Bob")
        // Map Alice to Section 1 and Bob to Section 2
        val metadata =
            ContactGroupingMetadata(
                mutableLongObjectMapOf<String>().apply {
                    put(1L, "Section 1")
                    put(2L, "Section 2")
                }
            )

        val contacts = listOf(alice, bob)
        val contactsGrouper = ContactsGrouper(contacts, metadata)
        val result = contactsGrouper.availableContactsGroups

        assertThat(result).hasSize(2)
        assertThat(result[StringKey("Section 1")]).containsExactly(alice)
        assertThat(result[StringKey("Section 2")]).containsExactly(bob)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_CONTACT_GROUPING_USING_CP2)
    fun availableContactsGroups_flagEnabled_favorites_appearsInFavoriteAndMetadataSections() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice", isFavorite = true)
        val bob = ContactTestDataFactory.createDisplayNameContact(2, "Bob", isFavorite = false)
        val metadata =
            ContactGroupingMetadata(
                mutableLongObjectMapOf<String>().apply {
                    put(1L, "Section 1")
                    put(2L, "Section 2")
                }
            )

        val contacts = listOf(alice, bob)
        val contactsGrouper = ContactsGrouper(contacts, metadata)
        val result = contactsGrouper.availableContactsGroups

        assertThat(result).hasSize(3) // Favorite, Section 1, Section 2
        assertThat(result[FavoriteSection]).containsExactly(alice)
        assertThat(result[StringKey("Section 1")]).containsExactly(alice)
        assertThat(result[StringKey("Section 2")]).containsExactly(bob)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_CONTACT_GROUPING_USING_CP2)
    fun availableContactsGroups_flagEnabled_missingMetadata_groupsByFallbackHeader() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice")
        val bob = ContactTestDataFactory.createDisplayNameContact(2, "Bob")
        val metadata =
            ContactGroupingMetadata(
                mutableLongObjectMapOf<String>().apply { put(1L, "Section 1") }
            ) // Missing for Bob

        val contacts = listOf(alice, bob)
        val contactsGrouper = ContactsGrouper(contacts, metadata)
        val result = contactsGrouper.availableContactsGroups

        assertThat(result).hasSize(2)
        assertThat(result[StringKey("Section 1")]).containsExactly(alice)
        assertThat(result[StringKey(FALLBACK_SECTION_HEADER)]).containsExactly(bob)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_CONTACT_GROUPING_USING_CP2)
    fun availableContactsGroups_flagEnabled_emptyList() {
        val contacts = emptyList<Contact>()
        val contactsGrouper = ContactsGrouper(contacts, ContactGroupingMetadata.EMPTY)
        val result = contactsGrouper.availableContactsGroups

        assertThat(result).isEmpty()
    }
}
