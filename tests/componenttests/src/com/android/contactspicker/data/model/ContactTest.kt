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
import com.google.common.truth.Truth.assertThat
import java.lang.IllegalArgumentException
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactTest {

    private val testPhoneEntry = PhoneEntry(id = 1L, number = "123-456-7890", label = "Home")
    private val testEmailEntry =
        EmailEntry(id = 2L, address = "john.doe@example.com", label = "Home")

    @Test(expected = IllegalArgumentException::class)
    fun createDisplayNameContact_withBlankDisplayName_throwsException() {
        DisplayNameContact(id = 1, displayName = " ")
    }

    @Test
    fun createDisplayNameContact_withValidDisplayName_succeeds() {
        DisplayNameContact(id = 1, displayName = "John Doe")
    }

    @Test(expected = IllegalArgumentException::class)
    fun createPhoneContact_withBlankDisplayName_throwsException() {
        PhoneContact(id = 1, displayName = " ", phones = listOf(testPhoneEntry))
    }

    @Test(expected = IllegalArgumentException::class)
    fun createPhoneContact_withEmptyPhoneList_throwsException() {
        PhoneContact(id = 1, displayName = "John Doe", phones = emptyList())
    }

    @Test
    fun createPhoneContact_withValidData_succeeds() {
        PhoneContact(id = 1, displayName = "John Doe", phones = listOf(testPhoneEntry))
    }

    @Test
    fun createPhoneContact_withMultiplePhones_succeeds() {
        val anotherPhoneEntry = PhoneEntry(id = 3L, number = "098-765-4321", label = "Work")
        PhoneContact(
            id = 1,
            displayName = "John Doe",
            phones = listOf(testPhoneEntry, anotherPhoneEntry),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun createEmailContact_withBlankDisplayName_throwsException() {
        EmailContact(id = 1, displayName = " ", emails = listOf(testEmailEntry))
    }

    @Test(expected = IllegalArgumentException::class)
    fun createEmailContact_withEmptyEmailList_throwsException() {
        EmailContact(id = 1, displayName = "John Doe", emails = emptyList())
    }

    @Test
    fun createEmailContact_withValidData_succeeds() {
        EmailContact(id = 1, displayName = "John Doe", emails = listOf(testEmailEntry))
    }

    @Test
    fun createEmailContact_withMultipleEmails_succeeds() {
        val anotherEmailEntry = EmailEntry(id = 4L, address = "j.doe@work.com", label = "Work")
        EmailContact(
            id = 1,
            displayName = "John Doe",
            emails = listOf(testEmailEntry, anotherEmailEntry),
        )
    }

    @Test
    fun isFullySelected_withNullOrEmptySelectedEntries_returnsFalse() {
        val displayNameContact = DisplayNameContact(id = 1, displayName = "John Doe")
        val phoneContact =
            PhoneContact(id = 1, displayName = "John Doe", phones = listOf(testPhoneEntry))
        val emailContact =
            EmailContact(id = 1, displayName = "John Doe", emails = listOf(testEmailEntry))

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
        val contact = DisplayNameContact(id = 1, displayName = "John Doe")
        assertThat(contact.isFullySelected(setOf(1L))).isTrue()
    }

    @Test
    fun isFullySelected_forPhoneContact_withPartialAndFullSelection() {
        val anotherPhoneEntry = PhoneEntry(id = 3L, number = "098-765-4321", label = "Work")
        val contact =
            PhoneContact(
                id = 1,
                displayName = "John Doe",
                phones = listOf(testPhoneEntry, anotherPhoneEntry),
            )
        // A single phone contact is not fully selected
        assertThat(contact.isFullySelected(setOf(testPhoneEntry.id))).isFalse()
        // All phone contacts are selected
        assertThat(contact.isFullySelected(setOf(testPhoneEntry.id, anotherPhoneEntry.id))).isTrue()
    }

    @Test
    fun isFullySelected_forEmailContact_withPartialAndFullSelection() {
        val anotherEmailEntry = EmailEntry(id = 4L, address = "j.doe@work.com", label = "Work")
        val contact =
            EmailContact(
                id = 1,
                displayName = "John Doe",
                emails = listOf(testEmailEntry, anotherEmailEntry),
            )

        // A single email contact is not fully selected
        assertThat(contact.isFullySelected(setOf(testEmailEntry.id))).isFalse()

        // All email contacts are selected
        assertThat(contact.isFullySelected(setOf(testEmailEntry.id, anotherEmailEntry.id))).isTrue()
    }
}
