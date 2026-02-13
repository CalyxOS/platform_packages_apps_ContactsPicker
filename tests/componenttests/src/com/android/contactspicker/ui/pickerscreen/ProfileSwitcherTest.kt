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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.contactspicker.R
import com.android.contactspicker.data.model.PickerUserState
import com.android.contactspicker.data.model.SwitchableProfileInfo
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.model.UserType
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ProfileSwitcherTest {

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val switcherContentDescription =
        context.getString(R.string.profile_switcher_content_description)

    private val personalProfile =
        createUserProfile(id = 0, type = UserType.PERSONAL, label = "Personal")
    private val workProfile = createUserProfile(id = 10, type = UserType.WORK, label = "Work")

    @Test
    fun profileSwitcher_whenSingleUser_isHidden() {
        val singleUserState = createPickerUserState(listOf(personalProfile))

        composeTestRule.setContent {
            ProfileSwitcher(userState = singleUserState, onProfileClicked = {})
        }

        composeTestRule
            .onNodeWithContentDescription(switcherContentDescription)
            .assertDoesNotExist()
    }

    @Test
    fun profileSwitcher_whenUserStateLoading_isHidden() {
        val loadingUserState = PickerUserState.Loading

        composeTestRule.setContent {
            ProfileSwitcher(userState = loadingUserState, onProfileClicked = {})
        }

        composeTestRule
            .onNodeWithContentDescription(switcherContentDescription)
            .assertDoesNotExist()
    }

    @Test
    fun profileSwitcher_whenMultipleUsers_isDisplayed() {
        val multiUserState = createPickerUserState(listOf(personalProfile, workProfile))

        composeTestRule.setContent {
            ProfileSwitcher(userState = multiUserState, onProfileClicked = {})
        }

        composeTestRule.onNodeWithContentDescription(switcherContentDescription).assertIsDisplayed()
    }

    @Test
    fun profileSwitcher_whenClicked_showsDropdownWithAllProfiles() {
        val multiUserState = createPickerUserState(listOf(personalProfile, workProfile))
        composeTestRule.setContent {
            ProfileSwitcher(userState = multiUserState, onProfileClicked = {})
        }

        composeTestRule.onNodeWithContentDescription(switcherContentDescription).performClick()

        composeTestRule.onNodeWithText("Personal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
    }

    @Test
    fun profileSwitcher_whenProfileSelected_invokesCallback() {
        var clickedProfile: UserProfile? = null
        val multiUserState = createPickerUserState(listOf(personalProfile, workProfile))

        composeTestRule.setContent {
            ProfileSwitcher(userState = multiUserState, onProfileClicked = { clickedProfile = it })
        }

        composeTestRule.onNodeWithContentDescription(switcherContentDescription).performClick()
        composeTestRule.onNodeWithText("Work").performClick()

        assertThat(clickedProfile).isEqualTo(workProfile)
    }

    private fun createUserProfile(id: Int, type: UserType, label: String): UserProfile {
        return UserProfile(
            userId = id,
            userIdToQueryContacts = id,
            userType = type,
            switchableInfo = SwitchableProfileInfo(label = label, icon = null),
            pausedInfo = null,
        )
    }

    private fun createPickerUserState(profiles: List<UserProfile>): PickerUserState {
        return PickerUserState.Success(
            userIdToAvailableUsersMap = profiles.associateBy { it.userId },
            selectedUserId = profiles.firstOrNull()?.userId ?: 0,
        )
    }
}
