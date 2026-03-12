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

package com.android.contactspicker.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PickerUserStateTest {

    @Test
    fun shouldDisableRecentsScreenshot_loadingState_returnsFalse() {
        val state = PickerUserState.Loading
        assertThat(state.shouldDisableRecentsScreenshot()).isFalse()
    }

    @Test
    fun shouldDisableRecentsScreenshot_noPrivateProfile_returnsFalse() {
        val personalProfile =
            UserProfile(
                userId = 0,
                userIdToQueryContacts = 0,
                userType = UserType.PERSONAL,
                switchableInfo = SwitchableProfileInfo("Personal", null),
            )
        val state =
            PickerUserState.Success(
                userIdToAvailableUsersMap = mapOf(0 to personalProfile),
                selectedUserId = 0,
            )
        assertThat(state.shouldDisableRecentsScreenshot()).isFalse()
    }

    @Test
    fun shouldDisableRecentsScreenshot_unlockedPrivateProfile_returnsTrue() {
        val privateProfile =
            UserProfile(
                userId = 12,
                userIdToQueryContacts = 12,
                userType = UserType.PRIVATE,
                switchableInfo = SwitchableProfileInfo("Private", null),
            )
        val state =
            PickerUserState.Success(
                userIdToAvailableUsersMap = mapOf(12 to privateProfile),
                selectedUserId = 0,
            )
        assertThat(state.shouldDisableRecentsScreenshot()).isTrue()
    }

    @Test
    fun shouldDisableRecentsScreenshot_lockedPrivateProfile_returnsFalse() {
        val privateProfile =
            UserProfile(
                userId = 12,
                userIdToQueryContacts = 12,
                userType = UserType.PRIVATE,
                switchableInfo = null, // Locked/Quiet mode
            )
        val state =
            PickerUserState.Success(
                userIdToAvailableUsersMap = mapOf(12 to privateProfile),
                selectedUserId = 0,
            )
        assertThat(state.shouldDisableRecentsScreenshot()).isFalse()
    }

    @Test
    fun shouldDisableRecentsScreenshot_multipleProfiles_oneUnlockedPrivate_returnsTrue() {
        val personalProfile =
            UserProfile(
                userId = 0,
                userIdToQueryContacts = 0,
                userType = UserType.PERSONAL,
                switchableInfo = SwitchableProfileInfo("Personal", null),
            )
        val privateProfile =
            UserProfile(
                userId = 12,
                userIdToQueryContacts = 12,
                userType = UserType.PRIVATE,
                switchableInfo = SwitchableProfileInfo("Private", null),
            )
        val state =
            PickerUserState.Success(
                userIdToAvailableUsersMap = mapOf(0 to personalProfile, 12 to privateProfile),
                selectedUserId = 0,
            )
        assertThat(state.shouldDisableRecentsScreenshot()).isTrue()
    }
}
