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

package com.android.contactspicker.data.repository

import android.app.UiAutomation
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.PhoneContact
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
@LargeTest
class ContactsRepositoryIntegrationTest {
    @get:Rule() val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contentResolver: ContentResolver = context.contentResolver
    private val repository = ContactsRepositoryImpl(context)
    private val rawContactIds = mutableListOf<Long>()

    private companion object {
        private const val TEST_CONTACT_NAME = "PickerTestContact"
        private const val TEST_CONTACT_EMAIL_1 = "pickertest1@example.com"
        private const val TEST_CONTACT_EMAIL_2 = "pickertest2@example.com"
        private const val TEST_CONTACT_PHONE_1 = "1234567890"
        private const val TEST_CONTACT_PHONE_2 = "0987654321"
    }

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.targetContext.packageName
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            "android.permission.READ_CONTACTS",
        )
    }

    @After
    fun tearDown() {
        withShellPermissions {
            rawContactIds.forEach { deleteContact(it) }
            rawContactIds.clear()
        }
    }

    @Test
    fun searchContacts_emailType_findsByEmailAndName() = runTest {
        insertContact(TEST_CONTACT_NAME, emails = listOf(TEST_CONTACT_EMAIL_1))

        // Search by email
        var result =
            repository.searchContacts(TEST_CONTACT_EMAIL_1, Intent.ACTION_PICK, Email.CONTENT_TYPE)
        assertEmailContactPresent(result, TEST_CONTACT_NAME, TEST_CONTACT_EMAIL_1)

        // Search by name
        result =
            repository.searchContacts(TEST_CONTACT_NAME, Intent.ACTION_PICK, Email.CONTENT_TYPE)
        assertContactNamePresent(result, TEST_CONTACT_NAME)
    }

    @Test
    fun searchContacts_emailMode_findByPrefix() = runTest {
        insertContact(TEST_CONTACT_NAME, emails = listOf(TEST_CONTACT_EMAIL_1))

        // Search by email
        var result =
            repository.searchContacts(
                TEST_CONTACT_EMAIL_1.substring(0, 4),
                Intent.ACTION_PICK,
                Email.CONTENT_TYPE,
            )
        assertEmailContactPresent(result, TEST_CONTACT_NAME, TEST_CONTACT_EMAIL_1)

        // Search by name
        result =
            repository.searchContacts(
                TEST_CONTACT_NAME.substring(0, 4),
                Intent.ACTION_PICK,
                Email.CONTENT_TYPE,
            )
        assertContactNamePresent(result, TEST_CONTACT_NAME)
    }

    @Test
    fun searchContacts_phoneType_findsByPhoneAndName() = runTest {
        insertContact(TEST_CONTACT_NAME, phones = listOf(TEST_CONTACT_PHONE_1))

        // Search by phone
        var result =
            repository.searchContacts(TEST_CONTACT_PHONE_1, Intent.ACTION_PICK, Phone.CONTENT_TYPE)
        assertPhoneContactPresent(result, TEST_CONTACT_NAME, TEST_CONTACT_PHONE_1)

        // Search by name
        result =
            repository.searchContacts(TEST_CONTACT_NAME, Intent.ACTION_PICK, Phone.CONTENT_TYPE)
        assertContactNamePresent(result, TEST_CONTACT_NAME)
    }

    @Test
    fun searchContacts_phoneMode_findByPrefix() = runTest {
        insertContact(TEST_CONTACT_NAME, phones = listOf(TEST_CONTACT_PHONE_1))

        // Search by phone
        var result =
            repository.searchContacts(
                TEST_CONTACT_PHONE_1.substring(0, 4),
                Intent.ACTION_PICK,
                Phone.CONTENT_TYPE,
            )
        assertPhoneContactPresent(result, TEST_CONTACT_NAME, TEST_CONTACT_PHONE_1)

        // Search by name
        result =
            repository.searchContacts(
                TEST_CONTACT_NAME.substring(0, 4),
                Intent.ACTION_PICK,
                Phone.CONTENT_TYPE,
            )
        assertContactNamePresent(result, TEST_CONTACT_NAME)
    }

    @Test
    fun searchContacts_displayNameMode_findsByName() = runTest {
        insertContact(TEST_CONTACT_NAME)
        val result =
            repository.searchContacts(
                TEST_CONTACT_NAME,
                Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_TYPE,
            )
        assertContactNamePresent(result, TEST_CONTACT_NAME)
    }

    @Test
    fun searchContacts_displayNameMode_findByPrefix() = runTest {
        insertContact(TEST_CONTACT_NAME)
        val result =
            repository.searchContacts(
                TEST_CONTACT_NAME.substring(0, 4),
                Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_TYPE,
            )
        assertContactNamePresent(result, TEST_CONTACT_NAME)
    }

    @Test
    fun searchContacts_displayNameMode_findsByEmail() = runTest {
        insertContact(TEST_CONTACT_NAME, emails = listOf(TEST_CONTACT_EMAIL_1))

        // Search by email
        val result =
            repository.searchContacts(
                TEST_CONTACT_EMAIL_1,
                Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_TYPE,
            )
        assertContactNamePresent(result, TEST_CONTACT_NAME)
    }

    @Test
    fun searchContacts_displayNameMode_findsByPhone() = runTest {
        insertContact(TEST_CONTACT_NAME, phones = listOf(TEST_CONTACT_PHONE_1))

        // Search by phone
        val result =
            repository.searchContacts(
                TEST_CONTACT_PHONE_1,
                Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_TYPE,
            )
        assertContactNamePresent(result, TEST_CONTACT_NAME)
    }

    @Test
    fun searchContacts_emailType_multipleEmails_findsContact() = runTest {
        insertContact(
            TEST_CONTACT_NAME,
            emails = listOf(TEST_CONTACT_EMAIL_1, TEST_CONTACT_EMAIL_2),
        )

        // Search by first email
        var result =
            repository.searchContacts(TEST_CONTACT_EMAIL_1, Intent.ACTION_PICK, Email.CONTENT_TYPE)
        assertEmailContactPresent(result, TEST_CONTACT_NAME, TEST_CONTACT_EMAIL_1)

        // Search by second email
        result =
            repository.searchContacts(TEST_CONTACT_EMAIL_2, Intent.ACTION_PICK, Email.CONTENT_TYPE)
        assertEmailContactPresent(result, TEST_CONTACT_NAME, TEST_CONTACT_EMAIL_2)

        // Search by name
        result =
            repository.searchContacts(TEST_CONTACT_NAME, Intent.ACTION_PICK, Email.CONTENT_TYPE)
        assertContactNamePresent(result, TEST_CONTACT_NAME)
        assertThat(result).hasSize(2)
        assertThat(
                result.find {
                    it is EmailContact && it.emails.any { it.address == TEST_CONTACT_EMAIL_1 }
                }
            )
            .isNotNull()
        assertThat(
                result.find {
                    it is EmailContact && it.emails.any { it.address == TEST_CONTACT_EMAIL_2 }
                }
            )
            .isNotNull()
    }

    @Test
    fun searchContacts_phoneType_multiplePhones_findsContact() = runTest {
        insertContact(
            TEST_CONTACT_NAME,
            phones = listOf(TEST_CONTACT_PHONE_1, TEST_CONTACT_PHONE_2),
        )

        // Search by first phone
        var result =
            repository.searchContacts(TEST_CONTACT_PHONE_1, Intent.ACTION_PICK, Phone.CONTENT_TYPE)
        assertPhoneContactPresent(result, TEST_CONTACT_NAME, TEST_CONTACT_PHONE_1)

        // Search by second phone
        result =
            repository.searchContacts(TEST_CONTACT_PHONE_2, Intent.ACTION_PICK, Phone.CONTENT_TYPE)
        assertPhoneContactPresent(result, TEST_CONTACT_NAME, TEST_CONTACT_PHONE_2)

        // Search by name
        result =
            repository.searchContacts(TEST_CONTACT_NAME, Intent.ACTION_PICK, Phone.CONTENT_TYPE)
        assertContactNamePresent(result, TEST_CONTACT_NAME)
        assertThat(result).hasSize(2)
        assertThat(
                result.find {
                    it is PhoneContact && it.phones.any { it.number == TEST_CONTACT_PHONE_1 }
                }
            )
            .isNotNull()
        assertThat(
                result.find {
                    it is PhoneContact && it.phones.any { it.number == TEST_CONTACT_PHONE_2 }
                }
            )
            .isNotNull()
    }

    @Test
    fun searchContacts_emailType_noMatch_returnsEmptyList() = runTest {
        val randomQuery = UUID.randomUUID().toString()
        val result = repository.searchContacts(randomQuery, Intent.ACTION_PICK, Email.CONTENT_TYPE)
        assertThat(result).isEmpty()
    }

    @Test
    fun searchContacts_phoneType_noMatch_returnsEmptyList() = runTest {
        val randomQuery = UUID.randomUUID().toString()
        val result = repository.searchContacts(randomQuery, Intent.ACTION_PICK, Phone.CONTENT_TYPE)
        assertThat(result).isEmpty()
    }

    private fun insertContact(
        displayName: String,
        emails: List<String> = emptyList(),
        phones: List<String> = emptyList(),
    ): Long {
        val ops = ArrayList<ContentProviderOperation>()

        ops.add(
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValues(ContentValues())
                .build()
        )

        ops.add(
            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, displayName)
                .build()
        )

        emails.forEach { email ->
            ops.add(
                ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                    .withValue(Email.ADDRESS, email)
                    .build()
            )
        }

        phones.forEach { phone ->
            ops.add(
                ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, phone)
                    .build()
            )
        }

        val results: Array<ContentProviderResult> = withShellPermissions {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        }
        assertThat(results.size).isEqualTo(ops.size)
        results.forEach { result -> assertThat(result.uri).isNotNull() }
        val rawContactUri = results[0].uri
        val rawContactId = rawContactUri?.lastPathSegment?.toLong() ?: -1
        if (rawContactId != -1L) {
            // Verify data insertion
            verifyDataInsertion(rawContactId, emails, phones)
            rawContactIds.add(rawContactId)
        }
        return rawContactId
    }

    private fun verifyDataInsertion(
        rawContactId: Long,
        expectedEmails: List<String>,
        expectedPhones: List<String>,
    ) {
        // Query for Emails
        val foundEmails = mutableListOf<String>()
        contentResolver
            .query(
                Email.CONTENT_URI,
                arrayOf(Email.ADDRESS),
                "${Email.RAW_CONTACT_ID} = ?",
                arrayOf(rawContactId.toString()),
                null,
            )
            ?.use { cursor ->
                val emailIndex = cursor.getColumnIndexOrThrow(Email.ADDRESS)
                while (cursor.moveToNext()) {
                    foundEmails.add(cursor.getString(emailIndex))
                }
            }
        assertThat(foundEmails).containsExactlyElementsIn(expectedEmails)

        // Query for Phones
        val foundPhones = mutableListOf<String>()
        contentResolver
            .query(
                Phone.CONTENT_URI,
                arrayOf(Phone.NUMBER),
                "${Phone.RAW_CONTACT_ID} = ?",
                arrayOf(rawContactId.toString()),
                null,
            )
            ?.use { cursor ->
                val phoneIndex = cursor.getColumnIndexOrThrow(Phone.NUMBER)
                while (cursor.moveToNext()) {
                    foundPhones.add(cursor.getString(phoneIndex))
                }
            }
        assertThat(foundPhones).containsExactlyElementsIn(expectedPhones)
    }

    private fun deleteContact(rawContactId: Long) {
        contentResolver.delete(
            RawContacts.CONTENT_URI,
            "${RawContacts._ID} = ?",
            arrayOf(rawContactId.toString()),
        )
    }

    private fun assertEmailContactPresent(
        result: List<Contact>,
        expectedName: String,
        expectedEmail: String,
    ) {
        assertThat(result).isNotEmpty()
        val contact =
            result.find {
                it.displayName == expectedName &&
                    it is EmailContact &&
                    it.emails.any { it.address == expectedEmail }
            }
        assertThat(contact).isNotNull()
    }

    private fun assertPhoneContactPresent(
        result: List<Contact>,
        expectedName: String,
        expectedPhone: String,
    ) {
        assertThat(result).isNotEmpty()
        val contact =
            result.find {
                it.displayName == expectedName &&
                    it is PhoneContact &&
                    it.phones.any { it.number == expectedPhone }
            }
        assertThat(contact).isNotNull()
    }

    private fun assertContactNamePresent(result: List<Contact>, expectedName: String) {
        assertThat(result).isNotEmpty()
        assertThat(result.any { it.displayName == expectedName }).isTrue()
    }

    /** Executes the given [block] of code with shell permissions. */
    fun <T> withShellPermissions(block: () -> T): T {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiAutomation: UiAutomation = instrumentation.uiAutomation
        uiAutomation.adoptShellPermissionIdentity()
        try {
            return block()
        } finally {
            uiAutomation.dropShellPermissionIdentity()
        }
    }
}
