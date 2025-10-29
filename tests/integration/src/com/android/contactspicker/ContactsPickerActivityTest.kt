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

package com.android.contactspicker

import android.app.Activity
import android.app.ApplicationPackageManager
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.flags.Flags
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.ContactsContract
import androidx.collection.longObjectMapOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.inject.ActivityModule
import com.android.contactspicker.inject.AppModule
import com.android.contactspicker.provider.CallingPackageProvider
import com.android.contactspicker.ui.components.BOTTOM_SHEET_TEST_TAG
import com.android.contactspicker.viewmodel.ContactsViewModel
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
@UninstallModules(AppModule::class, ActivityModule::class)
@HiltAndroidTest
class ContactsPickerActivityTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1)
    val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule(order = 2) val composeTestRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var baseIntent: Intent

    @BindValue @JvmField val mockPackageManager: ApplicationPackageManager = mock()

    @BindValue @JvmField val mockCallingPackageProvider: CallingPackageProvider = mock()

    @BindValue val mockViewModel: ContactsViewModel = mock()

    private lateinit var testPackageName: String

    private val testUri = Uri.parse("content://contacts/1")
    private val testUri2 = Uri.parse("content://data/10")
    private val testContact =
        DisplayNameContact(id = 1, displayName = "Test", lookupKey = "test_lookup")

    @Before
    fun setUp() {
        Intents.init()
        hiltRule.inject()

        testPackageName = context.packageName
        val appInfo = ApplicationInfo().apply { targetSdkVersion = 37 }
        whenever(mockPackageManager.getApplicationInfo(testPackageName, 0)).doReturn(appInfo)
        whenever(mockCallingPackageProvider.get()).doReturn(testPackageName)
        baseIntent =
            Intent(context, ContactsPickerActivity::class.java).apply {
                action = Intent.ACTION_PICK
                type = ContactsContract.Contacts.CONTENT_TYPE
            }
        val successState =
            MutableStateFlow<ContactsUiState>(
                ContactsUiState.Success(emptyList(), longObjectMapOf(), false)
            )
        whenever(mockViewModel.uiState).thenReturn(successState)
        doNothing().whenever(mockViewModel).processIntent(anyOrNull(), anyOrNull(), anyOrNull())
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
    fun intent_contactPickerFlagDisabled_throws() {
        assertThrows(RuntimeException::class.java) {
            ActivityScenario.launch<ContactsPickerActivity>(baseIntent)
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
    fun topBarSearchText_isDisplayed() {
        ActivityScenario.launch<ContactsPickerActivity>(baseIntent)
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
            )
            .assertIsDisplayed()
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
    fun whenSwipedDown_activityFinishes() {
        val scenario = ActivityScenario.launch<ContactsPickerActivity>(baseIntent)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        scenario.onActivity { activity ->
            if (activity != null) {
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
    fun whenActivityIsRecreated_bottomSheetIsStillVisible() {
        val scenario = ActivityScenario.launch<ContactsPickerActivity>(baseIntent)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).assertIsDisplayed()

        scenario.recreate()

        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TEST_TAG).assertIsDisplayed()
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
    fun highTargetSdk_handlesInternally() {
        // target SDK of calling app set to 37 in setUp
        val scenario = ActivityScenario.launch<ContactsPickerActivity>(baseIntent)

        scenario.onActivity { activity -> assertThat(activity.isFinishing).isFalse() }
        assertThat(Intents.getIntents().filter { it.action == Intent.ACTION_CHOOSER }).isEmpty()
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
    fun lowTargetSdk_forwardsToChooser() {
        val appInfo = ApplicationInfo().apply { targetSdkVersion = 36 }
        whenever(mockPackageManager.getApplicationInfo(testPackageName, 0)).doReturn(appInfo)
        whenever(mockPackageManager.getPreferredActivities(any(), any(), any())).thenAnswer { 0 }

        val scenario = ActivityScenario.launch<ContactsPickerActivity>(baseIntent)

        Intents.intended(hasAction(Intent.ACTION_CHOOSER))
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
    fun packageManagerThrowsException_handlesInternally() {
        whenever(mockPackageManager.getApplicationInfo(anyString(), anyInt()))
            .thenThrow(PackageManager.NameNotFoundException())

        val scenario = ActivityScenario.launch<ContactsPickerActivity>(baseIntent)

        composeTestRule
            .onNodeWithText(
                context.getString(R.string.contacts_picker_top_bar_search_placeholder_hint)
            )
            .assertIsDisplayed()

        scenario.onActivity { activity -> assertThat(activity.isFinishing).isFalse() }
        assertThat(Intents.getIntents().filter { it.action == Intent.ACTION_CHOOSER }).isEmpty()
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
    fun nullCallingPackage_finishesWithResultCanceled() {
        whenever(mockCallingPackageProvider.get()).doReturn(null)

        val scenario = ActivityScenario.launchActivityForResult<ContactsPickerActivity>(baseIntent)

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun handleDoneClicked_withSingleSelection_setsResultOkAndFinishes() = runTest {
        val successStateSingleSelect =
            MutableStateFlow<ContactsUiState>(
                ContactsUiState.Success(
                    availableContacts = listOf(testContact),
                    selectedContacts = longObjectMapOf(testContact.id, setOf(testContact.id)),
                    isMultiSelectEnabled = false,
                )
            )
        whenever(mockViewModel.uiState).thenReturn(successStateSingleSelect)
        whenever(mockViewModel.prepareSelectionResult()).thenReturn(listOf(testUri))

        val scenario = ActivityScenario.launchActivityForResult<ContactsPickerActivity>(baseIntent)

        composeTestRule.onNodeWithText("Done").performClick()

        composeTestRule.awaitIdle()

        val result = scenario.result
        assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)

        val resultIntent = result.resultData
        assertThat(resultIntent).isNotNull()
        assertThat(resultIntent.data).isEqualTo(testUri)
        assertThat(resultIntent.clipData).isNull()
        assertThat(resultIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isEqualTo(1)

        scenario.onActivity { activity ->
            if (activity != null) {
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun handleDoneClicked_withMultiSelection_setsResultOkWithClipData() {
        val successStateMultiSelect =
            MutableStateFlow<ContactsUiState>(
                ContactsUiState.Success(
                    availableContacts = listOf(testContact),
                    selectedContacts = longObjectMapOf(testContact.id, setOf(testContact.id)),
                    isMultiSelectEnabled = true,
                )
            )
        whenever(mockViewModel.uiState).thenReturn(successStateMultiSelect)
        // Set up ViewModel to return multiple URIs
        whenever(mockViewModel.prepareSelectionResult()).thenReturn(listOf(testUri, testUri2))

        val scenario = ActivityScenario.launchActivityForResult<ContactsPickerActivity>(baseIntent)

        // ACT
        composeTestRule.onNodeWithText("Done").performClick()

        // ASSERT
        val result = scenario.result
        assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)

        val resultIntent = result.resultData as Intent

        assertThat(resultIntent).isNotNull()
        assertThat(resultIntent.data).isNull()
        assertThat(resultIntent.clipData).isNotNull()
        assertThat(resultIntent.clipData!!.itemCount).isEqualTo(2)
        assertThat(resultIntent.clipData!!.getItemAt(0).uri).isEqualTo(testUri)
        assertThat(resultIntent.clipData!!.getItemAt(1).uri).isEqualTo(testUri2)
        assertThat(resultIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isEqualTo(1)
        scenario.onActivity { activity ->
            if (activity != null) {
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun handleDoneClicked_withNoSelection_setsResultCanceled() {
        val successStateSingleSelect =
            MutableStateFlow<ContactsUiState>(
                ContactsUiState.Success(
                    availableContacts = listOf(testContact),
                    selectedContacts = longObjectMapOf(testContact.id, setOf(testContact.id)),
                    isMultiSelectEnabled = false,
                )
            )
        whenever(mockViewModel.uiState).thenReturn(successStateSingleSelect)
        whenever(mockViewModel.prepareSelectionResult()).thenReturn(emptyList())

        val scenario = ActivityScenario.launchActivityForResult<ContactsPickerActivity>(baseIntent)

        composeTestRule.onNodeWithText("Done").performClick()

        val result = scenario.result
        assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        scenario.onActivity { activity ->
            if (activity != null) {
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
    fun lowTargetSdk_withPreferredActivity_startsPreferredActivity() {
        val appInfo = ApplicationInfo().apply { targetSdkVersion = 36 }
        whenever(mockPackageManager.getApplicationInfo(testPackageName, 0)).doReturn(appInfo)
        val preferredComponent =
            ComponentName("com.preferred.app", "com.preferred.app.PickerActivity")
        Intents.intending(hasComponent(preferredComponent))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
        whenever(mockPackageManager.getPreferredActivities(any(), any(), anyOrNull())).thenAnswer {
            val filters = it.getArgument<MutableList<IntentFilter>>(0)
            val activities = it.getArgument<MutableList<ComponentName>>(1)
            val filter = IntentFilter(Intent.ACTION_PICK)
            filter.addCategory(Intent.CATEGORY_DEFAULT)
            filter.addDataType(ContactsContract.Contacts.CONTENT_TYPE)
            filters.add(filter)
            activities.add(preferredComponent)
            1
        }

        val scenario = ActivityScenario.launch<ContactsPickerActivity>(baseIntent)

        Intents.intended(hasComponent(preferredComponent))
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }
}
