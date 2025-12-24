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

import android.provider.ContactsContract

/**
 * An enum that represents the mime types of contact data fields supported by the Contacts Picker.
 *
 * @param value The string representation of the mime type, as defined in
 *   [android.provider.ContactsContract].
 */
enum class MimeType(val value: String) {
    STRUCTURED_NAME(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
    PHONE(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
    EMAIL(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE),
    STRUCTURED_POSTAL(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE),
    ORGANIZATION(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
    RELATION(ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE),
    EVENT(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE),
    PHOTO(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
    GROUP_MEMBERSHIP(ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE),
    WEBSITE(ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE),
    NICKNAME(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE),
    CONTACTS(ContactsContract.Contacts.CONTENT_TYPE);

    /**
     * Validates that a given MimeType is supported for ACTION_PICK_CONTACTS.
     *
     * @throws IllegalArgumentException if the MimeType is not supported.
     */
    fun validateForActionPickContacts() {
        if (this !in ACTION_PICK_CONTACTS_SUPPORTED) {
            throw IllegalArgumentException(
                "MimeType '${this.name}' is not allowed for ACTION_PICK_CONTACTS"
            )
        }
    }

    companion object {
        /** A subset of MimeType values that are supported by ACTION_PICK_CONTACTS. */
        val ACTION_PICK_CONTACTS_SUPPORTED = entries.filter { it != CONTACTS }.toSet()

        /**
         * Converts a mime type string to a [MimeType] enum.
         *
         * @throws IllegalArgumentException if the string does not match any known mime type.
         */
        fun fromString(mimeTypeString: String): MimeType {
            return when (mimeTypeString) {
                // Phone and Email CONTENT_TYPE maps to the same value as CONTENT_ITEM_TYPE.
                ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE -> PHONE
                ContactsContract.CommonDataKinds.Email.CONTENT_TYPE -> EMAIL
                else -> entries.find { it.value == mimeTypeString }
            } ?: throw IllegalArgumentException("Unknown or unsupported mimetype: $mimeTypeString")
        }
    }
}
