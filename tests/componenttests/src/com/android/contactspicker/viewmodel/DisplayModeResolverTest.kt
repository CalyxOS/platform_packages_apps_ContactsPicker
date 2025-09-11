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
package com.android.contactspicker.viewmodel

import android.content.Intent
import android.provider.ContactsContract
import com.android.contactspicker.DisplayMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Unit tests for [DisplayModeResolver]. This test class uses a Parameterized runner to test
 * multiple intent types.
 */
@RunWith(Parameterized::class)
class DisplayModeResolverTest(
    private val intentAction: String?,
    private val intentType: String?,
    private val expectedDisplayMode: DisplayMode?,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Action={0}, Type={1}, ExpectedMode={2}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Email.CONTENT_TYPE,
                    DisplayMode.EMAIL_SELECTION,
                ),
                arrayOf(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                    DisplayMode.EMAIL_SELECTION,
                ),
                arrayOf(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE,
                    DisplayMode.PHONE_SELECTION,
                ),
                arrayOf(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    DisplayMode.PHONE_SELECTION,
                ),
                arrayOf(
                    Intent.ACTION_PICK,
                    ContactsContract.Contacts.CONTENT_TYPE,
                    DisplayMode.CONTACT_SELECTION,
                ),
                arrayOf(
                    Intent.ACTION_PICK,
                    ContactsContract.Contacts.CONTENT_ITEM_TYPE,
                    DisplayMode.CONTACT_SELECTION,
                ),
                // invalid types
                arrayOf(Intent.ACTION_PICK, "invalid_type", null),
                arrayOf(
                    "com.android.INVALID_ACTION",
                    ContactsContract.CommonDataKinds.Email.CONTENT_TYPE,
                    null,
                ),
                arrayOf(null, ContactsContract.CommonDataKinds.Email.CONTENT_TYPE, null),
            )
        }
    }

    @Test
    fun resolve_returnsCorrectDisplayMode() {
        val displayMode = DisplayModeResolver.resolve(intentAction, intentType)
        assertThat(displayMode).isEqualTo(expectedDisplayMode)
    }
}
