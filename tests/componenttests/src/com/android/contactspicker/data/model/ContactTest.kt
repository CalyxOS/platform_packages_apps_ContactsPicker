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
}
