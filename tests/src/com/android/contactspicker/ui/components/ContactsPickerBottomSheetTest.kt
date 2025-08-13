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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ContactsPickerActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsPickerBottomSheetTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ContactsPickerActivity>()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun sheetContent_isDisplayed() {
        composeTestRule.onNodeWithText("Contacts Picker").assertExists().assertIsDisplayed()
    }

    @Test
    fun initialState_peekHeightIsCorrect() {
        val sheetBounds =
            composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).getUnclippedBoundsInRoot()
        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val tolerance = 0.1f // 1% tolerance

        val visibleHeight = rootBounds.height - sheetBounds.top
        val bottomSheetHeightRatio = visibleHeight.value / rootBounds.height.value

        assertThat(bottomSheetHeightRatio).isWithin(tolerance).of(BOTTOM_SHEET_PEEK_HEIGHT_RATIO)
    }

    @Test
    fun whenSheetIsSwipedUp_itExpands() {
        val sheetNode = composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG)
        val initialBounds = sheetNode.getUnclippedBoundsInRoot()

        sheetNode.performTouchInput { swipeUp() }

        composeTestRule.waitForIdle()
        val expandedBounds = sheetNode.getUnclippedBoundsInRoot()
        sheetNode.assertIsDisplayed()
        assert(expandedBounds.top < initialBounds.top)
    }

    @Test
    fun whenSheetIsSwipedDown_contentIsHidden() {
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).performTouchInput { swipeDown() }

        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun whenActivityIsRecreated_bottomSheetIsStillVisible() {
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).assertIsDisplayed()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).assertIsDisplayed()
    }
}
