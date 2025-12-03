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
package com.android.contactspicker.ui.scrubber

import androidx.annotation.FloatRange
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.ui.pickerscreen.SectionKey
import java.util.SortedMap
import kotlin.math.roundToInt

private const val STICKY_HEADER_OFFSET = 1
private const val PRIVACY_BANNER_OFFSET = 1

// TODO(b/465962685): Replace linear search using indexOfLast with binary search

/**
 * A class responsible for bidirectional translation between the scrubber handle's vertical position
 * (represented as a `scrubberVerticalOffsetFraction`) and the corresponding index in the
 * `LazyColumn`.
 *
 * This is used in two main scenarios:
 * 1. When the user scrolls the contact list, the `listIndex` of the first visible item is
 *    translated into a `scrubberVerticalOffsetFraction` to update the scrubber handle's position.
 * 2. When the user drags the scrubber handle, the `scrubberVerticalOffsetFraction` is translated
 *    into a `listIndex` to scroll the `LazyColumn` to the correct position.
 */
internal class ScrubberPositionToListIndexMapper(
    contactSections: SortedMap<SectionKey, List<Contact>>,
    showPrivacyBanner: Boolean,
) {
    /** The total number of contacts across all sections. */
    private val contactCount: Int
    /** Stores the starting index of each section within the flat list of contacts. */
    private val contactSectionStartIndices: IntArray

    /**
     * Stores the number of non-contact items that appear before each section in the `LazyColumn`.
     * This includes the `PRIVACY_BANNER_OFFSET` (if shown) and a `STICKY_HEADER_OFFSET` for each
     * section.
     */
    private val nonContactItemsBeforeSection: IntArray

    /**
     * Pre-calculates lookup tables to efficiently map between a contact's index in a conceptual
     * flat list and its actual index in the `LazyColumn`. This is necessary because the
     * `LazyColumn` contains non-contact items (like a privacy banner and sticky headers) that
     * offset the real indices.
     */
    init {
        val sectionCount = contactSections.size
        val startIndices = IntArray(sectionCount)
        val nonContactItems = IntArray(sectionCount)
        var _contactCount = 0

        var currentContactIndex = 0
        var currentNonContactItemsCount = STICKY_HEADER_OFFSET
        currentNonContactItemsCount += if (showPrivacyBanner) PRIVACY_BANNER_OFFSET else 0

        contactSections.values.forEachIndexed { index, currentSectionContacts ->
            val currentSectionContactsCount = currentSectionContacts.size
            startIndices[index] = currentContactIndex
            nonContactItems[index] = currentNonContactItemsCount

            currentContactIndex += currentSectionContactsCount
            currentNonContactItemsCount += STICKY_HEADER_OFFSET
            _contactCount += currentSectionContactsCount
        }

        contactSectionStartIndices = startIndices
        nonContactItemsBeforeSection = nonContactItems
        contactCount = _contactCount
    }

    /**
     * Converts the scrubber's fractional vertical offset into a `LazyColumn` list index.
     *
     * @param scrubberVerticalOffsetFraction The vertical position of the scrubber handle, from 0.0f
     *   (top) to 1.0f (bottom).
     * @return The corresponding index in the `LazyColumn` that should be scrolled to.
     */
    fun toListIndex(@FloatRange(from = 0.0, to = 1.0) scrubberVerticalOffsetFraction: Float): Int {
        if (contactCount == 0) return 0
        val contactIndex = getContactIndexFromFraction(scrubberVerticalOffsetFraction)

        // Edge case when we have banner on top we want to map fraction 0 to top of list
        // state instead of first contact on screen
        if (contactIndex == 0) {
            return 0
        }

        return getListIndexFromContactIndex(contactIndex)
    }

    /**
     * Converts the `LazyColumn`'s list index into a fractional vertical offset for the scrubber
     * handle.
     *
     * @param listIndex The index of item in the `LazyColumn`.
     * @return The corresponding vertical offset for the scrubber handle, from 0.0f to 1.0f.
     */
    fun toVerticalOffsetFraction(listIndex: Int): Float {
        if (contactCount <= 1) return 0f

        val sectionIndex = findSectionIndex(listIndex)

        // If listIndex is before contact rows and sticky header (e.g., on the banner).
        // we will not find it's section as section tracks only contacts, in that case
        // we assume the input index is at the top of the list and map it to top position with 0f
        if (sectionIndex == -1) {
            return 0f
        }
        val maxContactIndex = contactCount - 1
        val listOffset = nonContactItemsBeforeSection[sectionIndex]
        val contactIndex = (listIndex - listOffset).coerceIn(0, maxContactIndex)

        return contactIndex.toFloat() / maxContactIndex
    }

    private fun getContactIndexFromFraction(verticalOffsetFraction: Float): Int {
        val maxIndex = contactCount - 1
        return (maxIndex * verticalOffsetFraction).roundToInt().coerceIn(0, maxIndex)
    }

    private fun findSectionIndex(listIndex: Int): Int {
        return contactSectionStartIndices.indices.indexOfLast {
            (contactSectionStartIndices[it] + nonContactItemsBeforeSection[it]) <= listIndex
        }
    }

    private fun getListIndexFromContactIndex(contactIndex: Int): Int {
        val sectionIndex = contactSectionStartIndices.indexOfLast { it <= contactIndex }
        val nonContactItemsCount = nonContactItemsBeforeSection[sectionIndex]
        return contactIndex + nonContactItemsCount
    }
}
