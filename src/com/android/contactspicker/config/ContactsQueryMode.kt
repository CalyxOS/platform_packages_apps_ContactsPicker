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
    data class Custom(val mimetypes: List<String>) : ContactsQueryMode()

    companion object {
        fun getQueryModeAndMimeTypes(
            pickerAction: ContactsPickerAction,
            intentType: String?,
            intentExtras: Bundle?,
        ): Pair<ContactsQueryMode, List<String>> {
            return when (pickerAction) {
                ContactsPickerAction.ACTION_PICK -> parseActionPick(intentType)
                ContactsPickerAction.ACTION_PICK_CONTACTS -> parseActionPickContacts(intentExtras)
            }
        }

        /** Handles parsing for the legacy ACTION_PICK intent. */
        private fun parseActionPick(intentType: String?): Pair<ContactsQueryMode, List<String>> {
            val queryMode =
                when (intentType) {
                    Email.CONTENT_TYPE,
                    Email.CONTENT_ITEM_TYPE -> EmailsOnly

                    Phone.CONTENT_TYPE,
                    Phone.CONTENT_ITEM_TYPE -> PhonesOnly

                    Contacts.CONTENT_TYPE,
                    Contacts.CONTENT_ITEM_TYPE -> DisplayNamesOnly

                    else ->
                        throw IllegalArgumentException(
                            "Unsupported intent type for ACTION_PICK: $intentType"
                        )
                }
            val mimeTypes = listOf(intentType)
            return Pair(queryMode, mimeTypes)
        }

        /** Handles parsing for the new ACTION_PICK_CONTACTS intent. */
        private fun parseActionPickContacts(
            intentExtras: Bundle?
        ): Pair<ContactsQueryMode, List<String>> {
            // TODO(b/442397528): check for allowed mime types
            val mimetypes =
                intentExtras?.getStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS
                ) ?: emptyList()

            if (mimetypes.isEmpty()) {
                throw IllegalArgumentException(
                    "Missing or empty EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS for ACTION_PICK_CONTACTS"
                )
            }

            val queryMode =
                if (mimetypes.size == 1) {
                    when (mimetypes.first()) {
                        Email.CONTENT_ITEM_TYPE -> EmailsOnly
                        Phone.CONTENT_ITEM_TYPE -> PhonesOnly
                        else -> Custom(mimetypes)
                    }
                } else {
                    Custom(mimetypes)
                }
            return Pair(queryMode, mimetypes)
        }
    }
}
