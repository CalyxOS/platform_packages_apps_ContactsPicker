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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/** The duration of inactivity in milliseconds before the scrubber automatically hides. */
internal const val SCRUBBER_VISIBILITY_TIMEOUT_MILLIS: Long = 3000L
private const val ENTER_ANIMATION_DURATION_MILLIS: Int = 400
private const val EXIT_ANIMATION_DURATION_MILLIS: Int = 200

/**
 * A composable that wraps the [Scrubber] to provide visibility control with animations.
 *
 * The scrubber becomes visible when the associated [LazyListState] is being scrolled or when the
 * [ScrubberState] handle is being dragged. It automatically hides after a specified
 * [SCRUBBER_VISIBILITY_TIMEOUT_MILLIS] of inactivity.
 *
 * @param scrubberState The state object that controls the scrubber's position and drag state.
 * @param listState The state of the list that this scrubber is controlling. Used to detect scroll
 *   activity.
 * @param modifier The [Modifier] to be applied to this composable.
 * @param label A composable lambda that displays content on side of handle.
 */
@Composable
fun AnimatedScrubber(
    scrubberState: ScrubberState,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }

    val isInteracting = listState.isScrollInProgress || scrubberState.isDragging
    LaunchedEffect(isInteracting) {
        if (isInteracting) {
            isVisible = true
        } else {
            delay(SCRUBBER_VISIBILITY_TIMEOUT_MILLIS)
            isVisible = false
        }
    }

    // TODO(b/464435978): Validate Scrubber animation with UX
    AnimatedVisibility(
        visible = isVisible,
        enter =
            fadeIn(
                animationSpec =
                    tween(
                        durationMillis = ENTER_ANIMATION_DURATION_MILLIS,
                        easing = LinearOutSlowInEasing,
                    )
            ),
        exit =
            fadeOut(
                animationSpec =
                    tween(
                        durationMillis = EXIT_ANIMATION_DURATION_MILLIS,
                        easing = FastOutLinearInEasing,
                    )
            ),
        modifier = modifier,
    ) {
        Scrubber(scrubberState = scrubberState, label = label)
    }
}
