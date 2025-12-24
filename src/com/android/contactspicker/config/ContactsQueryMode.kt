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
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsPickerSessionContract
import com.android.contactspicker.data.model.MimeType

/** Defines which specific contact data to query from the ContactsRepository. */
sealed class ContactsQueryMode {
    /** Only fetch contacts with phone numbers. */
    object PhonesOnly : ContactsQueryMode()

    /** Only fetch contacts with email addresses. */
    object EmailsOnly : ContactsQueryMode()

    /**
     * Only fetch contacts with display names only. Corresponds to Contact data type via
     * ACTION_PICK.
     */
    object DisplayNamesOnly : ContactsQueryMode()

    /**
     * Fetch contacts that have data for at least one of the specified mimetypes. Used by
     * ACTION_PICK_CONTACTS when multiple fields or types other than phone or email are requested.
     */
    data class Custom(val mimetypes: List<MimeType>, val matchAllRequestedMimeTypes: Boolean) :
        ContactsQueryMode()

    fun getMimeTypes(): List<MimeType> {
        return when (this) {
            is PhonesOnly -> listOf(MimeType.PHONE)
            is EmailsOnly -> listOf(MimeType.EMAIL)
            is DisplayNamesOnly -> listOf(MimeType.CONTACTS)
            is Custom -> this.mimetypes
        }
    }

    companion object {
        fun getQueryMode(
            pickerAction: ContactsPickerAction,
            intentType: String?,
            intentExtras: Bundle?,
        ): ContactsQueryMode {
            return when (pickerAction) {
                ContactsPickerAction.ACTION_PICK -> parseActionPick(intentType)
                ContactsPickerAction.ACTION_PICK_CONTACTS -> parseActionPickContacts(intentExtras)
            }
        }

        /** Handles parsing for the legacy ACTION_PICK intent. */
        private fun parseActionPick(intentType: String?): ContactsQueryMode {
            return when (intentType) {
                Email.CONTENT_TYPE -> EmailsOnly

                Phone.CONTENT_TYPE -> PhonesOnly

                Contacts.CONTENT_TYPE -> DisplayNamesOnly

                else ->
                    throw IllegalArgumentException(
                        "Unsupported intent type for ACTION_PICK: $intentType"
                    )
            }
        }

        /** Handles parsing for the new ACTION_PICK_CONTACTS intent. */
        private fun parseActionPickContacts(intentExtras: Bundle?): ContactsQueryMode {
            val mimeTypeStrings =
                intentExtras?.getStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS
                ) ?: emptyList()

            if (mimeTypeStrings.isEmpty()) {
                throw IllegalArgumentException(
                    "Missing or empty EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS for ACTION_PICK_CONTACTS"
                )
            }

            val validatedMimeTypes =
                mimeTypeStrings.map { mimeString ->
                    val mappedType = MimeType.fromString(mimeString)
                    mappedType.validateForActionPickContacts()
                    mappedType
                }

            val matchAll =
                intentExtras?.getBoolean(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_MATCH_ALL_DATA_FIELDS,
                    false,
                ) ?: false

            return if (validatedMimeTypes.size == 1) {
                when (validatedMimeTypes.first()) {
                    MimeType.EMAIL -> EmailsOnly
                    MimeType.PHONE -> PhonesOnly
                    else -> Custom(validatedMimeTypes, matchAll)
                }
            } else {
                Custom(validatedMimeTypes, matchAll)
            }
        }
    }
}
