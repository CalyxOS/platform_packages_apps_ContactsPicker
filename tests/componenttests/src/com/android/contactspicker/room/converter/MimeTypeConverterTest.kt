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

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.android.contactspicker.data.model.MimeType
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(JUnit4::class)
class MimeTypeConverterTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val mimeTypeConverter = MimeTypeConverter()

    @Test
    fun fromMimeTypeList_eachSupportedType_mapsToUniqueBitmask() {
        val generatedBitmasks =
            MimeType.entries.map { mimeType ->
                mimeTypeConverter.fromMimeTypeList(listOf(mimeType))
            }

        // Verify that no supported MIME type produces a 0 bitmask, which would indicate it's
        // not being mapped correctly.
        assertThat(generatedBitmasks).doesNotContain(0)

        val uniqueBitmasks = generatedBitmasks.toSet()
        assertThat(uniqueBitmasks.size).isEqualTo(MimeType.entries.size)
    }

    @Test
    fun fromMimeTypeList_multipleValidTypes_returnsCorrectBitmask() {
        val mimeTypes =
            listOf(
                MimeType.EMAIL, // bit 2
                MimeType.PHOTO, // bit 7
            )
        val expectedBitmask = (1 shl 2) or (1 shl 7)
        val bitmask = mimeTypeConverter.fromMimeTypeList(mimeTypes)
        assertThat(bitmask).isEqualTo(expectedBitmask)
    }

    @Test
    fun fromMimeTypeList_emptyList_returnsZeroBitmask() {
        // Verifies that converting an empty list of MimeTypes results in a bitmask of 0.
        val bitmask = mimeTypeConverter.fromMimeTypeList(emptyList())
        // An empty list should produce a bitmask with no bits set.
        assertThat(bitmask).isEqualTo(0)
    }

    @Test
    fun fromMimeTypeList_duplicateTypes_handledCorrectly() {
        // Verifies that duplicate MimeTypes in the input list are handled gracefully.
        val mimeTypes =
            listOf(
                MimeType.PHONE, // bit 1
                MimeType.EMAIL, // bit 2
                MimeType.PHONE, // duplicate
            )
        // The expected bitmask should be the same as if there were no duplicates.
        val expectedBitmask = (1 shl 1) or (1 shl 2)
        val bitmask = mimeTypeConverter.fromMimeTypeList(mimeTypes)
        // The conversion should correctly produce the bitmask for PHONE and EMAIL.
        assertThat(bitmask).isEqualTo(expectedBitmask)
    }

    @Test
    fun toMimeTypeList_bitmaskWithUnusedBits_ignoresUnusedBits() {
        // Verifies that bits in the bitmask that do not correspond to any known MimeType are
        // ignored.
        val unusedBitPosition = 30
        // Create a bitmask with a known type (EMAIL, bit 2) and an unknown bit.
        val bitmask = (1 shl 2) or (1 shl unusedBitPosition)
        val expectedMimeTypes = listOf(MimeType.EMAIL)
        val mimeTypes = mimeTypeConverter.toMimeTypeList(bitmask)
        // The result should only contain the MimeType for the known bit.
        assertThat(mimeTypes).containsExactlyElementsIn(expectedMimeTypes)
    }

    @Test
    fun toMimeTypeList_zeroBitmask_returnsEmptyList() {
        val mimeTypes = mimeTypeConverter.toMimeTypeList(0)
        assertThat(mimeTypes).isEmpty()
    }

    @Test
    fun toMimeTypeList_eachSingleBit_mapsToUniqueType() {
        val allConvertedMimeTypes = mutableListOf<MimeType>()
        MimeType.entries.forEachIndexed { index, _ ->
            val bitmask = 1 shl index
            val convertedList = mimeTypeConverter.toMimeTypeList(bitmask)
            // Each single-bit bitmask should correspond to exactly one MIME type.
            assertThat(convertedList).hasSize(1)
            allConvertedMimeTypes.addAll(convertedList)
        }

        // Check that the list of all converted MIME types contains exactly the same elements
        // as the original list of supported types, confirming a unique 1-to-1 mapping.
        assertThat(allConvertedMimeTypes).containsExactlyElementsIn(MimeType.entries)
    }

    @Test
    fun toMimeTypeList_multipleBitsSet_returnsCorrectTypes() {
        // Bitmask for Email (bit 2) and Photo (bit 7)
        val bitmask = (1 shl 2) or (1 shl 7)
        val expectedMimeTypes = listOf(MimeType.EMAIL, MimeType.PHOTO)
        val mimeTypes = mimeTypeConverter.toMimeTypeList(bitmask)
        assertThat(mimeTypes).containsExactlyElementsIn(expectedMimeTypes)
    }

    @Test
    fun conversion_isReversible() {
        val bitmask = mimeTypeConverter.fromMimeTypeList(MimeType.entries)
        val convertedMimeTypes = mimeTypeConverter.toMimeTypeList(bitmask)

        assertThat(convertedMimeTypes).containsExactlyElementsIn(MimeType.entries)
    }
}
