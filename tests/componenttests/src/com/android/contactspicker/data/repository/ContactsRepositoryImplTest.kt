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
import android.content.Intent
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
import android.test.mock.MockContentResolver
import androidx.test.core.app.ApplicationProvider
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.fakes.FakeContentProvider
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [ContactsRepository].
 *
 * This test class uses a Parameterized runner to test multiple intent logic paths.
 */
@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(Parameterized::class)
class ContactsRepositoryImplTest(
    private val intentAction: String?,
    private val intentType: String?,
    private val expectException: Boolean,
    private val expectedResultType: KClass<out Contact>?,
) {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Action={0}, Type={1}, ExpectsException={2}")
        fun data(): Collection<Array<Any?>> {
            // TODO(b/439807858): Add more thorough tests for:
            //  1. Sorting order
            //  2. Grouping of contacts.
            //  3. No contacts found in the cursor.
            return listOf(
                arrayOf(Intent.ACTION_PICK, Email.CONTENT_TYPE, false, EmailContact::class),
                arrayOf(Intent.ACTION_PICK, Email.CONTENT_ITEM_TYPE, false, EmailContact::class),
                arrayOf(Intent.ACTION_PICK, Phone.CONTENT_TYPE, false, PhoneContact::class),
                arrayOf(Intent.ACTION_PICK, Phone.CONTENT_ITEM_TYPE, false, PhoneContact::class),
                arrayOf(
                    Intent.ACTION_PICK,
                    Contacts.CONTENT_TYPE,
                    false,
                    DisplayNameContact::class,
                ),
                arrayOf(
                    Intent.ACTION_PICK,
                    Contacts.CONTENT_ITEM_TYPE,
                    false,
                    DisplayNameContact::class,
                ),
                // --- Failure cases ---
                arrayOf(Intent.ACTION_PICK, "invalid_type", true, null),
                arrayOf("com.android.INVALID_ACTION", Email.CONTENT_TYPE, true, null),
                arrayOf(null, Email.CONTENT_TYPE, true, null),
            )
        }
    }

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
    fun getContactsForIntent_returnsCorrectContactTypeOrThrows() = runTest {
        if (expectException) {
            // Test for failure
            assertFailsWith<IllegalArgumentException> {
                repository.getContactsForIntent(intentAction, intentType)
            }
        } else {
            // Test for success
            val (uriToExpect, cursor) =
                when (intentType) {
                    Email.CONTENT_TYPE,
                    Email.CONTENT_ITEM_TYPE ->
                        Pair(
                            Email.CONTENT_URI,
                            MatrixCursor(
                                arrayOf(
                                    Email.CONTACT_ID,
                                    Email.DISPLAY_NAME_PRIMARY,
                                    Email.ADDRESS,
                                    Email._ID,
                                    Email.TYPE,
                                    Email.LABEL,
                                )
                            ),
                        )
                    Phone.CONTENT_TYPE,
                    Phone.CONTENT_ITEM_TYPE ->
                        Pair(
                            Phone.CONTENT_URI,
                            MatrixCursor(
                                arrayOf(
                                    Phone.CONTACT_ID,
                                    Phone.DISPLAY_NAME_PRIMARY,
                                    Phone.NUMBER,
                                    Phone._ID,
                                    Phone.TYPE,
                                    Phone.LABEL,
                                )
                            ),
                        )
                    // ...
                    Contacts.CONTENT_TYPE,
                    Contacts.CONTENT_ITEM_TYPE ->
                        // Fix: Return a Pair, just like the other branches
                        Pair(
                            Contacts.CONTENT_URI,
                            MatrixCursor(
                                arrayOf(
                                    Contacts._ID, // Matches the '1L' in your addRow
                                    Contacts.DISPLAY_NAME_PRIMARY, // Matches the 'Test Contact'
                                )
                            ),
                        )
                    else -> throw IllegalArgumentException("Unsupported intent type: $intentType")
                }

            // Add a row to the cursor
            when (intentType) {
                Email.CONTENT_TYPE,
                Email.CONTENT_ITEM_TYPE ->
                    (cursor as MatrixCursor).addRow(
                        arrayOf<Any?>(
                            1L,
                            "Test Contact",
                            "test@example.com",
                            101L,
                            Email.TYPE_HOME,
                            null,
                        )
                    )
                Phone.CONTENT_TYPE,
                Phone.CONTENT_ITEM_TYPE ->
                    (cursor as MatrixCursor).addRow(
                        arrayOf<Any?>(1L, "Test Contact", "555-0123", 101L, Phone.TYPE_HOME, null)
                    )
                Contacts.CONTENT_TYPE,
                Contacts.CONTENT_ITEM_TYPE ->
                    (cursor as MatrixCursor).addRow(arrayOf<Any>(1L, "Test Contact"))
            }

            fakeContentProvider.setCursorForUri(uriToExpect, cursor)
            val contacts = repository.getContactsForIntent(intentAction, intentType)

            assertThat(contacts).isNotEmpty()
            assertThat(contacts.first()).isInstanceOf(expectedResultType!!.java)
        }
    }

    @Test
    fun getContactsForIntent_groupsMultipleEntriesForSameContact() = runTest {
        // This test only runs for phone contacts, but logic is shared.
        if (intentType == Phone.CONTENT_TYPE) {
            val cursor =
                MatrixCursor(
                    arrayOf(
                        Phone.CONTACT_ID,
                        Phone.DISPLAY_NAME_PRIMARY,
                        Phone.NUMBER,
                        Phone._ID,
                        Phone.TYPE,
                        Phone.LABEL,
                    )
                )
            cursor.addRow(
                arrayOf<Any?>(1L, "Test Contact", "555-0123", 101L, Phone.TYPE_HOME, null)
            )
            cursor.addRow(
                arrayOf<Any?>(1L, "Test Contact", "555-0124", 102L, Phone.TYPE_WORK, null)
            )

            fakeContentProvider.setCursorForUri(Phone.CONTENT_URI, cursor)

            val contacts = repository.getContactsForIntent(intentAction, intentType)

            assertThat(contacts).hasSize(1) // Should be grouped into one contact
            val phoneContact = contacts.first() as PhoneContact
            assertThat(phoneContact.phones).hasSize(2)
            assertThat(phoneContact.phones[0].number).isEqualTo("555-0123")
            assertThat(phoneContact.phones[0].label).isEqualTo("Home")
            assertThat(phoneContact.phones[1].number).isEqualTo("555-0124")
            assertThat(phoneContact.phones[1].label).isEqualTo("Work")
        }
    }
}
