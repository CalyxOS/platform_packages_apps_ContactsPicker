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

import androidx.room.TypeConverter
import com.android.contactspicker.data.model.MimeType
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
    private val mimeTypeToBitPosition: Map<MimeType, Int> =
        mapOf(
            MimeType.STRUCTURED_NAME to 0,
            MimeType.PHONE to 1,
            MimeType.EMAIL to 2,
            MimeType.STRUCTURED_POSTAL to 3,
            MimeType.ORGANIZATION to 4,
            MimeType.RELATION to 5,
            MimeType.EVENT to 6,
            MimeType.PHOTO to 7,
            MimeType.GROUP_MEMBERSHIP to 8,
            MimeType.WEBSITE to 9,
            MimeType.NICKNAME to 10,
            // TODO: agree with the team to "break" for the team members and use 11 for CONTACTS
            // or use 13 and reserve position 11 and 12 for no longer existing Phone and Email
            // CONTENT_TYPE. I am voting for just removing the Phone and Email CONTENT_TYPE since
            // the project is still only in development and only teamfood devices might have any
            // entries in the DB.
            MimeType.CONTACTS to 11,
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
    fun fromMimeTypeList(mimeTypes: List<MimeType>): Int {
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
    fun toMimeTypeList(bitmask: Int): List<MimeType> {
        val mimeTypes = mutableListOf<MimeType>()
        for ((mimeType, bitPosition) in mimeTypeToBitPosition) {
            if (bitmask and (1 shl bitPosition) != 0) {
                mimeTypes.add(mimeType)
            }
        }
        return mimeTypes
    }
}
