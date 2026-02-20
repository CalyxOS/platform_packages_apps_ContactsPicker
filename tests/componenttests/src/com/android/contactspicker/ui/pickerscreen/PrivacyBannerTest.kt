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
package com.android.contactspicker.ui.pickerscreen

import android.content.Context
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.R
import com.android.contactspicker.ui.components.PRIVACY_SHIELD_ICON_TEST_TAG
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class PrivacyBannerTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val moreDetailsText = context.getString(R.string.privacy_banner_more_details)
    private val dismissText = context.getString(R.string.privacy_banner_dismiss)
    private val testAppName = "Test App"

    @Test
    fun privacyBanner_displaysAllElements() {
        composeTestRule.setContent {
            PrivacyBanner(callingAppName = testAppName, onMoreDetails = {}, onDismissRequest = {})
        }

        val bannerDescription = context.getString(R.string.privacy_banner_description, testAppName)
        composeTestRule.onNodeWithText(bannerDescription).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PRIVACY_SHIELD_ICON_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(moreDetailsText).assertIsDisplayed()
        composeTestRule.onNodeWithText(dismissText).assertIsDisplayed()
    }

    @Test
    fun privacyBanner_clickActions_invokeCallbacks() {
        // Set up flags to track if the callbacks are invoked.
        var moreDetailsClicked = false
        var dismissRequestFired = false

        composeTestRule.setContent {
            PrivacyBanner(
                callingAppName = null,
                onMoreDetails = { moreDetailsClicked = true },
                onDismissRequest = { dismissRequestFired = true },
            )
        }

        composeTestRule.onNodeWithText(moreDetailsText).performClick()
        composeTestRule.onNodeWithText(dismissText).performClick()

        assertThat(moreDetailsClicked).isTrue()
        assertThat(dismissRequestFired).isTrue()
    }

    @Test
    fun privacyBanner_nullAppName_displaysFallbackAppName() {
        val expectedDescription =
            context.getString(
                R.string.privacy_banner_description,
                context.getString(R.string.default_calling_app_name),
            )
        composeTestRule.setContent {
            PrivacyBanner(callingAppName = null, onMoreDetails = {}, onDismissRequest = {})
        }
        composeTestRule.onNodeWithText(expectedDescription).assertIsDisplayed()
    }

    @Test
    fun privacyBanner_withAppName_displaysAppNameInDescription() {
        val expectedDescription =
            context.getString(R.string.privacy_banner_description, testAppName)
        composeTestRule.setContent {
            PrivacyBanner(callingAppName = testAppName, onMoreDetails = {}, onDismissRequest = {})
        }
        composeTestRule.onNodeWithText(expectedDescription).assertIsDisplayed()
    }
}
