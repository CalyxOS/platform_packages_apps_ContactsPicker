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

import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.android.contactspicker.R
import kotlin.math.roundToInt

/**
 * A vertical slider composable used for fast scrolling through a list.
 *
 * It consists of a draggable handle and a slot for an optional label that displays contextual
 * information (e.g. the current section letter).
 *
 * @param scrubberState The state object that controls the scrubber's position and drag state.
 * @param modifier The [Modifier] to be applied to this composable.
 * @param label A composable lambda that displays content on left side of handle.
 */
@Composable
fun Scrubber(
    scrubberState: ScrubberState,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    var containerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val scrubberHandleHeightPx = with(density) { ScrubberHandleHeight.toPx() }

    val draggableState = rememberDraggableState { delta ->
        if (containerHeightPx > 0) {
            val draggableHeightPx = containerHeightPx - scrubberHandleHeightPx
            if (draggableHeightPx > 0f) {
                val verticalOffsetFractionDelta = delta / draggableHeightPx
                scrubberState.onDrag(verticalOffsetFractionDelta)
            }
        }
    }

    Layout(
        content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                label()
                Handle(draggableState, scrubberState)
            }
        },
        modifier = modifier,
    ) { measurables: List<Measurable>, constraints: Constraints ->
        require(measurables.size == 1) { "Scrubber Layout must have exactly one child." }
        val handleConstraints = constraints.copy(minHeight = 0)
        val placeableHandle = measurables.first().measure(handleConstraints)

        containerHeightPx = constraints.maxHeight

        val handleHeightPx = placeableHandle.height
        val handleWidthPx = placeableHandle.width
        val draggableHeightPx = (containerHeightPx - handleHeightPx)
        // Calculate the y-position to place the handle. This logic ensures the
        // handle stays fully within the bounds of the container.
        val yOffset =
            (draggableHeightPx * scrubberState.verticalOffsetFraction)
                .roundToInt()
                .coerceIn(0, draggableHeightPx)
        val xOffset = (constraints.maxWidth - handleWidthPx)

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeableHandle.placeRelative(x = xOffset, y = yOffset)
        }
    }
}

/**
 * Renders the draggable handle of the scrubber.
 *
 * This composable displays an image for the handle and applies the [draggable] modifier to respond
 * to vertical drag gestures. It updates the [scrubberState] to indicate when a drag gesture starts
 * and stops.
 */
@Composable
private fun Handle(draggableState: DraggableState, scrubberState: ScrubberState) {
    Box(
        modifier =
            Modifier.size(width = ScrubberHandleWidth, height = ScrubberHandleHeight)
                .testTag(SCRUBBER_HANDLE_TEST_TAG)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStarted = { scrubberState.setDragging(true) },
                    onDragStopped = { scrubberState.setDragging(false) },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_scrubber_handle_background),
            contentDescription = stringResource(id = R.string.scrubber_handle_content_description),
            modifier = Modifier.matchParentSize(),
            tint = MaterialTheme.colorScheme.surfaceContainerLowest,
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_scrubber_handle_arrows),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

internal const val SCRUBBER_HANDLE_TEST_TAG = "scrubber_handle"
internal val ScrubberHandleWidth = 55.dp
internal val ScrubberHandleHeight = 70.dp
