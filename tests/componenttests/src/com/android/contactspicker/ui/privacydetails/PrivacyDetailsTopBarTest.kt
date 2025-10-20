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
package com.android.contactspicker.ui.privacydetails

import android.content.Context
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.R
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class PrivacyDetailsTopBarTest {
    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun topBar_displaysHeaderText() {
        composeTestRule.setContent { PrivacyDetailsTopBar(onBackPressed = {}) }

        composeTestRule
            .onNodeWithText(context.getString(R.string.privacy_details_top_bar_header))
            .assertIsDisplayed()
    }

    @Test
    fun topBar_backButton_isDisplayedAndClickable() {
        var backPressed = false
        composeTestRule.setContent { PrivacyDetailsTopBar(onBackPressed = { backPressed = true }) }

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.privacy_details_top_bar_back_button_content_description)
            )
            .assertIsDisplayed()
            .performClick()

        assertThat(backPressed).isTrue()
    }
}
