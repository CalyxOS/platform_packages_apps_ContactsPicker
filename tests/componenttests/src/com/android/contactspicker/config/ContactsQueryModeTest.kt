/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contactspicker.config

import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsPickerSessionContract
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test

class ContactsQueryModeTest {

    @Test
    fun getQueryModeAndMimeTypes_actionPick_supportedTypes() {
        listOf(
                Pair(Email.CONTENT_TYPE, ContactsQueryMode.EmailsOnly),
                Pair(Phone.CONTENT_TYPE, ContactsQueryMode.PhonesOnly),
                Pair(Contacts.CONTENT_TYPE, ContactsQueryMode.DisplayNamesOnly),
                Pair(Email.CONTENT_ITEM_TYPE, ContactsQueryMode.EmailsOnly),
                Pair(Phone.CONTENT_ITEM_TYPE, ContactsQueryMode.PhonesOnly),
                Pair(Contacts.CONTENT_ITEM_TYPE, ContactsQueryMode.DisplayNamesOnly),
            )
            .forEach { pair ->
                val (queryMode, requestedMimeTypes) =
                    ContactsQueryMode.getQueryModeAndMimeTypes(
                        pickerAction = ContactsPickerAction.ACTION_PICK,
                        intentType = pair.first,
                        intentExtras = null,
                    )

                assertThat(requestedMimeTypes).isEqualTo(listOf(pair.first))
                assertThat(queryMode).isEqualTo(pair.second)
            }
    }

    @Test
    fun getQueryModeAndMimeTypes_actionPick_unsupportedType() {
        assertFailsWith<IllegalArgumentException> {
            ContactsQueryMode.getQueryModeAndMimeTypes(
                pickerAction = ContactsPickerAction.ACTION_PICK,
                intentType = "vnd.android.cursor.dir/unsupported",
                intentExtras = null,
            )
        }
    }

    @Test
    fun getQueryModeAndMimeTypes_actionPick_nullType_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ContactsQueryMode.getQueryModeAndMimeTypes(
                pickerAction = ContactsPickerAction.ACTION_PICK,
                intentType = null,
                intentExtras = null,
            )
        }
    }

    @Test
    fun getQueryModeAndMimeTypes_actionPickContacts_missingMimeTypes_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ContactsQueryMode.getQueryModeAndMimeTypes(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = null,
            )
        }
    }

    @Test
    fun getQueryModeAndMimeTypes_actionPickContacts_singleEmailItemType() {
        val mimeTypes = ArrayList(listOf(Email.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimeTypes,
                )
            }

        val (queryMode, requestedMimeTypes) =
            ContactsQueryMode.getQueryModeAndMimeTypes(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(queryMode).isEqualTo(ContactsQueryMode.EmailsOnly)
        assertThat(requestedMimeTypes).isEqualTo(mimeTypes)
    }

    @Test
    fun getQueryModeAndMimeTypes_actionPickContacts_singlePhoneItemType() {
        val mimes = ArrayList(listOf(Phone.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimes,
                )
            }

        val (queryMode, requestedMimeTypes) =
            ContactsQueryMode.getQueryModeAndMimeTypes(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(queryMode).isEqualTo(ContactsQueryMode.PhonesOnly)
    }

    @Test
    fun getQueryModeAndMimeTypes_actionPickContactsWithMultipleMimeTypes_customMode() {
        val mimeTypes = ArrayList(listOf(Email.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimeTypes,
                )
            }

        val (queryMode, requestedMimeTypes) =
            ContactsQueryMode.getQueryModeAndMimeTypes(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(queryMode).isEqualTo(ContactsQueryMode.Custom(mimeTypes))
        assertThat(requestedMimeTypes).isEqualTo(mimeTypes)
    }

    @Test
    fun getQueryModeAndMimeTypes_actionPickContactsWithSingleOtherMimeType_customMode() {
        val mimeTypes =
            ArrayList(listOf(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimeTypes,
                )
            }

        val (queryMode, requestedMimeTypes) =
            ContactsQueryMode.getQueryModeAndMimeTypes(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(queryMode).isEqualTo(ContactsQueryMode.Custom(mimeTypes))
        assertThat(requestedMimeTypes).isEqualTo(mimeTypes)
    }
}
