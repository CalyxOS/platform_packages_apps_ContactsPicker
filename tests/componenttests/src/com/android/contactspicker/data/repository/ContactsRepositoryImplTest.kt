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

import android.app.ActivityManager
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.flags.Flags
import android.content.pm.ProviderInfo
import android.content.res.Resources
import android.database.MatrixCursor
import android.os.Bundle
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.test.mock.MockContentResolver
import androidx.collection.mutableLongObjectMapOf
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.bedstead.nene.TestApis
import com.android.contactspicker.R
import com.android.contactspicker.config.ContactsQueryMode
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.MimeType
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.fakes.FakeContentProvider
import com.android.contactspicker.viewmodel.FALLBACK_SECTION_HEADER
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for [ContactsRepository].
 *
 * This test class uses a Parameterized runner to test multiple intent logic paths.
 */
@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsRepositoryImplTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val realContext: Context = ApplicationProvider.getApplicationContext()
    private val fakeContentProvider = FakeContentProvider()
    private val mockContentResolver = MockContentResolver()

    private var currentUserId: Int = 0

    private lateinit var contextWrapper: Context
    private lateinit var repository: ContactsRepository

    companion object {
        private const val CROSS_PROFILE_USER_ID = 10
        private const val CROSS_PROFILE_CONTACT_NAME = "Cross-Profile User Contact"
        private const val CROSS_PROFILE_CONTACT_ID = 99L
        private const val CROSS_PROFILE_LOOKUP_KEY = "lookup_key_99"
        private const val CROSS_PROFILE_SEARCH_RESULT_NAME = "Cross-Profile Result"
        private const val CROSS_PROFILE_SEARCH_RESULT_ID = 88L
        private const val CROSS_PROFILE_SEARCH_LOOKUP_KEY = "lookup_key_88"
        private const val CROSS_PROFILE_DATA_CONTACT_ID = 55L
        private const val CROSS_PROFILE_DATA_ROW_ID = 550L
        private const val TEST_CONTACT_ID = 1L
        private const val TEST_CONTACT_NAME = "Test Contact"
        private const val TEST_CONTACT_NAME_2 = "Test Contact2"
        private const val TEST_CONTACT_DATA_ID_1 = 101L
        private const val TEST_CONTACT_DATA_ID_2 = 102L
        private const val TEST_EMAIL = "test@example.com"
        private const val TEST_PHONE_1 = "555-0123"
        private const val TEST_PHONE_2 = "555-0124"
        private const val DISPLAY_NAME_SOURCE = ContactsContract.DisplayNameSources.STRUCTURED_NAME

        private val SECTION_TITLES = arrayOf("A", "B", "C")
        private val SECTION_COUNTS = intArrayOf(1, 1, 1)
    }

    @Before
    fun setUp() {
        TestApis.permissions()
            .withPermission(android.Manifest.permission.INTERACT_ACROSS_USERS)
            .use { currentUserId = ActivityManager.getCurrentUser() }

        val providerInfo = ProviderInfo().apply { authority = ContactsContract.AUTHORITY }
        fakeContentProvider.attachInfo(realContext, providerInfo)

        mockContentResolver.addProvider(
            "$currentUserId@${ContactsContract.AUTHORITY}",
            fakeContentProvider,
        )
        mockContentResolver.addProvider(
            "$CROSS_PROFILE_USER_ID@${ContactsContract.AUTHORITY}",
            fakeContentProvider,
        )

        contextWrapper =
            object : ContextWrapper(realContext) {
                override fun getContentResolver(): ContentResolver? {
                    return mockContentResolver
                }

                override fun getResources(): Resources? {
                    return realContext.resources
                }
            }
        repository = ContactsRepositoryImpl(contextWrapper)
    }

    private fun createCursorWithExtras(cursor: MatrixCursor): MatrixCursor {
        val extras =
            Bundle().apply {
                putStringArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_TITLES, SECTION_TITLES)
                putIntArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS, SECTION_COUNTS)
            }
        cursor.extras = extras
        return cursor
    }

    @Test
    fun getContacts_displayNamesOnlyMode_returnsDisplayNamesContacts() = runTest {
        val queryMode = ContactsQueryMode.DisplayNamesOnly
        val uriToExpect =
            Contacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor = createCursorWithExtras(createDisplayNameCursor().apply { addDisplayNameRow() })
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.contacts).isNotEmpty()
        val contact = result.contacts.first()
        assertThat(contact).isInstanceOf(DisplayNameContact::class.java)
        assertThat(fakeContentProvider.capturedUris)
            .contains(ContentProvider.maybeAddUserId(uriToExpect, currentUserId))
    }

    @Test
    fun getContacts_displayNamesOnlyMode_parsesGroupMetadata() = runTest {
        val queryMode = ContactsQueryMode.DisplayNamesOnly
        val uriToExpect =
            Contacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            createCursorWithExtras(
                createDisplayNameCursor().apply {
                    addDisplayNameRow(id = 1L, displayName = "Apple")
                    addDisplayNameRow(id = 2L, displayName = "Banana")
                    addDisplayNameRow(id = 3L, displayName = "Cherry")
                }
            )
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.groupingMetadata).isNotNull()
        val expectedMap =
            mutableLongObjectMapOf<String>().apply {
                put(1L, "A")
                put(2L, "B")
                put(3L, "C")
            }
        assertThat(result.groupingMetadata.contactIdToSectionMap).isEqualTo(expectedMap)
    }

    @Test
    fun getContacts_mismatchedIndexCounts_usesFallbackHeader() = runTest {
        val queryMode = ContactsQueryMode.DisplayNamesOnly
        val uriToExpect =
            Contacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor = MatrixCursor(ContactsRepositoryImpl.DISPLAY_NAME_FETCH_PROJECTION)
        cursor.addRow(arrayOf<Any?>(1L, "Apple", null, 0, "lookup_1", DISPLAY_NAME_SOURCE))
        cursor.addRow(arrayOf<Any?>(2L, "Banana", null, 0, "lookup_2", DISPLAY_NAME_SOURCE))
        val extras =
            Bundle().apply {
                putStringArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_TITLES, arrayOf("A"))
                putIntArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS, intArrayOf(1))
            }
        cursor.extras = extras
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        val expectedMap =
            mutableLongObjectMapOf<String>().apply {
                put(1L, "A")
                put(2L, FALLBACK_SECTION_HEADER)
            }
        assertThat(result.groupingMetadata.contactIdToSectionMap).isEqualTo(expectedMap)
    }

    @Test
    fun getContacts_displayNamesOnlyMode_noName_returnsNoName() = runTest {
        val queryMode = ContactsQueryMode.DisplayNamesOnly
        val uriToExpect =
            Contacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            createCursorWithExtras(
                createDisplayNameCursor().apply { addDisplayNameRow(displayName = null) }
            )
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.contacts).isNotEmpty()
        val contact = result.contacts.first()
        assertThat(contact).isInstanceOf(DisplayNameContact::class.java)
        assertThat(contact.displayName)
            .isEqualTo(realContext.getString(R.string.no_name_placeholder))
    }

    @Test
    fun getContacts_emailsOnlyMode_returnsEmailContacts() = runTest {
        val queryMode = ContactsQueryMode.EmailsOnly
        val uriToExpect =
            Email.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor = createCursorWithExtras(createEmailCursor().apply { addEmailRow() })
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.contacts).isNotEmpty()
        val contact = result.contacts.first()
        assertThat(contact).isInstanceOf(EmailContact::class.java)
        assertThat(fakeContentProvider.capturedUris)
            .contains(ContentProvider.maybeAddUserId(uriToExpect, currentUserId))
    }

    @Test
    fun getContacts_emailsOnlyMode_parsesGroupMetadata() = runTest {
        val queryMode = ContactsQueryMode.EmailsOnly
        val uriToExpect =
            Email.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            createCursorWithExtras(
                createEmailCursor().apply {
                    addEmailRow(id = 1L, displayName = "Apple")
                    addEmailRow(id = 2L, displayName = "Banana")
                    addEmailRow(id = 3L, displayName = "Cherry")
                }
            )
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.groupingMetadata).isNotNull()
        val expectedMap =
            mutableLongObjectMapOf<String>().apply {
                put(1L, "A")
                put(2L, "B")
                put(3L, "C")
            }
        assertThat(result.groupingMetadata.contactIdToSectionMap).isEqualTo(expectedMap)
    }

    @Test
    fun getContacts_phonesOnlyMode_returnsPhoneContacts() = runTest {
        val queryMode = ContactsQueryMode.PhonesOnly
        val uriToExpect =
            Phone.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor = createCursorWithExtras(createPhoneCursor().apply { addPhoneRow() })
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.contacts).isNotEmpty()
        val contact = result.contacts.first()
        assertThat(contact).isInstanceOf(PhoneContact::class.java)
        assertThat(fakeContentProvider.capturedUris)
            .contains(ContentProvider.maybeAddUserId(uriToExpect, currentUserId))
    }

    @Test
    fun getContacts_phonesOnlyMode_parsesGroupMetadata() = runTest {
        val queryMode = ContactsQueryMode.PhonesOnly
        val uriToExpect =
            Phone.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            createCursorWithExtras(
                createPhoneCursor().apply {
                    addPhoneRow(id = 1L, displayName = "Apple")
                    addPhoneRow(id = 2L, displayName = "Banana")
                    addPhoneRow(id = 3L, displayName = "Cherry")
                }
            )
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.groupingMetadata).isNotNull()
        val expectedMap =
            mutableLongObjectMapOf<String>().apply {
                put(1L, "A")
                put(2L, "B")
                put(3L, "C")
            }
        assertThat(result.groupingMetadata.contactIdToSectionMap).isEqualTo(expectedMap)
    }

    @Test
    fun getContacts_customMode_returnsDisplayNamesContacts() = runTest {
        val mimeTypes = listOf(MimeType.EMAIL, MimeType.PHONE)
        val matchAll = true
        val queryMode = ContactsQueryMode.Custom(mimeTypes, matchAll)
        val expectedUri =
            ContactsContract.AUTHORITY_URI.buildUpon()
                .appendPath("contacts")
                .appendPath("mimes")
                .appendQueryParameter(
                    Contacts.REQUESTED_MIMETYPES_PARAM_KEY,
                    mimeTypes.joinToString(",") { it.value },
                )
                .appendQueryParameter(Contacts.MATCH_ALL_MIMETYPES_PARAM_KEY, "true")
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor = createCursorWithExtras(createDisplayNameCursor().apply { addDisplayNameRow() })
        fakeContentProvider.setCursorForUri(expectedUri, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.contacts).isNotEmpty()
        val contact = result.contacts.first()
        assertThat(contact).isInstanceOf(DisplayNameContact::class.java)
        assertThat(contact.displayName).isEqualTo(TEST_CONTACT_NAME)
        assertThat(fakeContentProvider.capturedUris)
            .contains(ContentProvider.maybeAddUserId(expectedUri, currentUserId))
    }

    @Test
    fun getContacts_customMode_parsesGroupMetadata() = runTest {
        val mimeTypes = listOf(MimeType.EMAIL, MimeType.PHONE)
        val matchAll = true
        val queryMode = ContactsQueryMode.Custom(mimeTypes, matchAll)
        val expectedUri =
            ContactsContract.AUTHORITY_URI.buildUpon()
                .appendPath("contacts")
                .appendPath("mimes")
                .appendQueryParameter(
                    Contacts.REQUESTED_MIMETYPES_PARAM_KEY,
                    mimeTypes.joinToString(",") { it.value },
                )
                .appendQueryParameter(Contacts.MATCH_ALL_MIMETYPES_PARAM_KEY, "true")
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            createCursorWithExtras(
                createDisplayNameCursor().apply {
                    addDisplayNameRow(id = 1L, displayName = "Apple")
                    addDisplayNameRow(id = 2L, displayName = "Banana")
                    addDisplayNameRow(id = 3L, displayName = "Cherry")
                }
            )
        fakeContentProvider.setCursorForUri(expectedUri, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.groupingMetadata).isNotNull()
        val expectedMap =
            mutableLongObjectMapOf<String>().apply {
                put(1L, "A")
                put(2L, "B")
                put(3L, "C")
            }
        assertThat(result.groupingMetadata.contactIdToSectionMap).isEqualTo(expectedMap)
    }

    @Test
    fun searchContacts_customMode_returnsDisplayNamesContacts() = runTest {
        val query = "Test"
        val mimeTypes = listOf(MimeType.PHOTO)
        val matchAll = false
        val queryMode = ContactsQueryMode.Custom(mimeTypes, matchAll)
        val expectedUri =
            ContactsContract.AUTHORITY_URI.buildUpon()
                .appendPath("contacts")
                .appendPath("mimes")
                .appendPath("filter")
                .appendPath(query)
                .appendQueryParameter(
                    Contacts.REQUESTED_MIMETYPES_PARAM_KEY,
                    mimeTypes.joinToString(",") { it.value },
                )
                .appendQueryParameter(Contacts.MATCH_ALL_MIMETYPES_PARAM_KEY, "false")
                // EXTRA_ADDRESS_BOOK_INDEX is not used in search
                .build()
        val cursor = createDisplayNameCursor().apply { addDisplayNameRow() }
        fakeContentProvider.setCursorForUri(expectedUri, cursor)

        val contacts = repository.searchContacts(query, queryMode, currentUserId)

        assertThat(contacts).isNotEmpty()
        val contact = contacts.first()
        assertThat(contact.displayName).isEqualTo(TEST_CONTACT_NAME)
        assertThat(fakeContentProvider.capturedUris)
            .contains(ContentProvider.maybeAddUserId(expectedUri, currentUserId))
    }

    @Test
    fun getContactsForIntent_groupsMultipleEntriesForSameContact() = runTest {
        val queryMode = ContactsQueryMode.PhonesOnly
        val uriToExpect =
            Phone.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            createCursorWithExtras(
                createPhoneCursor().apply {
                    addPhoneRow(
                        number = TEST_PHONE_1,
                        dataId = TEST_CONTACT_DATA_ID_1,
                        type = Phone.TYPE_HOME,
                    )
                    addPhoneRow(
                        displayName = TEST_CONTACT_NAME_2, // Same contact ID
                        number = TEST_PHONE_2,
                        dataId = TEST_CONTACT_DATA_ID_2,
                        type = Phone.TYPE_WORK,
                    )
                }
            )
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.contacts).hasSize(1)
        val phoneContact = result.contacts.first() as PhoneContact
        assertThat(phoneContact.phones).hasSize(2)
        assertThat(phoneContact.phones[0].number).isEqualTo(TEST_PHONE_1)
        assertThat(phoneContact.phones[0].label).isEqualTo("Home")
        assertThat(phoneContact.phones[1].number).isEqualTo(TEST_PHONE_2)
        assertThat(phoneContact.phones[1].label).isEqualTo("Work")
    }

    @Test
    fun getContactsForIntent_phonesOnlyMode_passesUriValueCorrectly() = runTest {
        val queryMode = ContactsQueryMode.PhonesOnly
        val fakeUri = "content://fake/uri/123"
        val uriToExpect =
            Phone.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            createCursorWithExtras(
                createPhoneCursor().apply {
                    addPhoneRow(photoUri = null, type = Phone.TYPE_HOME)
                    addPhoneRow(
                        id = 2L,
                        displayName = TEST_CONTACT_NAME_2,
                        photoUri = fakeUri,
                        number = TEST_PHONE_2,
                        dataId = TEST_CONTACT_DATA_ID_2,
                        type = Phone.TYPE_WORK,
                    )
                }
            )
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.contacts).hasSize(2)
        assertThat((result.contacts[0] as PhoneContact).profilePictureUri).isNull()
        assertThat((result.contacts[1] as PhoneContact).profilePictureUri).isNotNull()
        val profilePictureUri = (result.contacts[1] as PhoneContact).profilePictureUri!!.toUri()
        assertThat(profilePictureUri.userInfo).isNotNull()
        assertThat(ContentProvider.getUriWithoutUserId(profilePictureUri).toString())
            .isEqualTo(fakeUri)
    }

    @Test
    fun getContactsForIntent_emailsOnlyMode_passesUriValueCorrectly() = runTest {
        val queryMode = ContactsQueryMode.EmailsOnly
        val fakeUri = "content://fake/uri/111"
        val uriToExpect =
            Email.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            createCursorWithExtras(
                createEmailCursor().apply {
                    addEmailRow(photoUri = null, type = Email.TYPE_HOME)
                    addEmailRow(
                        id = 2L,
                        displayName = TEST_CONTACT_NAME_2,
                        photoUri = fakeUri,
                        address = "testemail@email.com",
                        dataId = TEST_CONTACT_DATA_ID_2,
                        type = Email.TYPE_WORK,
                    )
                }
            )
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.contacts).hasSize(2)
        assertThat((result.contacts[0] as EmailContact).profilePictureUri).isNull()
        assertThat((result.contacts[1] as EmailContact).profilePictureUri).isNotNull()
        val profilePictureUri = (result.contacts[1] as EmailContact).profilePictureUri!!.toUri()
        assertThat(profilePictureUri.userInfo).isNotNull()
        assertThat(ContentProvider.getUriWithoutUserId(profilePictureUri).toString())
            .isEqualTo(fakeUri)
    }

    @Test
    fun getContactsForIntent_displayNamesOnlyMode_passesUriValueCorrectly() = runTest {
        val queryMode = ContactsQueryMode.DisplayNamesOnly
        val fakeUri = "content://fake/uri/123"
        val uriToExpect =
            Contacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            createCursorWithExtras(
                createDisplayNameCursor().apply {
                    addDisplayNameRow(photoUri = null)
                    addDisplayNameRow(
                        id = TEST_CONTACT_ID,
                        displayName = TEST_CONTACT_NAME_2,
                        photoUri = fakeUri,
                    )
                }
            )
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, currentUserId)

        assertThat(result.contacts).hasSize(2)
        assertThat((result.contacts[0] as DisplayNameContact).profilePictureUri).isNull()
        assertThat((result.contacts[1] as DisplayNameContact).profilePictureUri).isNotNull()
        val profilePictureUri =
            (result.contacts[1] as DisplayNameContact).profilePictureUri!!.toUri()
        assertThat(profilePictureUri.userInfo).isNotNull()
        assertThat(ContentProvider.getUriWithoutUserId(profilePictureUri).toString())
            .isEqualTo(fakeUri)
    }

    @Test
    fun getDataRowIds_returnsCorrectDataIds_forContactsAndMimeTypes() = runTest {
        val contactId = TEST_CONTACT_ID
        val emailDataId = TEST_CONTACT_DATA_ID_1
        val phoneDataId = TEST_CONTACT_DATA_ID_2
        val mimeTypes = listOf(MimeType.EMAIL, MimeType.PHONE)
        val cursor = MatrixCursor(arrayOf(Data._ID))
        cursor.addRow(arrayOf(emailDataId))
        cursor.addRow(arrayOf(phoneDataId))
        fakeContentProvider.setCursorForUri(Data.CONTENT_URI, cursor)

        val result =
            repository.getDataRowIds(
                contactIds = listOf(contactId),
                mimeTypes = mimeTypes,
                userId = currentUserId,
            )

        assertThat(result).containsExactly(emailDataId, phoneDataId)
    }

    @Test
    fun hasAnyContacts_cursorHasRows_returnsTrue() = runTest {
        val cursor = MatrixCursor(arrayOf(Contacts._ID))
        cursor.addRow(arrayOf(1))
        fakeContentProvider.setCursorForUri(Contacts.CONTENT_URI, cursor)

        val result = repository.hasAnyContacts(currentUserId)

        assertThat(result).isTrue()
    }

    @Test
    fun hasAnyContacts_cursorEmpty_returnsFalse() = runTest {
        val cursor = MatrixCursor(arrayOf(Contacts._ID))
        fakeContentProvider.setCursorForUri(Contacts.CONTENT_URI, cursor)

        val result = repository.hasAnyContacts(currentUserId)

        assertThat(result).isFalse()
    }

    @Test
    fun hasAnyContacts_cursorNull_returnsFalse() = runTest {
        val result = repository.hasAnyContacts(currentUserId)

        assertThat(result).isFalse()
    }

    @Test
    fun getContacts_crossProfileUser_queriesCorrectProviderAuthority() = runTest {
        fakeContentProvider.bypassUserIdCheck = true
        val queryMode = ContactsQueryMode.DisplayNamesOnly
        val uriToExpect =
            Contacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            createCursorWithExtras(
                createDisplayNameCursor().apply {
                    addDisplayNameRow(
                        id = CROSS_PROFILE_CONTACT_ID,
                        displayName = CROSS_PROFILE_CONTACT_NAME,
                        lookupKey = CROSS_PROFILE_LOOKUP_KEY,
                    )
                }
            )
        fakeContentProvider.setCursorForUri(uriToExpect, cursor)

        val result = repository.getContacts(queryMode, CROSS_PROFILE_USER_ID)

        assertThat(result.contacts).isNotEmpty()
        val contact = result.contacts.first()
        assertThat(contact).isInstanceOf(DisplayNameContact::class.java)
        assertThat((contact as DisplayNameContact).displayName)
            .isEqualTo(CROSS_PROFILE_CONTACT_NAME)
        assertThat(fakeContentProvider.capturedUris)
            .contains(ContentProvider.maybeAddUserId(uriToExpect, CROSS_PROFILE_USER_ID))
    }

    @Test
    fun searchContacts_crossProfileUser_queriesCorrectProviderAuthority() = runTest {
        fakeContentProvider.bypassUserIdCheck = true
        val query = "Cross-Profile"
        val queryMode = ContactsQueryMode.DisplayNamesOnly
        val expectedUri = Contacts.CONTENT_FILTER_URI.buildUpon().appendPath(query).build()
        val cursor =
            createDisplayNameCursor().apply {
                addDisplayNameRow(
                    id = CROSS_PROFILE_SEARCH_RESULT_ID,
                    displayName = CROSS_PROFILE_SEARCH_RESULT_NAME,
                    lookupKey = CROSS_PROFILE_SEARCH_LOOKUP_KEY,
                )
            }
        fakeContentProvider.setCursorForUri(expectedUri, cursor)

        val contacts = repository.searchContacts(query, queryMode, CROSS_PROFILE_USER_ID)

        assertThat(contacts).isNotEmpty()
        assertThat((contacts.first() as DisplayNameContact).displayName)
            .isEqualTo(CROSS_PROFILE_SEARCH_RESULT_NAME)
        assertThat(fakeContentProvider.capturedUris)
            .contains(ContentProvider.maybeAddUserId(expectedUri, CROSS_PROFILE_USER_ID))
    }

    @Test
    fun getDataRowIds_crossProfileUser_queriesCorrectProviderAuthority() = runTest {
        fakeContentProvider.bypassUserIdCheck = true
        val contactId = CROSS_PROFILE_DATA_CONTACT_ID
        val dataId = CROSS_PROFILE_DATA_ROW_ID
        val mimeTypes = listOf(MimeType.EMAIL)
        val expectedUri = Data.CONTENT_URI
        val cursor = MatrixCursor(arrayOf(Data._ID))
        cursor.addRow(arrayOf(dataId))
        fakeContentProvider.setCursorForUri(expectedUri, cursor)

        val result =
            repository.getDataRowIds(
                contactIds = listOf(contactId),
                mimeTypes = mimeTypes,
                userId = CROSS_PROFILE_USER_ID,
            )

        assertThat(result).containsExactly(dataId)
        assertThat(fakeContentProvider.capturedUris)
            .contains(ContentProvider.maybeAddUserId(Data.CONTENT_URI, CROSS_PROFILE_USER_ID))
    }

    @Test
    fun createHeaderIterator_emptyList_hasNoNext() {
        val iterator = ContactsRepositoryImpl.createHeaderIterator(emptyArray(), intArrayOf())
        assertThat(iterator.hasNext()).isFalse()
    }

    @Test
    fun createHeaderIterator_singleSection_yieldsCorrectTitles() {
        val iterator =
            ContactsRepositoryImpl.createHeaderIterator(
                titles = arrayOf("A"),
                counts = intArrayOf(2),
            )
        assertThat(iterator.hasNext()).isTrue()
        assertThat(iterator.next()).isEqualTo("A")
        assertThat(iterator.hasNext()).isTrue()
        assertThat(iterator.next()).isEqualTo("A")
        assertThat(iterator.hasNext()).isFalse()
    }

    @Test
    fun createHeaderIterator_multipleSections_yieldsCorrectTitles() {
        val iterator =
            ContactsRepositoryImpl.createHeaderIterator(
                titles = arrayOf("A", "B"),
                counts = intArrayOf(1, 2),
            )
        assertThat(iterator.next()).isEqualTo("A")
        assertThat(iterator.next()).isEqualTo("B")
        assertThat(iterator.next()).isEqualTo("B")
        assertThat(iterator.hasNext()).isFalse()
    }

    @Test
    fun createHeaderIterator_mismatchedListSizes_usesShorterCountsList() {
        val iterator =
            ContactsRepositoryImpl.createHeaderIterator(
                titles = arrayOf("A", "B"),
                counts = intArrayOf(1),
            )
        assertThat(iterator.next()).isEqualTo("A")
        assertThat(iterator.hasNext()).isFalse()
    }

    @Test
    fun createHeaderIterator_mismatchedListSizes_usesShorterTitlesList() {
        val iterator =
            ContactsRepositoryImpl.createHeaderIterator(
                titles = arrayOf("A"),
                counts = intArrayOf(1, 2),
            )
        assertThat(iterator.next()).isEqualTo("A")
        assertThat(iterator.hasNext()).isFalse()
    }

    @Test
    fun createHeaderIterator_sectionsWithZeroCount_areSkipped() {
        val iterator =
            ContactsRepositoryImpl.createHeaderIterator(
                titles = arrayOf("A", "B", "C"),
                counts = intArrayOf(2, 0, 1),
            )
        assertThat(iterator.next()).isEqualTo("A")
        assertThat(iterator.next()).isEqualTo("A")
        assertThat(iterator.next()).isEqualTo("C")
        assertThat(iterator.hasNext()).isFalse()
    }

    @Test
    fun createHeaderIterator_emptyCountsList_hasNextIsFalse() {
        val iterator =
            ContactsRepositoryImpl.createHeaderIterator(
                titles = arrayOf("A", "B"),
                counts = intArrayOf(),
            )
        assertThat(iterator.hasNext()).isFalse()
    }

    @Test
    fun createHeaderIterator_emptyTitlesList_hasNextIsFalse() {
        val iterator =
            ContactsRepositoryImpl.createHeaderIterator(
                titles = emptyArray(),
                counts = intArrayOf(1, 2),
            )
        assertThat(iterator.hasNext()).isFalse()
    }

    private fun createDisplayNameCursor(): MatrixCursor =
        MatrixCursor(ContactsRepositoryImpl.DISPLAY_NAME_FETCH_PROJECTION)

    private fun MatrixCursor.addDisplayNameRow(
        id: Long = TEST_CONTACT_ID,
        displayName: String? = TEST_CONTACT_NAME,
        photoUri: String? = null,
        starred: Int = 0,
        lookupKey: String = "lookupKey_$id",
    ) {
        addRow(arrayOf<Any?>(id, displayName, photoUri, starred, lookupKey, DISPLAY_NAME_SOURCE))
    }

    private fun createEmailCursor(): MatrixCursor =
        MatrixCursor(ContactsRepositoryImpl.EMAIL_FETCH_PROJECTION)

    private fun MatrixCursor.addEmailRow(
        id: Long = TEST_CONTACT_ID,
        displayName: String? = TEST_CONTACT_NAME,
        photoUri: String? = null,
        starred: Int = 0,
        lookupKey: String = "lookupKey_$id",
        address: String = TEST_EMAIL,
        dataId: Long = TEST_CONTACT_DATA_ID_1,
        type: Int = Email.TYPE_HOME,
        label: String? = null,
    ) {
        addRow(
            arrayOf<Any?>(
                id,
                displayName,
                photoUri,
                starred,
                lookupKey,
                address,
                dataId,
                type,
                label,
                DISPLAY_NAME_SOURCE,
            )
        )
    }

    private fun createPhoneCursor(): MatrixCursor =
        MatrixCursor(ContactsRepositoryImpl.PHONE_FETCH_PROJECTION)

    private fun MatrixCursor.addPhoneRow(
        id: Long = TEST_CONTACT_ID,
        displayName: String? = TEST_CONTACT_NAME,
        photoUri: String? = null,
        starred: Int = 0,
        lookupKey: String = "lookupKey_$id",
        number: String = TEST_PHONE_1,
        dataId: Long = TEST_CONTACT_DATA_ID_1,
        type: Int = Phone.TYPE_HOME,
        label: String? = null,
    ) {
        addRow(
            arrayOf<Any?>(
                id,
                displayName,
                photoUri,
                starred,
                lookupKey,
                number,
                dataId,
                type,
                label,
                DISPLAY_NAME_SOURCE,
            )
        )
    }
}
