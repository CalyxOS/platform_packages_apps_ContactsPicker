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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ContactsPreviewState
import com.android.contactspicker.R
import com.android.contactspicker.data.model.buildContactsSelection
import com.android.contactspicker.data.model.emptyContactsSelection
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.android.contactspicker.ui.theme.ContactsPickerAppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class PreviewScreenTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun previewScreen_displaysTopBarWithPrivacyDetailsHeader() {
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                PreviewScreen(
                    onBackPressed = {},
                    uiState =
                        ContactsPreviewState(
                            emptyList(),
                            emptyContactsSelection(),
                            isMultiSelectEnabled = false,
                        ),
                    onToggleContactSelection = {},
                    onToggleEntrySelection = { _, _ -> },
                )
            }
        }

        composeTestRule.onNodeWithTag(PREVIEW_SCREEN_TEST_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.preview_screen_title))
            .assertIsDisplayed()
    }

    @Test
    fun previewScreen_onBackPressed_invokesCallback() {
        var onBackPressed = false
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                PreviewScreen(
                    onBackPressed = { onBackPressed = true },
                    uiState = ContactsPreviewState(emptyList(), emptyContactsSelection(), false),
                    onToggleContactSelection = {},
                    onToggleEntrySelection = { _, _ -> },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.title_top_bar_back_button_content_description)
            )
            .performClick()

        assertThat(onBackPressed).isTrue()
    }

    @Test
    fun previewScreen_onBackGesture_invokesCallback() {
        var onBackPressed = false
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                PreviewScreen(
                    onBackPressed = { onBackPressed = true },
                    uiState = ContactsPreviewState(emptyList(), emptyContactsSelection(), false),
                    onToggleContactSelection = {},
                    onToggleEntrySelection = { _, _ -> },
                )
            }
        }

        Espresso.pressBack()

        assertThat(onBackPressed).isTrue()
    }

    @Test
    fun previewScreen_withSelectedContacts_displaysAllSelectedContacts() {
        val selectedContact1 = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        val selectedContact2 = ContactTestDataFactory.GENERIC_PHONE_CONTACT
        val selectedMap = buildContactsSelection {
            put(selectedContact1.id, setOf(selectedContact1.id))
            put(selectedContact2.id, setOf(selectedContact2.phones.first().id))
        }

        val uiState =
            ContactsPreviewState(listOf(selectedContact1, selectedContact2), selectedMap, false)

        composeTestRule.setContent {
            ContactsPickerAppTheme {
                PreviewScreen(
                    onBackPressed = {},
                    uiState = uiState,
                    onToggleContactSelection = {},
                    onToggleEntrySelection = { _, _ -> },
                )
            }
        }

        // All selected contacts should be displayed
        composeTestRule.onNodeWithText(selectedContact1.displayName).assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedContact2.displayName).assertIsDisplayed()

        // Check if the selected entry is shown for PhoneContact
        composeTestRule.onNodeWithText(selectedContact2.phones.first().number).assertIsDisplayed()
    }

    @Test
    fun previewScreen_hasContentDescription() {
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                PreviewScreen(
                    onBackPressed = {},
                    uiState =
                        ContactsPreviewState(
                            emptyList(),
                            emptyContactsSelection(),
                            isMultiSelectEnabled = false,
                        ),
                    onToggleContactSelection = {},
                    onToggleEntrySelection = { _, _ -> },
                )
            }
        }

        val screenContentDesc = context.getString(R.string.preview_screen_content_description)
        composeTestRule.onNodeWithContentDescription(screenContentDesc).assertIsDisplayed()
    }
}
