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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A state holder for a vertical scrubber, responsible for managing its position and drag state.
 *
 * @param initialVerticalOffsetFraction The initial vertical position of the scrubber, represented
 *   as a fraction from 0.0f (top) to 1.0f (bottom).
 * @param onVerticalOffsetFractionChanged A callback invoked when the scrubber's position is changed
 *   by a user drag gesture.
 */
class ScrubberState(
    @FloatRange(from = 0.0, to = 1.0) initialVerticalOffsetFraction: Float,
    private val onVerticalOffsetFractionChanged: (newFraction: Float) -> Unit,
) {
    private var _isDragging: Boolean by mutableStateOf(false)
    /** Indicates whether the user is currently dragging the scrubber handle. */
    val isDragging: Boolean
        get() = _isDragging

    private var _verticalOffsetFraction by mutableFloatStateOf(initialVerticalOffsetFraction)
    /**
     * The current vertical position of the scrubber, as a fraction from 0.0f (top) to 1.0f
     * (bottom).
     */
    val verticalOffsetFraction: Float
        get() = _verticalOffsetFraction

    /**
     * Updates the scrubber's position based on a drag gesture. This method also invokes the
     * `onVerticalOffsetFractionChanged` callback.
     *
     * @param verticalOffsetDragFractionDelta The change in vertical position as a fraction of the
     *   total scrubber height.
     */
    internal fun onDrag(verticalOffsetDragFractionDelta: Float) {
        val newVerticalOffsetFraction = _verticalOffsetFraction + verticalOffsetDragFractionDelta
        updateFraction(newVerticalOffsetFraction, notifyListener = true)
    }

    /**
     * Sets the vertical offset fraction of the scrubber. This method does not invoke the
     * `onVerticalOffsetFractionChanged` callback.
     *
     * @param newVerticalOffsetFraction The new vertical position as a fraction from 0.0f (top) to
     *   1.0f (bottom).
     */
    internal fun setVerticalOffsetFraction(
        @FloatRange(from = 0.0, to = 1.0) newVerticalOffsetFraction: Float
    ) {
        updateFraction(newVerticalOffsetFraction, notifyListener = false)
    }

    private fun updateFraction(newVerticalOffsetFraction: Float, notifyListener: Boolean) {
        val coercedFraction = newVerticalOffsetFraction.coerceIn(0f, 1f)

        if (_verticalOffsetFraction != coercedFraction) {
            _verticalOffsetFraction = coercedFraction
            if (notifyListener) {
                onVerticalOffsetFractionChanged(coercedFraction)
            }
        }
    }

    fun setDragging(isDragging: Boolean) {
        _isDragging = isDragging
    }
}
