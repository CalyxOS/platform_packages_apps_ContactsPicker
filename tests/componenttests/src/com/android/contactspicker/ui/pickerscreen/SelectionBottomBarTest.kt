/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import androidx.compose.ui.Modifier
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
class SelectionBottomBarTest {

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun selectionBottomBar_displaysCorrectCount() {
        val count = 5
        composeTestRule.setContent {
            SelectionBottomBar(
                selectedCount = count,
                onPreviewClicked = {},
                onDoneClick = {},
                onClearSelection = {},
                isPreviewMode = false,
                onBackFromPreview = {},
                modifier = Modifier,
            )
        }

        composeTestRule.onNodeWithText(count.toString()).assertIsDisplayed()
    }

    @Test
    fun clearSelectionButton_triggersCallback() {
        var clearClicked = false
        composeTestRule.setContent {
            SelectionBottomBar(
                selectedCount = 1,
                onPreviewClicked = {},
                onDoneClick = {},
                onClearSelection = { clearClicked = true },
                isPreviewMode = false,
                onBackFromPreview = {},
                modifier = Modifier,
            )
        }

        val clearContentDesc =
            context.getString(R.string.selection_bottom_bar_clear_button_content_description)
        composeTestRule.onNodeWithContentDescription(clearContentDesc).performClick()

        assertThat(clearClicked).isTrue()
    }

    @Test
    fun previewButton_triggersCallback() {
        var previewClicked = false
        composeTestRule.setContent {
            SelectionBottomBar(
                selectedCount = 1,
                onPreviewClicked = { previewClicked = true },
                onDoneClick = {},
                onClearSelection = {},
                isPreviewMode = false,
                onBackFromPreview = {},
                modifier = Modifier,
            )
        }

        val previewLabel = context.getString(R.string.selection_bottom_bar_preview_button_label)
        composeTestRule.onNodeWithText(previewLabel).performClick()

        assertThat(previewClicked).isTrue()
    }

    @Test
    fun previewButton_hasContentDescription() {
        composeTestRule.setContent {
            SelectionBottomBar(
                selectedCount = 1,
                onPreviewClicked = {},
                onDoneClick = {},
                onClearSelection = {},
                isPreviewMode = false,
                onBackFromPreview = {},
                modifier = Modifier,
            )
        }

        val previewContentDesc =
            context.getString(R.string.selection_bottom_bar_preview_button_content_description)
        composeTestRule.onNodeWithContentDescription(previewContentDesc).assertIsDisplayed()
    }
}
