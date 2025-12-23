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

import android.content.Intent
import android.content.flags.Flags
import android.os.Bundle
import android.platform.test.annotations.RequiresFlagsEnabled
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsPickerSessionContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.data.model.MimeType
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsPickerRequestConfigTest {

    @Test
    fun create_unsupportedAction_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ContactsPickerRequestConfig.create(
                intentAction = "android.intent.action.VIEW",
                intentType = Contacts.CONTENT_TYPE,
                intentExtras = null,
            )
        }
    }

    @Test
    fun create_nullAction_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ContactsPickerRequestConfig.create(
                intentAction = null,
                intentType = Contacts.CONTENT_TYPE,
                intentExtras = null,
            )
        }
    }

    @Test
    fun create_actionPick_emailType() {
        val config =
            ContactsPickerRequestConfig.create(
                intentAction = Intent.ACTION_PICK,
                intentType = Email.CONTENT_TYPE,
                intentExtras = null,
            )

        assertThat(config.pickerAction).isEqualTo(ContactsPickerAction.ACTION_PICK)
        assertThat(config.queryMode).isEqualTo(ContactsQueryMode.EmailsOnly)
        assertThat(config.isMultiSelectEnabled).isFalse()
        assertThat(config.maxSelectionLimit).isEqualTo(1)
        assertThat(config.requestedMimeTypes).isEqualTo(listOf(MimeType.EMAIL))
    }

    @Test
    fun create_actionPick_phoneType_multiSelect() {
        val extras = Bundle().apply { putBoolean(Intent.EXTRA_ALLOW_MULTIPLE, true) }
        val config =
            ContactsPickerRequestConfig.create(
                intentAction = Intent.ACTION_PICK,
                intentType = Phone.CONTENT_TYPE,
                intentExtras = extras,
            )

        assertThat(config.pickerAction).isEqualTo(ContactsPickerAction.ACTION_PICK)
        assertThat(config.queryMode).isEqualTo(ContactsQueryMode.PhonesOnly)
        assertThat(config.isMultiSelectEnabled).isTrue()
        assertThat(config.maxSelectionLimit)
            .isEqualTo(ContactsPickerRequestConfig.DEFAULT_SELECTION_LIMIT)
        assertThat(config.requestedMimeTypes).isEqualTo(listOf(MimeType.PHONE))
    }

    @Test
    fun create_actionPick_contactType() {
        val config =
            ContactsPickerRequestConfig.create(
                intentAction = Intent.ACTION_PICK,
                intentType = Contacts.CONTENT_TYPE,
                intentExtras = null,
            )

        assertThat(config.pickerAction).isEqualTo(ContactsPickerAction.ACTION_PICK)
        assertThat(config.queryMode).isEqualTo(ContactsQueryMode.DisplayNamesOnly)
        assertThat(config.maxSelectionLimit).isEqualTo(1)
    }

    @Test
    fun create_actionPick_unsupportedType_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ContactsPickerRequestConfig.create(
                intentAction = Intent.ACTION_PICK,
                intentType = "vnd.android.cursor.dir/unsupported",
                intentExtras = null,
            )
        }
    }

    @Test
    fun create_actionPick_nullType_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ContactsPickerRequestConfig.create(
                intentAction = Intent.ACTION_PICK,
                intentType = null,
                intentExtras = null,
            )
        }
    }

    @Test
    fun create_actionPickContacts_missingMimeTypes_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ContactsPickerRequestConfig.create(
                intentAction = ContactsPickerSessionContract.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = null,
            )
        }
    }

    @Test
    fun create_actionPickContacts_singleEmailItemType() {
        val mimeTypes = ArrayList(listOf(Email.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimeTypes,
                )
            }

        val config =
            ContactsPickerRequestConfig.create(
                intentAction = ContactsPickerSessionContract.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(config.pickerAction).isEqualTo(ContactsPickerAction.ACTION_PICK_CONTACTS)
        assertThat(config.queryMode).isEqualTo(ContactsQueryMode.EmailsOnly)
        assertThat(config.requestedMimeTypes).isEqualTo(listOf(MimeType.EMAIL))
    }

    @Test
    fun create_actionPickContacts_singlePhoneItemType() {
        val mimes = ArrayList(listOf(Phone.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimes,
                )
            }

        val config =
            ContactsPickerRequestConfig.create(
                intentAction = ContactsPickerSessionContract.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(config.queryMode).isEqualTo(ContactsQueryMode.PhonesOnly)
    }

    @Test
    fun create_actionPickContacts_multipleMimeTypes_customMode() {
        val mimeTypes = listOf(MimeType.EMAIL, MimeType.PHONE)
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    ArrayList(listOf(Email.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE)),
                )
            }

        val config =
            ContactsPickerRequestConfig.create(
                intentAction = ContactsPickerSessionContract.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(config.pickerAction).isEqualTo(ContactsPickerAction.ACTION_PICK_CONTACTS)
        assertThat(config.queryMode).isEqualTo(ContactsQueryMode.Custom(mimeTypes, false))
        assertThat(config.requestedMimeTypes).isEqualTo(mimeTypes)
    }

    @Test
    fun create_actionPickContacts_singleOtherMimeType_customMode() {
        val mimeTypes = listOf(MimeType.STRUCTURED_POSTAL)
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    ArrayList(mimeTypes.map { it.value }),
                )
            }

        val config =
            ContactsPickerRequestConfig.create(
                intentAction = ContactsPickerSessionContract.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(config.queryMode).isEqualTo(ContactsQueryMode.Custom(mimeTypes, false))
    }

    @Test
    fun create_actionPickContacts_matchAllRequestedMimeTypes_true() {
        val mimeTypes = listOf(MimeType.EMAIL, MimeType.PHONE)
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    ArrayList(mimeTypes.map { it.value }),
                )
                putBoolean(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_MATCH_ALL_DATA_FIELDS,
                    true,
                )
            }

        val config =
            ContactsPickerRequestConfig.create(
                intentAction = ContactsPickerSessionContract.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(config.matchAllRequestedMimeTypes).isTrue()
        assertThat(config.queryMode).isEqualTo(ContactsQueryMode.Custom(mimeTypes, true))
    }

    @Test
    fun create_actionPickContacts_multiSelectWithCustomLimit() {
        val mimeTypes = ArrayList(listOf(Email.CONTENT_ITEM_TYPE))
        val customLimit = 75
        val extras =
            Bundle().apply {
                putBoolean(Intent.EXTRA_ALLOW_MULTIPLE, true)
                putInt(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT,
                    customLimit,
                )
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimeTypes,
                )
            }

        val config =
            ContactsPickerRequestConfig.create(
                intentAction = ContactsPickerSessionContract.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(config.isMultiSelectEnabled).isTrue()
        assertThat(config.maxSelectionLimit).isEqualTo(customLimit)
    }

    @Test
    fun create_actionPickContacts_multiSelectWithLimitTooHigh_throwsException() {
        val mimes = ArrayList(listOf(Email.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putBoolean(Intent.EXTRA_ALLOW_MULTIPLE, true)
                putInt(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT,
                    ContactsPickerRequestConfig.MAX_SELECTION_LIMIT + 1,
                )
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimes,
                )
            }

        assertFailsWith<IllegalArgumentException> {
            ContactsPickerRequestConfig.create(
                intentAction = ContactsPickerSessionContract.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )
        }
    }

    @Test
    fun create_actionPickContacts_multiSelectWithLimitZero_throwsException() {
        val mimes = ArrayList(listOf(Email.CONTENT_ITEM_TYPE))
        val extras =
            Bundle().apply {
                putBoolean(Intent.EXTRA_ALLOW_MULTIPLE, true)
                putInt(ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT, 0)
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    mimes,
                )
            }

        assertFailsWith<IllegalArgumentException> {
            ContactsPickerRequestConfig.create(
                intentAction = ContactsPickerSessionContract.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )
        }
    }

    @Test
    fun create_actionPickContacts_matchAllRequestedMimeTypes_defaultFalse() {
        val mimeTypes = listOf(MimeType.EMAIL, MimeType.PHONE)
        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    ArrayList(mimeTypes.map { it.value }),
                )
            }

        val config =
            ContactsPickerRequestConfig.create(
                intentAction = ContactsPickerSessionContract.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
            )

        assertThat(config.matchAllRequestedMimeTypes).isFalse()
        assertThat(config.queryMode).isEqualTo(ContactsQueryMode.Custom(mimeTypes, false))
    }
}
