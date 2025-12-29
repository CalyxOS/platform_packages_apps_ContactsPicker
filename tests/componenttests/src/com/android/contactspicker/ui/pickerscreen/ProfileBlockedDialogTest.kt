/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.contactspicker.ui.pickerscreen

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.contactspicker.data.model.ProfileBlockedDialogData
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ProfileBlockedDialogTest {

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun dialog_rendersTitleAndMessageCorrectly() {
        val expectedTitle = "Work Profile Paused"
        val expectedMessage = "You cannot access contacts right now."
        val dialogData = ProfileBlockedDialogData(title = expectedTitle, message = expectedMessage)

        composeTestRule.setContent {
            ProfileBlockedDialog(data = dialogData, onDismissRequest = {})
        }

        composeTestRule.onNodeWithText(expectedTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedMessage).assertIsDisplayed()
    }

    @Test
    fun dialog_whenConfirmClicked_callsOnDismissRequest() {
        var isDismissed = false
        val dialogData = ProfileBlockedDialogData(title = "Title", message = "Message")

        composeTestRule.setContent {
            ProfileBlockedDialog(data = dialogData, onDismissRequest = { isDismissed = true })
        }

        composeTestRule.onNodeWithText(context.getString(android.R.string.ok)).performClick()

        assertThat(isDismissed).isTrue()
    }
}
