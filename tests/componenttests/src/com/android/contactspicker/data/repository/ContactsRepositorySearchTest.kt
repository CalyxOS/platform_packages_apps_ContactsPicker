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
import android.test.mock.MockContentResolver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.config.ContactsQueryMode
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.fakes.FakeContentProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsRepositorySearchTest {
    @get:Rule() val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

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
    fun searchContacts_inEmailMode_returnsEmailContacts() = runTest {
        val cursor =
            MatrixCursor(
                arrayOf(
                    Email.CONTACT_ID,
                    Email.DISPLAY_NAME_PRIMARY,
                    Email.PHOTO_THUMBNAIL_URI,
                    Email.ADDRESS,
                    Email._ID,
                )
            )
        cursor.addRow(arrayOf<Any?>(1L, "John Doe", null, "john.doe@example.com", 101L))
        val query = "john"
        val filterUri = Email.CONTENT_FILTER_URI.buildUpon().appendPath(query).build()
        fakeContentProvider.setCursorForUri(filterUri, cursor)

        val contacts = repository.searchContacts(query, ContactsQueryMode.EmailsOnly)

        assertThat(contacts).hasSize(1)
        val contact = contacts.first() as com.android.contactspicker.data.model.EmailContact
        assertThat(contact.id).isEqualTo(1L)
        assertThat(contact.displayName).isEqualTo("John Doe")
        assertThat(contact.emails).hasSize(1)
        with(contact.emails.first()) {
            assertThat(id).isEqualTo(101L)
            assertThat(address).isEqualTo("john.doe@example.com")
        }
    }

    @Test
    fun searchContacts_inPhoneMode_returnsPhoneContacts() = runTest {
        val cursor =
            MatrixCursor(
                arrayOf(
                    Phone.CONTACT_ID,
                    Phone.DISPLAY_NAME_PRIMARY,
                    Phone.PHOTO_THUMBNAIL_URI,
                    Phone.NUMBER,
                    Phone._ID,
                )
            )
        cursor.addRow(arrayOf<Any?>(2L, "Jane Doe", null, "123-456-7890", 102L))
        val query = "jane"
        val filterUri = Phone.CONTENT_FILTER_URI.buildUpon().appendPath(query).build()
        fakeContentProvider.setCursorForUri(filterUri, cursor)

        val contacts = repository.searchContacts(query, ContactsQueryMode.PhonesOnly)

        assertThat(contacts).hasSize(1)
        val contact = contacts.first() as com.android.contactspicker.data.model.PhoneContact
        assertThat(contact.id).isEqualTo(2L)
        assertThat(contact.displayName).isEqualTo("Jane Doe")
        assertThat(contact.phones).hasSize(1)
        with(contact.phones.first()) {
            assertThat(id).isEqualTo(102L)
            assertThat(number).isEqualTo("123-456-7890")
        }
    }

    @Test
    fun searchContacts_inDisplayNameMode_returnsDisplayNameContacts() = runTest {
        val cursor =
            MatrixCursor(
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                    ContactsContract.Contacts.LOOKUP_KEY,
                )
            )
        cursor.addRow(arrayOf<Any?>(3L, "Alice Smith", null, "lookupKeyAlice"))
        val query = "alice"
        val filterUri =
            ContactsContract.Contacts.CONTENT_FILTER_URI.buildUpon().appendPath(query).build()
        fakeContentProvider.setCursorForUri(filterUri, cursor)

        val contacts = repository.searchContacts(query, ContactsQueryMode.DisplayNamesOnly)

        assertThat(contacts).hasSize(1)
        val contact = contacts.first() as DisplayNameContact
        assertThat(contact.id).isEqualTo(3L)
        assertThat(contact.displayName).isEqualTo("Alice Smith")
        assertThat(contact.lookupKey).isEqualTo("lookupKeyAlice")
    }

    @Test
    fun searchContacts_emptyQuery_returnsEmptyList() = runTest {
        val contacts = repository.searchContacts("", ContactsQueryMode.PhonesOnly)
        assertThat(contacts).isEmpty()
    }

    @Test
    fun searchContacts_whitespaceQuery_returnsEmptyList() = runTest {
        val contacts = repository.searchContacts("   ", ContactsQueryMode.EmailsOnly)
        assertThat(contacts).isEmpty()
    }

    @Test
    fun searchContacts_noMatch_returnsEmptyList() = runTest {
        val cursor = MatrixCursor(arrayOf(Phone.CONTACT_ID)) // Empty cursor
        val query = "nomatch"
        val filterUri = Phone.CONTENT_FILTER_URI.buildUpon().appendPath(query).build()
        fakeContentProvider.setCursorForUri(filterUri, cursor)

        val contacts = repository.searchContacts(query, ContactsQueryMode.PhonesOnly)
        assertThat(contacts).isEmpty()
    }
}
