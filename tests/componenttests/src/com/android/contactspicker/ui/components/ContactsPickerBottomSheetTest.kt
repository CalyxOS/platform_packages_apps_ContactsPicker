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
package com.android.contactspicker.ui.components

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalMaterial3Api::class)
@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsPickerBottomSheetTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun initialState_peekHeightIsCorrect() {
        val mockOnDismissRequest: () -> Unit = mock()

        composeTestRule.setContent {
            ContactsPickerBottomSheet(onDismissRequest = mockOnDismissRequest)
        }

        val sheetBounds =
            composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).getUnclippedBoundsInRoot()
        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val tolerance = 0.1f // 1% tolerance

        val visibleHeight = rootBounds.height - sheetBounds.top
        val bottomSheetHeightRatio = visibleHeight.value / rootBounds.height.value

        assertThat(bottomSheetHeightRatio).isWithin(tolerance).of(BOTTOM_SHEET_PEEK_HEIGHT_RATIO)
    }

    @Test
    fun whenSheetIsSwipedUp_expands() {
        val mockOnDismissRequest: () -> Unit = mock()

        composeTestRule.setContent {
            ContactsPickerBottomSheet(onDismissRequest = mockOnDismissRequest)
        }

        val sheetNode = composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG)
        val initialBounds = sheetNode.getUnclippedBoundsInRoot()

        sheetNode.performTouchInput { swipeUp() }

        composeTestRule.waitForIdle()
        val expandedBounds = sheetNode.getUnclippedBoundsInRoot()
        sheetNode.assertIsDisplayed()
        assert(expandedBounds.top < initialBounds.top)
    }

    @Test
    fun whenSheetIsSwipedDown_onDismissIsCalled() {
        val mockOnDismissRequest: () -> Unit = mock()

        composeTestRule.setContent {
            ContactsPickerBottomSheet(onDismissRequest = mockOnDismissRequest)
        }

        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()

        verify(mockOnDismissRequest, times(1)).invoke()
    }
}
