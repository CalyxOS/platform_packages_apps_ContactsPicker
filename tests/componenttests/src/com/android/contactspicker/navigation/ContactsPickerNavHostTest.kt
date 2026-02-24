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
package com.android.contactspicker.navigation

import android.content.Context
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.R
import com.android.contactspicker.data.model.PickerUserState
import com.android.contactspicker.data.model.emptyContactsSelection
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.android.contactspicker.ui.pickerscreen.CONTACTS_PICKER_SCREEN_TEST_TAG
import com.android.contactspicker.ui.privacydetails.PRIVACY_DETAILS_SCREEN_BODY_TEST_TAG
import com.android.contactspicker.ui.theme.ContactsPickerAppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsPickerNavHostTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private lateinit var navController: TestNavHostController
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        navController = TestNavHostController(context)
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    @Test
    fun startDestinationIsContactsPickerScreen() {
        setupNavHostInitialState()
        composeTestRule.onNodeWithTag(CONTACTS_PICKER_SCREEN_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun fromContactsPickerScreen_clickingMoreDetails_navigatesToPrivacyScreen() {
        setupNavHostInitialState(
            mutableStateOf(
                ContactsListState.Success(
                    availableContacts = listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT),
                    selectedContacts = emptyContactsSelection(),
                    isMultiSelectEnabled = false,
                    callingAppName = null,
                    showPrivacyBanner = true,
                    requestedMimeTypes = emptyList(),
                )
            )
        )
        val moreDetailsButton =
            composeTestRule.onNodeWithText(context.getString(R.string.privacy_banner_more_details))
        moreDetailsButton.assertIsDisplayed()
        moreDetailsButton.performClick()
        composeTestRule.onNodeWithTag(PRIVACY_DETAILS_SCREEN_BODY_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun fromPrivacyScreen_clickingBack_navigatesToContactsPickerScreen() {
        setupNavHostInitialState(
            mutableStateOf(
                ContactsListState.Success(
                    availableContacts = listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT),
                    selectedContacts = emptyContactsSelection(),
                    isMultiSelectEnabled = false,
                    callingAppName = null,
                    showPrivacyBanner = true,
                    requestedMimeTypes = emptyList(),
                )
            )
        )
        // Navigate to the privacy details screen
        composeTestRule
            .onNodeWithText(context.getString(R.string.privacy_banner_more_details))
            .performClick()

        composeTestRule.onNodeWithTag(PRIVACY_DETAILS_SCREEN_BODY_TEST_TAG).assertIsDisplayed()

        // Find and click the back button on the privacy details screen
        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.title_top_bar_back_button_content_description)
            )
            .performClick()
        composeTestRule.onNodeWithTag(CONTACTS_PICKER_SCREEN_TEST_TAG).assertIsDisplayed()
    }

    private fun setupNavHostInitialState(
        initialUiState: State<ContactsUiState> = mutableStateOf(ContactsListState.Loading)
    ) {
        composeTestRule.setContent {
            ContactsPickerAppTheme {
                ContactsPickerNavHost(
                    navController = navController,
                    uiState = initialUiState,
                    userState = PickerUserState.Loading,
                    onExpandRequest = {},
                    onToggleContactSelection = {},
                    onToggleEntrySelection = { _, _ -> },
                    onQueryChange = {},
                    onExitSearch = {},
                    onBackFromPreview = {},
                    onPrivacyBannerDismissRequest = {},
                    onProfileClicked = {},
                    onDismissProfileBlockedDialog = {},
                )
            }
        }
    }
}
