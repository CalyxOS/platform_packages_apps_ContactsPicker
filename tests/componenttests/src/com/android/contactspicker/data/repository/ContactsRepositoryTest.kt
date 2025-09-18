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

import android.content.Intent
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.PhoneContact
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Unit tests for [ContactsRepository].
 *
 * This test class uses a Parameterized runner to test multiple intent logic paths.
 */
@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(Parameterized::class)
class ContactsRepositoryTest(
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

    private val repository = ContactsRepository()

    @Test
    fun fetchContacts_returnsCorrectContactTypeOrThrows() = runTest {
        if (expectException) {
            // Test for failure
            assertFailsWith<IllegalArgumentException> {
                repository.fetchContacts(intentAction, intentType)
            }
        } else {
            // Test for success
            val contacts = repository.fetchContacts(intentAction, intentType)

            // Check that the list is not empty and the first item
            // is an instance of the class we expect.
            assertThat(contacts).isNotEmpty()
            assertThat(contacts.first()).isInstanceOf(expectedResultType!!.java)
        }
    }
}
