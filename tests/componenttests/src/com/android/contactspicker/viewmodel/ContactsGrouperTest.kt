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

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.data.model.SectionKey.EmojiSection
import com.android.contactspicker.data.model.SectionKey.FavoriteSection
import com.android.contactspicker.data.model.SectionKey.LetterKey
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsGrouperTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun availableContactsGroups_emptyList_returnsEmptyMap() {
        val result = ContactsGrouper(emptyList()).availableContactsGroups
        assertThat(result).isEmpty()
    }

    @Test
    fun availableContactsGroups_alphabeticalContacts_groupsByLetter() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice")
        val bob = ContactTestDataFactory.createDisplayNameContact(2, "Bob")
        val charlie = ContactTestDataFactory.createDisplayNameContact(3, "Charlie")

        val result = ContactsGrouper(listOf(alice, bob, charlie)).availableContactsGroups

        assertThat(result).hasSize(3)
        assertThat(result[LetterKey('A')]).containsExactly(alice)
        assertThat(result[LetterKey('B')]).containsExactly(bob)
        assertThat(result[LetterKey('C')]).containsExactly(charlie)
    }

    @Test
    fun availableContactsGroups_sameInitial_groupsInSameSection() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice")
        val andrew = ContactTestDataFactory.createDisplayNameContact(2, "Andrew")

        val result = ContactsGrouper(listOf(alice, andrew)).availableContactsGroups

        assertThat(result).hasSize(1)
        assertThat(result[LetterKey('A')]).containsExactly(alice, andrew).inOrder()
    }

    @Test
    fun availableContactsGroups_favoriteContacts_appearsInFavoriteAndLetterSections() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice", isFavorite = true)
        val bob = ContactTestDataFactory.createDisplayNameContact(2, "Bob", isFavorite = false)

        val result = ContactsGrouper(listOf(alice, bob)).availableContactsGroups

        assertThat(result).hasSize(3) // Favorite, A, B
        assertThat(result[FavoriteSection]).containsExactly(alice)
        assertThat(result[LetterKey('A')]).containsExactly(alice)
        assertThat(result[LetterKey('B')]).containsExactly(bob)
    }

    @Test
    fun availableContactsGroups_emojiAndSpecialChars_groupsInEmojiSection() {
        val emojiContact = ContactTestDataFactory.createDisplayNameContact(1, "😀 Emoji")
        val specialCharContact = ContactTestDataFactory.createDisplayNameContact(2, "# Special")

        val result =
            ContactsGrouper(listOf(emojiContact, specialCharContact)).availableContactsGroups

        assertThat(result).hasSize(1)
        assertThat(result[EmojiSection]).containsExactly(emojiContact, specialCharContact).inOrder()
    }

    @Test
    fun availableContactsGroups_mixedContacts_maintainsOrderAndSections() {
        val alice = ContactTestDataFactory.createDisplayNameContact(1, "Alice", isFavorite = true)
        val bob = ContactTestDataFactory.createDisplayNameContact(2, "Bob")
        val emoji = ContactTestDataFactory.createDisplayNameContact(3, "😀")

        val result = ContactsGrouper(listOf(alice, bob, emoji)).availableContactsGroups

        assertThat(result.keys.toList())
            .containsExactly(FavoriteSection, EmojiSection, LetterKey('A'), LetterKey('B'))
            .inOrder()

        assertThat(result[FavoriteSection]).containsExactly(alice)
        assertThat(result[EmojiSection]).containsExactly(emoji)
        assertThat(result[LetterKey('A')]).containsExactly(alice)
        assertThat(result[LetterKey('B')]).containsExactly(bob)
    }
}
