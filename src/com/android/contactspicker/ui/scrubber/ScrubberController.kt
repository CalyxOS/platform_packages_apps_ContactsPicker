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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.ui.pickerscreen.SectionKey
import com.android.contactspicker.ui.pickerscreen.getSectionKeyForNonFavorite
import java.util.SortedMap
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * A state holder and controller for the scrubber, responsible for managing the interaction with the
 * scrubber position and drag
 *
 * @param contactSections Source data used for index-to-position mapping.
 * @param numberOfFavoriteContacts Count of favorites (Used in scrubber label mapping).
 * @param showPrivacyBanner A flag indicating if the privacy banner is displayed. This is used to
 *   correctly calculate list indices from contact indices, as the banner adds an offset to the item
 *   positions.
 */
class ScrubberController(
    contactSections: SortedMap<SectionKey, List<Contact>>,
    val numberOfFavoriteContacts: Int,
    showPrivacyBanner: Boolean,
) {
    private val flattenedContacts = contactSections.values.flatten()
    private val scrollRequestChannel = Channel<Int>(Channel.CONFLATED)
    /** A flow emitting target list indices for scrolling. */
    val scrollRequests: Flow<Int> = scrollRequestChannel.receiveAsFlow()

    private val scrubberPositionToListIndexMapper =
        ScrubberPositionToListIndexMapper(contactSections, showPrivacyBanner)

    /** Manages the scrubber's position and drag state. */
    val scrubberState =
        ScrubberState(
            initialVerticalOffsetFraction = 0f,
            onVerticalOffsetFractionChanged = { newFraction ->
                val newListIndex = scrubberPositionToListIndexMapper.toListIndex(newFraction)
                scrollRequestChannel.trySend(newListIndex)
            },
        )

    /**
     * Determines if user-initiated scrolling on the `LazyColumn` should be enabled.
     *
     * Scrolling is disabled when the user is actively dragging the scrubber handle to prevent
     * conflicting scroll inputs between the list and the scrubber.
     */
    val isListScrollEnabledForUser: Boolean
        get() = !scrubberState.isDragging

    /**
     * The [SectionKey] for the content currently under the scrubber handle.
     *
     * This value is derived from the scrubber's drag position and is used to display the
     * appropriate label (e.g., 'A', 'B', or a favorite/emoji icon) next to the handle. It is null
     * when the user is not dragging.
     */
    val labelSectionKey: SectionKey? by derivedStateOf {
        if (!scrubberState.isDragging) return@derivedStateOf null

        val contactIndex =
            scrubberPositionToListIndexMapper.getContactIndexFromFraction(
                scrubberState.verticalOffsetFraction
            )

        if (contactIndex < numberOfFavoriteContacts) {
            SectionKey.FavoriteIconKey
        } else {
            flattenedContacts[contactIndex].getSectionKeyForNonFavorite()
        }
    }

    /**
     * Allows updating the scrubber position based on the list index if user is not dragging the
     * scrubber
     */
    internal fun updateVerticalOffsetFraction(listIndex: Int) {
        if (!scrubberState.isDragging) {
            scrubberState.setVerticalOffsetFraction(
                scrubberPositionToListIndexMapper.toVerticalOffsetFraction(listIndex)
            )
        }
    }
}

/**
 * Creates and remembers a [ScrubberController]
 *
 * @param contactSections Source data used for index-to-position mapping.
 * @param numberOfFavoriteContacts Count of favorites (Used in scrubber label mapping).
 * @param showPrivacyBanner A flag indicating if the privacy banner is displayed. This is used to
 *   correctly calculate list indices from contact indices, as the banner adds an offset to the item
 *   positions.
 */
@Composable
internal fun rememberScrubberController(
    contactSections: SortedMap<SectionKey, List<Contact>>,
    numberOfFavoriteContacts: Int,
    showPrivacyBanner: Boolean,
): ScrubberController {
    return remember(contactSections, numberOfFavoriteContacts, showPrivacyBanner) {
        ScrubberController(
            contactSections = contactSections,
            numberOfFavoriteContacts = numberOfFavoriteContacts,
            showPrivacyBanner = showPrivacyBanner,
        )
    }
}
