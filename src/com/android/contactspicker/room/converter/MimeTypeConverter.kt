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
package com.android.contactspicker.room.converter

import android.provider.ContactsContract
import androidx.room.TypeConverter
import kotlin.collections.iterator

/**
 * Provides [TypeConverter]s to allow Room to store a list of contact data MIME types as a single
 * integer bitmask.
 *
 * This is an efficient way to persist a set of predefined strings in the database.
 */
class MimeTypeConverter {

    /**
     * Defines the mapping from a contact data MIME type to its unique bit position within the
     * integer bitmask.
     *
     * Note: Re-ordering or changing the bit positions in this map will break backward compatibility
     * for existing data stored in the Room database. New MIME types should be added with
     * incrementing bit positions.
     */
    private val mimeTypeToBitPosition: Map<String, Int> =
        mapOf(
            // Mimetypes supported for ACTION_PICK_CONTACTS
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE to 0,
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE to 1,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE to 2,
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE to 3,
            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE to 4,
            ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE to 5,
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE to 6,
            ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE to 7,
            ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE to 8,
            ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE to 9,
            ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE to 10,
            // Mimetypes supported for ACTION_PICK
            ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE to 11,
            ContactsContract.CommonDataKinds.Email.CONTENT_TYPE to 12,
            ContactsContract.Contacts.CONTENT_TYPE to 13,
        )

    /**
     * Converts a list of MIME type strings into an integer bitmask representation.
     *
     * Each MIME type in the list corresponds to a specific bit in the returned integer. If a MIME
     * type is present in the list, its corresponding bit is set to 1.
     *
     * @param mimeTypes The list of MIME type strings to convert.
     * @return An integer bitmask representing the provided list of MIME types.
     */
    @TypeConverter
    fun fromMimeTypeList(mimeTypes: List<String>): Int {
        var bitmask = 0
        for (mimeType in mimeTypes) {
            val bitPosition = mimeTypeToBitPosition[mimeType]
            if (bitPosition != null) {
                bitmask = bitmask or (1 shl bitPosition)
            }
        }
        return bitmask
    }

    /**
     * Converts an integer bitmask back into a list of MIME type strings.
     *
     * Each bit set to 1 in the bitmask corresponds to a specific MIME type, which is added to the
     * returned list.
     *
     * @param bitmask The integer bitmask to convert.
     * @return A list of MIME type strings represented by the bitmask.
     */
    @TypeConverter
    fun toMimeTypeList(bitmask: Int): List<String> {
        val mimeTypes = mutableListOf<String>()
        for ((mimeType, bitPosition) in mimeTypeToBitPosition) {
            if (bitmask and (1 shl bitPosition) != 0) {
                mimeTypes.add(mimeType)
            }
        }
        return mimeTypes
    }
}
