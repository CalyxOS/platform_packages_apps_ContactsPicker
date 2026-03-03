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
import com.android.contactspicker.data.model.SectionKey
import kotlin.math.abs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

private const val SCRUBBER_VERTICAL_OFFSET_FRACTION_UPDATE_THRESHOLD = 0.001f

/**
 * A state holder and controller for the scrubber, responsible for managing the interaction with the
 * scrubber position and drag
 *
 * @param contactSections Source data used for index-to-position mapping.
 * @param showPrivacyBanner A flag indicating if the privacy banner is displayed. This is used to
 *   correctly calculate list indices from contact indices, as the banner adds an offset to the item
 *   positions.
 */
class ScrubberController(
    contactSections: Map<SectionKey, List<Contact>>,
    showPrivacyBanner: Boolean,
) {
    // TODO(b/489313689) : Reuse ScrubberPositionToListIndexMapper to find SectionKey instead of
    // creating a new list
    private val contactIndexToSectionKey = buildList {
        contactSections.forEach { (sectionKey, contacts) ->
            repeat(contacts.size) { add(sectionKey) }
        }
    }
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

        contactIndexToSectionKey.getOrNull(contactIndex)
    }

    /**
     * Updates the scrubber's vertical offset fraction to reflect the current list scroll position.
     * It converts the [preciseListIndex] into a vertical fraction (0.0 to 1.0) and updates the
     * [scrubberState] if the change exceeds [SCRUBBER_VERTICAL_OFFSET_FRACTION_UPDATE_THRESHOLD].
     *
     * To prevent conflicting state updates, this operation is skipped while the user is dragging
     * the scrubber. The scrubber's position is either driven by the user's drag gesture or by the
     * list's scroll position, but never both simultaneously.
     *
     * Note: The [preciseListIndex] represents the index of the first visible item plus the fraction
     * of that item that has been scrolled past the top edge of the viewport. This ensures that the
     * scrubber position is updated continuously as the user scrolls, rather than jumping between
     * item indices.
     *
     * @param preciseListIndex The fractional index of the first visible item in the list.
     */
    internal fun updateVerticalOffsetFraction(preciseListIndex: Float) {
        if (!scrubberState.isDragging) {
            val targetFraction =
                scrubberPositionToListIndexMapper.toVerticalOffsetFraction(
                    preciseListIndex = preciseListIndex
                )
            if (
                abs(targetFraction - scrubberState.verticalOffsetFraction) >
                    SCRUBBER_VERTICAL_OFFSET_FRACTION_UPDATE_THRESHOLD
            ) {
                scrubberState.setVerticalOffsetFraction(targetFraction)
            }
        }
    }
}

/**
 * Creates and remembers a [ScrubberController]
 *
 * @param contactSections Source data used for index-to-position mapping.
 * @param showPrivacyBanner A flag indicating if the privacy banner is displayed. This is used to
 *   correctly calculate list indices from contact indices, as the banner adds an offset to the item
 *   positions.
 */
@Composable
internal fun rememberScrubberController(
    contactSections: Map<SectionKey, List<Contact>>,
    showPrivacyBanner: Boolean,
): ScrubberController {
    return remember(contactSections, showPrivacyBanner) {
        ScrubberController(contactSections = contactSections, showPrivacyBanner = showPrivacyBanner)
    }
}
