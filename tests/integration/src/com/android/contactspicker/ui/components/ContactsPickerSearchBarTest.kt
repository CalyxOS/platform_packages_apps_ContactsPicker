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

import android.app.ApplicationPackageManager
import android.content.Context
import android.content.Intent
import android.content.flags.Flags
import android.content.pm.ApplicationInfo
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.ContactsContract
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.contactspicker.ContactsPickerActivity
import com.android.contactspicker.R
import com.android.contactspicker.inject.ActivityModule
import com.android.contactspicker.inject.AppModule
import com.android.contactspicker.provider.CallingPackageProvider
import com.android.contactspicker.room.dao.PrivacyBannerShownDao
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@UninstallModules(AppModule::class, ActivityModule::class)
@HiltAndroidTest
class ContactsPickerSearchBarTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1)
    val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule(order = 2) val composeTestRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var scenario: ActivityScenario<ContactsPickerActivity>

    @BindValue @JvmField val mockPackageManager: ApplicationPackageManager = mock()

    @BindValue @JvmField val mockCallingPackageProvider: CallingPackageProvider = mock()
    @BindValue val mockPrivacyBannerShownDao: PrivacyBannerShownDao = mock()

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.targetContext.packageName
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            "android.permission.READ_CONTACTS",
        )

        val clientAppPackageName = context.packageName
        val appInfo = ApplicationInfo().apply { targetSdkVersion = 37 }
        whenever(mockPackageManager.getApplicationInfo(clientAppPackageName, 0)).doReturn(appInfo)
        whenever(mockPackageManager.getApplicationLabel(appInfo)).doReturn("Test App")
        whenever(mockCallingPackageProvider.get()).doReturn(clientAppPackageName)
        val intent =
            Intent(context, ContactsPickerActivity::class.java).apply {
                action = Intent.ACTION_PICK
                type = ContactsContract.Contacts.CONTENT_TYPE
            }
        scenario = ActivityScenario.launch<ContactsPickerActivity>(intent)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun whenSearchBoxCollapsed() {
        val placeholderText =
            context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
        val clearTextContentDescription =
            context.getString(
                R.string.contacts_picker_top_bar_search_clear_text_content_description
            )

        composeTestRule.onNodeWithContentDescription(placeholderText).assertExists()
        composeTestRule
            .onNodeWithContentDescription(clearTextContentDescription)
            .assertDoesNotExist()
    }

    @Test
    fun whenSearchBoxTapped_searchBoxIsExpanded() {
        val placeholderText =
            context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
        val expandedPlaceholderText =
            context.getString(R.string.contacts_picker_search_expanded_hint)
        val clearTextContentDescription =
            context.getString(
                R.string.contacts_picker_top_bar_search_clear_text_content_description
            )

        composeTestRule.onNodeWithText(placeholderText).performClick()

        val testQuery = "Test Query"
        composeTestRule.onNodeWithText(expandedPlaceholderText).performTextInput(testQuery)

        composeTestRule.onNodeWithContentDescription(clearTextContentDescription).assertExists()
    }

    @Test
    fun whenInputEntered_queryIsDisplayed() {
        val placeholderText =
            context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
        val expandedPlaceholderText =
            context.getString(R.string.contacts_picker_search_expanded_hint)
        composeTestRule.onNodeWithText(placeholderText).performClick()

        val testQuery = "Test Query"
        composeTestRule.onNodeWithText(expandedPlaceholderText).performTextInput(testQuery)

        composeTestRule.onNodeWithText(testQuery).assertExists()
    }

    @Test
    fun whenClearButtonIsTapped_queryIsCleared() {
        val placeholderText =
            context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
        val expandedPlaceholderText =
            context.getString(R.string.contacts_picker_search_expanded_hint)
        val clearTextContentDescription =
            context.getString(
                R.string.contacts_picker_top_bar_search_clear_text_content_description
            )
        composeTestRule.onNodeWithText(placeholderText).performClick()
        val testQuery = "Test Query"
        composeTestRule.onNodeWithText(expandedPlaceholderText).performTextInput(testQuery)
        composeTestRule.onNodeWithText(testQuery).assertExists()

        composeTestRule.onNodeWithContentDescription(clearTextContentDescription).performClick()

        composeTestRule.onNodeWithText(testQuery).assertDoesNotExist()
        composeTestRule.onNodeWithText(expandedPlaceholderText).assertExists()
    }

    @Test
    fun whenSearchBoxExpanded_queryEmpty_clearButtonDoesNotExist() {
        val placeholderText =
            context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
        val clearTextContentDescription =
            context.getString(
                R.string.contacts_picker_top_bar_search_clear_text_content_description
            )
        composeTestRule.onNodeWithText(placeholderText).performClick()

        composeTestRule
            .onNodeWithContentDescription(clearTextContentDescription)
            .assertDoesNotExist()

        val expandedPlaceholderText =
            context.getString(R.string.contacts_picker_search_expanded_hint)
        composeTestRule.onNodeWithText(expandedPlaceholderText).performTextInput("A")
        composeTestRule.onNodeWithContentDescription(clearTextContentDescription).assertExists()
    }
}
