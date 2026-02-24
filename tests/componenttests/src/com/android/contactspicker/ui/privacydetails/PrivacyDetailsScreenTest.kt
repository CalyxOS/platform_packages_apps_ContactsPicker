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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.R
import com.android.contactspicker.ui.theme.ContactsPickerAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class PrivacyDetailsScreenTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun privacyDetailsScreen_displaysTopBarAndBody() {
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                PrivacyDetailsScreen(
                    onBackPressed = {},
                    uiState = mutableStateOf(ContactsListState.Loading),
                )
            }
        }

        composeTestRule.onNodeWithTag(PRIVACY_DETAILS_SCREEN_TOP_BAR_TEST_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.privacy_details_top_bar_header))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(PRIVACY_DETAILS_SCREEN_BODY_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun privacyDetailsScreen_invokesOnBackPressedCallback() {
        val mockOnBackPressed: () -> Unit = mock()

        composeTestRule.setContent {
            PrivacyDetailsScreen(
                onBackPressed = mockOnBackPressed,
                uiState = mutableStateOf(ContactsListState.Loading),
            )
        }

        val backButtonContentDescription =
            context.getString(R.string.title_top_bar_back_button_content_description)
        composeTestRule.onNodeWithContentDescription(backButtonContentDescription).performClick()

        verify(mockOnBackPressed, times(1)).invoke()
    }
}
