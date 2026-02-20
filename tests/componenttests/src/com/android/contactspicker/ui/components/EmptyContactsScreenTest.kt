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

import android.content.Context
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class EmptyContactsScreenTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun emptyContactsScreen_showsEmptyScreenElements() {
        composeTestRule.setContent {
            EmptyContactsScreen(
                title = stringResource(id = R.string.no_contacts_title),
                description = stringResource(id = R.string.no_contacts_description),
                icon = Icons.Outlined.Group,
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.no_contacts_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.no_contacts_description))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(EMPTY_SCREEN_DESCRIPTION_TEXT_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun emptyContactsScreen_whenDescriptionIsNull_doesNotShowSecondaryText() {
        composeTestRule.setContent {
            EmptyContactsScreen(
                title = stringResource(id = R.string.no_contacts_title),
                description = null,
                icon = Icons.Outlined.Group,
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.no_contacts_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(EMPTY_SCREEN_DESCRIPTION_TEXT_TEST_TAG).assertIsNotDisplayed()
    }
}
