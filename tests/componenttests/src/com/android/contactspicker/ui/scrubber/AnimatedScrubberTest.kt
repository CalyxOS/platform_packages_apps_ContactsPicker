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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class AnimatedScrubberTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun scrubber_isInitiallyNotVisible() {
        composeTestRule.setContent {
            AnimatedScrubber(
                scrubberState = ScrubberState(0f) {},
                listState = rememberLazyListState(),
                modifier = Modifier.testTag(SCRUBBER_TEST_TAG),
                label = {},
            )
        }

        composeTestRule.onNodeWithTag(SCRUBBER_TEST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun scrubber_becomesVisible_whenListIsScrolled_andHidesAfterTimeout() {
        lateinit var listState: LazyListState
        composeTestRule.setContent {
            listState = rememberLazyListState()
            TestLayout(listState)
        }

        assertScrubberVisibilityAndTimeout(
            startAction = {
                // Start a scroll gesture. This will set isScrollInProgress to true.
                composeTestRule.onNodeWithTag(LAZY_COLUMN_TEST_TAG).performTouchInput { swipeUp() }
            },
            stopAction = {
                // Wait for the scroll to finish and the list to settle.
                composeTestRule.waitForIdle()
            },
        )
    }

    @Test
    fun scrubber_becomesVisible_whenScrubberIsDragged_andHidesAfterTimeout() {
        val scrubberState = ScrubberState(0f) {}
        composeTestRule.setContent {
            AnimatedScrubber(
                scrubberState = scrubberState,
                listState = rememberLazyListState(),
                modifier = Modifier.testTag(SCRUBBER_TEST_TAG),
                label = { Text("A") },
            )
        }

        assertScrubberVisibilityAndTimeout(
            startAction = { composeTestRule.runOnIdle { scrubberState.setDragging(true) } },
            stopAction = { composeTestRule.runOnIdle { scrubberState.setDragging(false) } },
        )
    }

    @Test
    fun scrubber_timeoutResets_whenNewVisibilityTriggerOccurs() {
        val scrubberState = ScrubberState(0f) {}
        lateinit var listState: LazyListState

        composeTestRule.setContent {
            listState = rememberLazyListState()
            TestLayout(listState, scrubberState)
        }

        // 1. Make scrubber visible by dragging
        composeTestRule.runOnIdle { scrubberState.setDragging(true) }
        composeTestRule.onNodeWithTag(SCRUBBER_TEST_TAG).assertIsDisplayed()

        // 2. Stop dragging to start the visibility timer
        composeTestRule.runOnIdle { scrubberState.setDragging(false) }

        // 3. Advance clock part-way and assert it's still visible
        composeTestRule.mainClock.advanceTimeBy(SCRUBBER_VISIBILITY_TIMEOUT_MILLIS / 2)
        composeTestRule.onNodeWithTag(SCRUBBER_TEST_TAG).assertIsDisplayed()

        // 4. Before the timeout completes, simulate a list scroll
        composeTestRule.onNodeWithTag(LAZY_COLUMN_TEST_TAG).performTouchInput { swipeUp() }
        composeTestRule.onNodeWithTag(SCRUBBER_TEST_TAG).assertIsDisplayed()

        // 5. Stop the list scroll
        composeTestRule.waitForIdle()

        // 6. Advance past the *original* timeout. It should still be visible.
        composeTestRule.mainClock.advanceTimeBy(SCRUBBER_VISIBILITY_TIMEOUT_MILLIS / 2 + 1)
        composeTestRule.onNodeWithTag(SCRUBBER_TEST_TAG).assertIsDisplayed()

        // 7. Advance past a *full new* timeout to confirm it finally disappears.
        composeTestRule.mainClock.advanceTimeBy(SCRUBBER_VISIBILITY_TIMEOUT_MILLIS)
        composeTestRule.onNodeWithTag(SCRUBBER_TEST_TAG).assertIsNotDisplayed()
    }

    /**
     * Helper composable to set up a LazyColumn with an AnimatedScrubber for testing scroll
     * behavior.
     */
    @Composable
    private fun TestLayout(
        listState: LazyListState,
        scrubberState: ScrubberState = ScrubberState(0f) {},
    ) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.testTag(LAZY_COLUMN_TEST_TAG)) {
                items(100) { index -> Text("Item $index") }
            }
            AnimatedScrubber(
                scrubberState = scrubberState,
                listState = listState,
                modifier = Modifier.testTag(SCRUBBER_TEST_TAG),
                label = { Text("A") },
            )
        }
    }

    private fun assertScrubberVisibilityAndTimeout(
        startAction: () -> Unit,
        stopAction: () -> Unit,
    ) {
        // 1. Scrubber is not visible initially
        composeTestRule.onNodeWithTag(SCRUBBER_TEST_TAG).assertIsNotDisplayed()
        // 2. Trigger visibility
        startAction()
        // 3. Scrubber becomes visible
        composeTestRule.onNodeWithTag(SCRUBBER_TEST_TAG).assertIsDisplayed()
        // 4. Stop the trigger, which starts the auto-hide timer
        stopAction()
        // 5. Scrubber remains visible immediately after the trigger stops
        composeTestRule.onNodeWithTag(SCRUBBER_TEST_TAG).assertIsDisplayed()
        // 6. Advance clock part-way and assert it's still visible
        composeTestRule.mainClock.advanceTimeBy(SCRUBBER_VISIBILITY_TIMEOUT_MILLIS / 2)
        // 7. Scrubber is still visible
        composeTestRule.onNodeWithTag(SCRUBBER_TEST_TAG).assertIsDisplayed()
        // 8. Advance the clock past the full timeout duration
        composeTestRule.mainClock.advanceTimeBy(SCRUBBER_VISIBILITY_TIMEOUT_MILLIS)
        // 9. Scrubber is no longer visible
        composeTestRule.onNodeWithTag(SCRUBBER_TEST_TAG).assertIsNotDisplayed()
    }
}

private const val SCRUBBER_TEST_TAG = "scrubber"
private const val LAZY_COLUMN_TEST_TAG = "list"
