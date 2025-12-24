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

import android.content.Context
import android.content.flags.Flags
import android.content.pm.ProviderInfo
import android.database.MatrixCursor
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.test.mock.MockContentResolver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.config.ContactsQueryMode
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.MimeType
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.fakes.FakeContentProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [ContactsRepository].
 *
 * This test class uses a Parameterized runner to test multiple intent logic paths.
 */
@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsRepositoryImplTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockContext: Context = mock()

    private val fakeContentProvider = FakeContentProvider()
    private val mockContentResolver = MockContentResolver()
    private lateinit var repository: ContactsRepository

    @Before
    fun setUp() {
        val providerInfo = ProviderInfo().apply { authority = ContactsContract.AUTHORITY }
        fakeContentProvider.attachInfo(mockContext, providerInfo)

        mockContentResolver.addProvider(ContactsContract.AUTHORITY, fakeContentProvider)
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
        whenever(mockContext.resources).thenReturn(context.resources)
        repository = ContactsRepositoryImpl(mockContext)
    }

    @Test
    fun getContacts_displayNamesOnlyMode_returnsDisplayNamesContacts() = runTest {
        val queryMode = ContactsQueryMode.DisplayNamesOnly
        val uriToExpect = Contacts.CONTENT_URI
        val cursor =
            MatrixCursor(
                arrayOf(
                    Contacts._ID,
                    Contacts.DISPLAY_NAME_PRIMARY,
                    Contacts.STARRED,
                    Contacts.PHOTO_THUMBNAIL_URI,
                    Contacts.LOOKUP_KEY,
                )
            )
        cursor.addRow(arrayOf<Any?>(1L, "Test Contact", 0, null, "contact_lookup_key"))

        fakeContentProvider.setCursorForUri(uriToExpect, cursor)
        val contacts = repository.getContacts(queryMode)

        assertThat(contacts).isNotEmpty()
        val contact = contacts.first()
        assertThat(contact).isInstanceOf(DisplayNameContact::class.java)
    }

    @Test
    fun getContacts_displayNamesOnlyMode_noName_returnsNoName() = runTest {
        val queryMode = ContactsQueryMode.DisplayNamesOnly
        val uriToExpect = Contacts.CONTENT_URI
        val cursor =
            MatrixCursor(
                arrayOf(
                    Contacts._ID,
                    Contacts.DISPLAY_NAME_PRIMARY,
                    Contacts.STARRED,
                    Contacts.PHOTO_THUMBNAIL_URI,
                    Contacts.LOOKUP_KEY,
                )
            )
        cursor.addRow(arrayOf<Any?>(1L, null, 0, null, "contact_lookup_key"))

        fakeContentProvider.setCursorForUri(uriToExpect, cursor)
        val contacts = repository.getContacts(queryMode)

        assertThat(contacts).isNotEmpty()
        val contact = contacts.first()
        assertThat(contact).isInstanceOf(DisplayNameContact::class.java)
        assertThat(contact.displayName).isEqualTo("(No name)")
    }

    @Test
    fun getContacts_emailsOnlyMode_returnsEmailContacts() = runTest {
        val queryMode = ContactsQueryMode.EmailsOnly
        val uriToExpect = Email.CONTENT_URI
        val cursor =
            MatrixCursor(
                arrayOf(
                    Email.CONTACT_ID,
                    Email.DISPLAY_NAME_PRIMARY,
                    Email.STARRED,
                    Email.PHOTO_THUMBNAIL_URI,
                    Email.ADDRESS,
                    Email._ID,
                    Email.TYPE,
                    Email.LABEL,
                )
            )

        cursor.addRow(
            arrayOf<Any?>(
                1L,
                "Test Contact",
                0,
                null,
                "test@example.com",
                101L,
                Email.TYPE_HOME,
                null,
            )
        )

        fakeContentProvider.setCursorForUri(uriToExpect, cursor)
        val contacts = repository.getContacts(queryMode)

        assertThat(contacts).isNotEmpty()
        val contact = contacts.first()
        assertThat(contact).isInstanceOf(EmailContact::class.java)
    }

    @Test
    fun getContacts_phonesOnlyMode_returnsPhoneContacts() = runTest {
        val queryMode = ContactsQueryMode.PhonesOnly
        val uriToExpect = Phone.CONTENT_URI
        val cursor =
            MatrixCursor(
                arrayOf(
                    Phone.CONTACT_ID,
                    Phone.DISPLAY_NAME_PRIMARY,
                    Phone.STARRED,
                    Phone.PHOTO_THUMBNAIL_URI,
                    Phone.NUMBER,
                    Phone._ID,
                    Phone.TYPE,
                    Phone.LABEL,
                )
            )

        cursor.addRow(
            arrayOf<Any?>(1L, "Test Contact", 0, null, "555-0123", 101L, Phone.TYPE_HOME, null)
        )

        fakeContentProvider.setCursorForUri(uriToExpect, cursor)
        val contacts = repository.getContacts(queryMode)

        assertThat(contacts).isNotEmpty()
        val contact = contacts.first()
        assertThat(contact).isInstanceOf(PhoneContact::class.java)
    }

    @Test
    fun getContacts_customMode_returnsDisplayNamesContacts() = runTest {
        val mimeTypes = listOf(MimeType.EMAIL, MimeType.PHONE)
        val matchAll = true
        val queryMode = ContactsQueryMode.Custom(mimeTypes, matchAll)

        val expectedUri =
            ContactsContract.AUTHORITY_URI.buildUpon()
                .appendPath("contacts_data")
                .appendQueryParameter(
                    Contacts.REQUESTED_MIMETYPES_PARAM_KEY,
                    mimeTypes.joinToString(",") { it.value },
                )
                .appendQueryParameter(Contacts.MATCH_ALL_MIMETYPES_PARAM_KEY, "true")
                .build()

        val cursor =
            MatrixCursor(
                arrayOf(
                    Contacts._ID,
                    Contacts.DISPLAY_NAME_PRIMARY,
                    Contacts.STARRED,
                    Contacts.PHOTO_THUMBNAIL_URI,
                    Contacts.LOOKUP_KEY,
                )
            )
        cursor.addRow(arrayOf<Any?>(1L, "Test Contact", 0, null, "contact_lookup_key"))

        fakeContentProvider.setCursorForUri(expectedUri, cursor)
        val contacts = repository.getContacts(queryMode)

        assertThat(contacts).isNotEmpty()
        val contact = contacts.first()
        assertThat(contact).isInstanceOf(DisplayNameContact::class.java)
        assertThat(contact.displayName).isEqualTo("Test Contact")
    }

    @Test
    fun searchContacts_customMode_returnsDisplayNamesContacts() = runTest {
        val query = "Test"
        val mimeTypes = listOf(MimeType.PHOTO)
        val matchAll = false
        val queryMode = ContactsQueryMode.Custom(mimeTypes, matchAll)

        val expectedUri =
            ContactsContract.AUTHORITY_URI.buildUpon()
                .appendPath("contacts_data/filter")
                .appendPath(query)
                .appendQueryParameter(
                    Contacts.REQUESTED_MIMETYPES_PARAM_KEY,
                    mimeTypes.joinToString(",") { it.value },
                )
                .appendQueryParameter(Contacts.MATCH_ALL_MIMETYPES_PARAM_KEY, "false")
                .build()

        val cursor =
            MatrixCursor(
                arrayOf(
                    Contacts._ID,
                    Contacts.DISPLAY_NAME_PRIMARY,
                    Contacts.STARRED,
                    Contacts.PHOTO_THUMBNAIL_URI,
                    Contacts.LOOKUP_KEY,
                )
            )
        cursor.addRow(arrayOf<Any?>(1L, "Test Contact", 0, null, "contact_lookup_key"))

        fakeContentProvider.setCursorForUri(expectedUri, cursor)
        val contacts = repository.searchContacts(query, queryMode)

        assertThat(contacts).isNotEmpty()
        val contact = contacts.first()
        assertThat(contact.displayName).isEqualTo("Test Contact")
    }

    @Test
    fun getContactsForIntent_groupsMultipleEntriesForSameContact() = runTest {
        val queryMode = ContactsQueryMode.PhonesOnly
        val cursor =
            MatrixCursor(
                arrayOf(
                    Phone.CONTACT_ID,
                    Phone.DISPLAY_NAME_PRIMARY,
                    Phone.STARRED,
                    Phone.PHOTO_THUMBNAIL_URI,
                    Phone.NUMBER,
                    Phone._ID,
                    Phone.TYPE,
                    Phone.LABEL,
                )
            )
        cursor.addRow(
            arrayOf<Any?>(1L, "Test Contact", 0, null, "555-0123", 101L, Phone.TYPE_HOME, null)
        )
        cursor.addRow(
            arrayOf<Any?>(1L, "Test Contact2", 0, null, "555-0124", 102L, Phone.TYPE_WORK, null)
        )

        fakeContentProvider.setCursorForUri(Phone.CONTENT_URI, cursor)

        val contacts = repository.getContacts(queryMode)

        assertThat(contacts).hasSize(1)
        val phoneContact = contacts.first() as PhoneContact
        assertThat(phoneContact.phones).hasSize(2)
        assertThat(phoneContact.phones[0].number).isEqualTo("555-0123")
        assertThat(phoneContact.phones[0].label).isEqualTo("Home")
        assertThat(phoneContact.phones[1].number).isEqualTo("555-0124")
        assertThat(phoneContact.phones[1].label).isEqualTo("Work")
    }

    @Test
    fun getContactsForIntent_passesUriValueCorrectly() = runTest {
        val queryMode = ContactsQueryMode.PhonesOnly
        val cursor =
            MatrixCursor(
                arrayOf(
                    Phone.CONTACT_ID,
                    Phone.DISPLAY_NAME_PRIMARY,
                    Phone.PHOTO_THUMBNAIL_URI,
                    Phone.STARRED,
                    Phone.NUMBER,
                    Phone._ID,
                    Phone.TYPE,
                    Phone.LABEL,
                )
            )
        val fakeUri = "content://fake/uri/123"
        cursor.addRow(
            arrayOf<Any?>(1L, "Test Contact", null, 0, "555-0123", 101L, Phone.TYPE_HOME, null)
        )
        cursor.addRow(
            arrayOf<Any?>(2L, "Test Contact2", fakeUri, 0, "555-0124", 102L, Phone.TYPE_WORK, null)
        )

        fakeContentProvider.setCursorForUri(Phone.CONTENT_URI, cursor)

        val contacts = repository.getContacts(queryMode)

        assertThat(contacts).hasSize(2)
        assertThat((contacts[0] as PhoneContact).profilePictureUri).isNull()
        assertThat((contacts[1] as PhoneContact).profilePictureUri).isEqualTo(fakeUri)
    }

    @Test
    fun getDataRowIds_returnsCorrectDataIds_forContactsAndMimeTypes() = runTest {
        val contactId = 1L
        val emailDataId = 101L
        val phoneDataId = 102L
        val mimeTypes = listOf(MimeType.EMAIL, MimeType.PHONE)

        val cursor = MatrixCursor(arrayOf(Data._ID))
        cursor.addRow(arrayOf(emailDataId))
        cursor.addRow(arrayOf(phoneDataId))

        fakeContentProvider.setCursorForUri(Data.CONTENT_URI, cursor)

        val result = repository.getDataRowIds(contactIds = listOf(contactId), mimeTypes = mimeTypes)

        assertThat(result).containsExactly(emailDataId, phoneDataId)
    }
}
