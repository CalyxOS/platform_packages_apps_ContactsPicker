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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ScrubberTest {

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun handleAndLabel_arePlacedAtTop_whenVerticalOffsetFractionIsZero() {
        val state = ScrubberState(initialVerticalOffsetFraction = 0f) {}
        val containerSize = 300.dp

        composeTestRule.setContent {
            Box(modifier = Modifier.size(containerSize)) {
                Scrubber(
                    scrubberState = state,
                    modifier = Modifier.fillMaxHeight().testTag(SCRUBBER_TEST_TAG),
                ) {
                    Text("A", Modifier.testTag(LABEL_TEST_TAG))
                }
            }
        }

        composeTestRule
            .onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG)
            .assertTopPositionInRootIsEqualTo(0.dp)
        assertHandleAndLabelAreVerticallyCentered()
        assertHandleIsHorizontallyAtTheEnd()
    }

    @Test
    fun handleAndLabel_arePlacedAtBottom_whenVerticalOffsetFractionIsOne() {
        val state = ScrubberState(initialVerticalOffsetFraction = 1f) {}
        val containerSize = 300.dp

        composeTestRule.setContent {
            Box(modifier = Modifier.size(containerSize)) {
                Scrubber(
                    scrubberState = state,
                    modifier = Modifier.fillMaxHeight().testTag(SCRUBBER_TEST_TAG),
                ) {
                    Text("A", Modifier.testTag(LABEL_TEST_TAG))
                }
            }
        }

        val expectedY = containerSize - ScrubberHandleHeight
        val handleNode = composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG)
        handleNode.assertTopPositionInRootIsEqualTo(expectedY)
        assertHandleAndLabelAreVerticallyCentered()
        assertHandleIsHorizontallyAtTheEnd()
    }

    @Test
    fun handleAndLabel_arePlacedInMiddle_whenFractionIsHalf() {
        val state = ScrubberState(initialVerticalOffsetFraction = 0.5f) {}
        val containerSize = 300.dp

        composeTestRule.setContent {
            Box(modifier = Modifier.size(containerSize)) {
                Scrubber(
                    scrubberState = state,
                    modifier = Modifier.fillMaxHeight().testTag(SCRUBBER_TEST_TAG),
                ) {
                    Text("A", Modifier.testTag(LABEL_TEST_TAG))
                }
            }
        }

        val expectedY = (containerSize - ScrubberHandleHeight) * 0.5f
        val handleNode = composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG)
        handleNode.assertTopPositionInRootIsEqualTo(expectedY)
        assertHandleAndLabelAreVerticallyCentered()
        assertHandleIsHorizontallyAtTheEnd()
    }

    @Test
    fun onDrag_updatesFraction_whenDraggingDownwards() {
        var lastNotifiedDeltaFraction: Float? = null
        val initialFraction = 0f
        val state =
            ScrubberState(initialFraction) { deltaFraction ->
                lastNotifiedDeltaFraction = deltaFraction
            }

        val containerSize = 300.dp
        val containerSizePx = with(composeTestRule.density) { containerSize.toPx() }
        val scrubberHandleHeightPx = with(composeTestRule.density) { ScrubberHandleHeight.toPx() }

        composeTestRule.setContent {
            Box(modifier = Modifier.size(containerSize)) {
                Scrubber(scrubberState = state, modifier = Modifier.fillMaxHeight(), label = {})
            }
        }

        val handleNode = composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG)
        handleNode.assertIsDisplayed()
        handleNode.performTouchInput {
            down(center)
            moveBy(Offset(0f, 100f))
        }
        val draggableHeight = containerSizePx - scrubberHandleHeightPx
        val expectedFraction = 100f / draggableHeight
        /**
         * The reported drag delta may not exactly match the drag distance due to the touch slop
         * implemented in Jetpack Compose's gesture detector. The detector consumes an initial part
         * of the drag to recognize the gesture to distinguish it from a tap. This results in a
         * discrepancy between the actual distance moved and the reported delta. To make the test
         * resilient to this, we assert with a tolerance.
         */
        assertThat(lastNotifiedDeltaFraction).isWithin(0.6f).of(expectedFraction)
    }

    @Test
    fun onDrag_updatesFractionOnDraggingUpwards() {
        var lastNotifiedDeltaFraction: Float? = null
        val initialFraction = 0.5f
        val state =
            ScrubberState(initialFraction) { deltaFraction ->
                lastNotifiedDeltaFraction = deltaFraction
            }

        val containerSize = 300.dp
        val containerSizePx = with(composeTestRule.density) { containerSize.toPx() }
        val scrubberHandleHeightPx = with(composeTestRule.density) { ScrubberHandleHeight.toPx() }

        composeTestRule.setContent {
            Box(modifier = Modifier.size(containerSize)) {
                Scrubber(scrubberState = state, modifier = Modifier.fillMaxHeight(), label = {})
            }
        }

        val handleNode = composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG)
        handleNode.assertIsDisplayed()
        handleNode.performTouchInput {
            down(center)
            moveBy(Offset(0f, -100f))
        }
        val draggableHeight = containerSizePx - scrubberHandleHeightPx
        val expectedFraction = -100f / draggableHeight
        /**
         * The reported drag delta may not exactly match the drag distance due to the touch slop
         * implemented in Jetpack Compose's gesture detector. The detector consumes an initial part
         * of the drag to recognize the gesture to distinguish it from a tap. This results in a
         * discrepancy between the actual distance moved and the reported delta. To make the test
         * resilient to this, we assert with a tolerance.
         */
        assertThat(lastNotifiedDeltaFraction).isWithin(0.6f).of(expectedFraction)
    }

    @Test
    fun onDrag_updatesIsDraggingState() {
        val state = ScrubberState(initialVerticalOffsetFraction = 0f) {}

        composeTestRule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                Scrubber(
                    scrubberState = state,
                    modifier = Modifier.testTag(SCRUBBER_TEST_TAG),
                    label = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        val handleNode = composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG)
        handleNode.performTouchInput {
            down(center)
            moveBy(Offset(0f, 50f))
        }

        composeTestRule.waitForIdle()
        assertThat(state.isDragging).isTrue()

        handleNode.performTouchInput { up() }
        composeTestRule.waitForIdle()
        assertThat(state.isDragging).isFalse()
    }

    /**
     * Asserts that the handle and label are vertically centered with respect to each other. This is
     * because they are in a Row with `verticalAlignment = Alignment.CenterVertically`.
     */
    private fun assertHandleAndLabelAreVerticallyCentered() {
        val handleBounds = composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG).getBoundsInRoot()
        val labelBounds = composeTestRule.onNodeWithTag(LABEL_TEST_TAG).getBoundsInRoot()

        // Calculate the vertical center in Dp
        val handleCenterY = handleBounds.top + (handleBounds.height / 2)
        val labelCenterY = labelBounds.top + (labelBounds.height / 2)

        assertThat(handleCenterY.value).isWithin(1f).of(labelCenterY.value)
    }

    /**
     * Asserts that the handle is placed at the horizontal end of the Scrubber container, respecting
     * the current layout direction (LTR or RTL).
     */
    private fun assertHandleIsHorizontallyAtTheEnd() {
        val scrubberNode = composeTestRule.onNodeWithTag(SCRUBBER_TEST_TAG)
        val scrubberBounds = scrubberNode.getBoundsInRoot()
        val handleBounds = composeTestRule.onNodeWithTag(SCRUBBER_HANDLE_TEST_TAG).getBoundsInRoot()

        if (scrubberNode.fetchSemanticsNode().layoutInfo.layoutDirection == LayoutDirection.Ltr) {
            // In LTR, the end is the right edge.
            assertThat(handleBounds.right.value).isWithin(1f).of(scrubberBounds.right.value)
        } else { // LayoutDirection.Rtl
            // In RTL, the end is the left edge.
            assertThat(handleBounds.left.value).isWithin(1f).of(scrubberBounds.left.value)
        }
    }
}

private const val LABEL_TEST_TAG = "scrubber_label"
private const val SCRUBBER_TEST_TAG = "scrubber"
