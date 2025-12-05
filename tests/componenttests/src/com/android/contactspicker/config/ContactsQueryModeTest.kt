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
import com.android.contactspicker.data.model.MimeType
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test

class ContactsQueryModeTest {

    @Test
    fun getQueryMode_actionPick_supportedTypes() {
        listOf(
                Pair(Email.CONTENT_TYPE, ContactsQueryMode.EmailsOnly),
                Pair(Phone.CONTENT_TYPE, ContactsQueryMode.PhonesOnly),
                Pair(Contacts.CONTENT_TYPE, ContactsQueryMode.DisplayNamesOnly),
            )
            .forEach { pair ->
                val queryMode =
                    ContactsQueryMode.getQueryMode(
                        pickerAction = ContactsPickerAction.ACTION_PICK,
                        intentType = pair.first,
                        intentExtras = null,
                    )

                assertThat(queryMode).isEqualTo(pair.second)
            }
    }

    @Test
    fun getQueryMode_actionPick_unsupportedType() {
        assertFailsWith<IllegalArgumentException> {
            ContactsQueryMode.getQueryMode(
                pickerAction = ContactsPickerAction.ACTION_PICK,
                intentType = "vnd.android.cursor.dir/unsupported",
                intentExtras = null,
            )
        }
    }

    @Test
    fun getQueryMode_actionPick_nullType_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ContactsQueryMode.getQueryMode(
                pickerAction = ContactsPickerAction.ACTION_PICK,
                intentType = null,
                intentExtras = null,
            )
        }
    }

    @Test
    fun getQueryMode_actionPickContacts_missingMimeTypes_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ContactsQueryMode.getQueryMode(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = null,
            )
        }
    }

    @Test
    fun getQueryMode_actionPickContacts_unsupportedType() {
        val mimeTypes = ArrayList(listOf("vnd.android.cursor.dir/unsupported"))
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimeTypes,
                )
            }
        assertFailsWith<IllegalArgumentException> {
            ContactsQueryMode.getQueryMode(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )
        }
    }

    @Test
    fun getQueryMode_actionPickContacts_singleEmailItemType() {
        val mimeTypes = ArrayList(listOf(Email.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimeTypes,
                )
            }

        val queryMode =
            ContactsQueryMode.getQueryMode(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(queryMode).isEqualTo(ContactsQueryMode.EmailsOnly)
    }

    @Test
    fun getQueryMode_actionPickContacts_singlePhoneItemType() {
        val mimes = ArrayList(listOf(Phone.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimes,
                )
            }

        val queryMode =
            ContactsQueryMode.getQueryMode(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(queryMode).isEqualTo(ContactsQueryMode.PhonesOnly)
    }

    @Test
    fun getQueryMode_actionPickContactsWithMultipleMimeTypes_customMode() {
        val mimeTypes = ArrayList(listOf(Email.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimeTypes,
                )
            }

        val queryMode =
            ContactsQueryMode.getQueryMode(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(queryMode)
            .isEqualTo(ContactsQueryMode.Custom(listOf(MimeType.EMAIL, MimeType.PHONE), false))
    }

    @Test
    fun getQueryMode_actionPickContactsWithSingleOtherMimeType_customMode() {
        val mimeTypes =
            ArrayList(listOf(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimeTypes,
                )
            }

        val queryMode =
            ContactsQueryMode.getQueryMode(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(queryMode)
            .isEqualTo(ContactsQueryMode.Custom(listOf(MimeType.STRUCTURED_POSTAL), false))
    }

    @Test
    fun getQueryMode_actionPickContacts_allSupportedTypes() {
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    MimeType.ACTION_PICK_CONTACTS_SUPPORTED.map { it.value } as ArrayList,
                )
            }
        val queryMode =
            ContactsQueryMode.getQueryMode(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(queryMode)
            .isEqualTo(
                ContactsQueryMode.Custom(MimeType.ACTION_PICK_CONTACTS_SUPPORTED.map { it }, false)
            )
    }

    @Test
    fun getQueryMode_actionPickContacts_contactsMimeTypeNotAllowed() {
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    ArrayList(listOf(MimeType.CONTACTS.value)),
                )
            }
        assertFailsWith<IllegalArgumentException> {
            ContactsQueryMode.getQueryMode(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )
        }
    }

    @Test
    fun getQueryMode_actionPickContactsWithMatchAll() {
        val mimeTypes = ArrayList(listOf(Email.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimeTypes,
                )
                putBoolean(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_MATCH_ALL_DATA_FIELDS,
                    true,
                )
            }

        val queryMode =
            ContactsQueryMode.getQueryMode(
                pickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(queryMode)
            .isEqualTo(ContactsQueryMode.Custom(listOf(MimeType.EMAIL, MimeType.PHONE), true))
    }

    @Test
    fun getMimeTypes_phonesOnly_returnsPhone() {
        val queryMode: ContactsQueryMode = ContactsQueryMode.PhonesOnly
        assertThat(queryMode.getMimeTypes()).containsExactly(MimeType.PHONE)
    }

    @Test
    fun getMimeTypes_emailsOnly_returnsEmail() {
        val queryMode: ContactsQueryMode = ContactsQueryMode.EmailsOnly
        assertThat(queryMode.getMimeTypes()).containsExactly(MimeType.EMAIL)
    }

    @Test
    fun getMimeTypes_displayNamesOnly_returnsContacts() {
        val queryMode: ContactsQueryMode = ContactsQueryMode.DisplayNamesOnly
        assertThat(queryMode.getMimeTypes()).containsExactly(MimeType.CONTACTS)
    }

    @Test
    fun getMimeTypes_custom_returnsCustomTypes() {
        val customMimes = listOf(MimeType.STRUCTURED_POSTAL, MimeType.WEBSITE)
        val queryMode: ContactsQueryMode =
            ContactsQueryMode.Custom(customMimes, matchAllRequestedMimeTypes = false)
        assertThat(queryMode.getMimeTypes()).containsExactlyElementsIn(customMimes).inOrder()
    }
}
