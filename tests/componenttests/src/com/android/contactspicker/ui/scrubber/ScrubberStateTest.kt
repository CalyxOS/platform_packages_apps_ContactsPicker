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

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(JUnit4::class)
class ScrubberStateTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private var lastNotifiedFraction: Float? = null
    private val onChangeCallback = { newFraction: Float -> lastNotifiedFraction = newFraction }

    @Test
    fun initialState_isCorrect() {
        val state = ScrubberState(0.5f, onChangeCallback)
        assertThat(state.verticalOffsetFraction).isEqualTo(0.5f)
        assertThat(state.isDragging).isFalse()
    }

    @Test
    fun onDrag_updatesFractionAndNotifiesListener() {
        val state = ScrubberState(0.5f, onChangeCallback)
        state.onDrag(0.1f)
        assertThat(state.verticalOffsetFraction).isEqualTo(0.6f)
        assertThat(lastNotifiedFraction).isEqualTo(0.6f)
    }

    @Test
    fun onDrag_coercesFractionToBeWithinBounds() {
        val state = ScrubberState(0.9f, onChangeCallback)
        state.onDrag(0.2f)
        assertThat(state.verticalOffsetFraction).isEqualTo(1.0f)
        assertThat(lastNotifiedFraction).isEqualTo(1.0f)

        state.onDrag(-2.0f)
        assertThat(state.verticalOffsetFraction).isEqualTo(0.0f)
        assertThat(lastNotifiedFraction).isEqualTo(0.0f)
    }

    @Test
    fun setVerticalOffsetFraction_updatesFractionButDoesNotNotify() {
        val state = ScrubberState(0.5f, onChangeCallback)
        state.setVerticalOffsetFraction(0.8f)
        assertThat(state.verticalOffsetFraction).isEqualTo(0.8f)
        assertThat(lastNotifiedFraction).isNull()
    }

    @Test
    fun setVerticalOffsetFraction_coercesFractionToBeWithinBounds() {
        val state = ScrubberState(0.5f, onChangeCallback)
        state.setVerticalOffsetFraction(1.5f)
        assertThat(state.verticalOffsetFraction).isEqualTo(1.0f)

        state.setVerticalOffsetFraction(-0.5f)
        assertThat(state.verticalOffsetFraction).isEqualTo(0.0f)
    }

    @Test
    fun setDragging_updatesIsDraggingState() {
        val state = ScrubberState(0.5f, onChangeCallback)
        state.setDragging(true)
        assertThat(state.isDragging).isTrue()
        state.setDragging(false)
        assertThat(state.isDragging).isFalse()
    }
}
