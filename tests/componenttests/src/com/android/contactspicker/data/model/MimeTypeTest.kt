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
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class MimeTypeTest {

    @Test
    fun fromString_unknownType_throwsException() {
        assertFailsWith<IllegalArgumentException> { MimeType.fromString("unknown/mimetype") }
    }

    @Test
    fun fromString_knownType_returnsCorrectEnum() {
        val result = MimeType.fromString(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        assertThat(result).isEqualTo(MimeType.PHONE)
    }

    @Test
    fun fromString_phoneAndEmailContentType_returnsCorrectEnum() {
        assertThat(MimeType.fromString(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE))
            .isEqualTo(MimeType.PHONE)
        assertThat(MimeType.fromString(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE))
            .isEqualTo(MimeType.EMAIL)
    }

    @Test
    fun validateForActionPickContacts_allowedType_doesNotThrow() {
        MimeType.ACTION_PICK_CONTACTS_SUPPORTED.forEach { mimeType ->
            mimeType.validateForActionPickContacts()
        }
    }

    @Test
    fun validateForActionPickContacts_disallowedType_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            MimeType.CONTACTS.validateForActionPickContacts()
        }
    }
}
