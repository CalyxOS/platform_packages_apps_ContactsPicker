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
package com.android.contactspicker.data.model

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.google.common.truth.Truth.assertThat
import java.lang.IllegalArgumentException
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactTest {

    @Test(expected = IllegalArgumentException::class)
    fun createDisplayNameContact_withBlankDisplayName_throwsException() {
        ContactTestDataFactory.createDisplayNameContact(id = 1, displayName = " ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun createDisplayNameContact_withBlankLookupKey_throwsException() {
        ContactTestDataFactory.createDisplayNameContact(
            id = 1,
            displayName = "John Doe",
            lookupKey = " ",
        )
    }

    @Test
    fun createDisplayNameContact_withValidData_succeeds() {
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        assertThat(contact.id).isEqualTo(1)
        assertThat(contact.displayName).isEqualTo("Alice Wonderland")
        assertThat(contact.lookupKey).isEqualTo("key_1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun createPhoneContact_withBlankDisplayName_throwsException() {
        ContactTestDataFactory.createPhoneContact(id = 1, displayName = " ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun createPhoneContact_withEmptyPhoneList_throwsException() {
        ContactTestDataFactory.createPhoneContact(
            id = 1,
            displayName = "John Doe",
            phones = emptyList(),
        )
    }

    @Test
    fun createPhoneContact_withValidData_succeeds() {
        val contact = ContactTestDataFactory.GENERIC_PHONE_CONTACT
        assertThat(contact.id).isEqualTo(2)
        assertThat(contact.displayName).isEqualTo("Bob the Builder")
        assertThat(contact.phones).isNotEmpty()
    }

    @Test
    fun createPhoneContact_withMultiplePhones_succeeds() {
        val contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        assertThat(contact.phones).hasSize(3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun createEmailContact_withBlankDisplayName_throwsException() {
        ContactTestDataFactory.createEmailContact(id = 1, displayName = " ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun createEmailContact_withEmptyEmailList_throwsException() {
        ContactTestDataFactory.createEmailContact(
            id = 1,
            displayName = "John Doe",
            emails = emptyList(),
        )
    }

    @Test
    fun createEmailContact_withValidData_succeeds() {
        val contact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        assertThat(contact.id).isEqualTo(3)
        assertThat(contact.displayName).isEqualTo("Charlie Chaplin")
        assertThat(contact.emails).isNotEmpty()
    }

    @Test
    fun createEmailContact_withMultipleEmails_succeeds() {
        val contact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        assertThat(contact.emails).hasSize(3)
    }

    @Test
    fun isFullySelected_withNullOrEmptySelectedEntries_returnsFalse() {
        val displayNameContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        val phoneContact = ContactTestDataFactory.GENERIC_PHONE_CONTACT
        val emailContact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT

        // Test with null set
        assertThat(displayNameContact.isFullySelected(null)).isFalse()
        assertThat(phoneContact.isFullySelected(null)).isFalse()
        assertThat(emailContact.isFullySelected(null)).isFalse()

        // Test with empty set
        assertThat(displayNameContact.isFullySelected(emptySet())).isFalse()
        assertThat(phoneContact.isFullySelected(emptySet())).isFalse()
        assertThat(emailContact.isFullySelected(emptySet())).isFalse()
    }

    @Test
    fun isFullySelected_forDisplayNameContact_withNonEmptySelectedEntries_returnsTrue() {
        val contact = ContactTestDataFactory.GENERIC_PHONE_CONTACT

        assertThat(contact.isFullySelected(setOf(1L))).isTrue()
    }

    @Test
    fun isFullySelected_forPhoneContact_withPartialAndFullSelection() {
        val phoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        // A single phone contact is not fully selected
        assertThat(phoneContact.isFullySelected(setOf(phoneContact.phones.first().id))).isFalse()
        // All phone contacts are selected
        assertThat(phoneContact.isFullySelected(phoneContact.phones.map { it.id }.toSet())).isTrue()
    }

    @Test
    fun isFullySelected_forEmailContact_withPartialAndFullSelection() {
        val emailContact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT

        // A single email contact is not fully selected
        assertThat(emailContact.isFullySelected(setOf(emailContact.emails.first().id))).isFalse()

        // All email contacts are selected
        assertThat(emailContact.isFullySelected(emailContact.emails.map { it.id }.toSet())).isTrue()
    }

    @Test
    fun getDisplayNameInitialLetter_startsWithCapitalLetter() {
        assertThat(
                ContactTestDataFactory.createDisplayNameContact(1, "Alice")
                    .getDisplayNameInitialLetter()
            )
            .isEqualTo('A')
    }

    @Test
    fun getDisplayNameInitialLetter_startsWithLowercaseLetter() {
        assertThat(
                ContactTestDataFactory.createDisplayNameContact(123, ("bob"))
                    .getDisplayNameInitialLetter()
            )
            .isEqualTo('B')
    }

    @Test
    fun getDisplayNameInitialLetter_startsWithNumber() {
        assertThat(
                ContactTestDataFactory.createDisplayNameContact(123, "123 Contact")
                    .getDisplayNameInitialLetter()
            )
            .isNull()
    }

    @Test
    fun getDisplayNameInitialLetter_startsWithSymbol() {
        assertThat(
                ContactTestDataFactory.createDisplayNameContact(1, "#hashtag")
                    .getDisplayNameInitialLetter()
            )
            .isNull()
    }

    @Test
    fun getDisplayNameInitialLetter_startsWithEmoji() {
        assertThat(
                ContactTestDataFactory.createDisplayNameContact(1, "😊")
                    .getDisplayNameInitialLetter()
            )
            .isNull()
    }
}
