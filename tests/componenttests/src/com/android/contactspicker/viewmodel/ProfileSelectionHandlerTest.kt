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
package com.android.contactspicker.viewmodel

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.data.model.PausedProfileInfo
import com.android.contactspicker.data.model.PausedReason
import com.android.contactspicker.data.model.PickerUserState
import com.android.contactspicker.data.model.SwitchableProfileInfo
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.model.UserType
import com.android.contactspicker.data.repository.UserRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ProfileSelectionHandlerTest {

    companion object {
        private const val TEST_PACKAGE_NAME = "com.test.app"
        private const val PERSONAL_USER_ID = 0
        private const val WORK_USER_ID = 10
        private const val CALLING_USER_ID = 23456
    }

    private val mockUserRepository: UserRepository = mock()
    private lateinit var handler: ProfileSelectionHandler

    private val availableUsersFlow = MutableSharedFlow<Map<Int, UserProfile>>(replay = 1)

    private val resultsChannel = Channel<PickerUserState>(Channel.UNLIMITED)

    @Before
    fun setUp() {
        whenever(mockUserRepository.getAvailableUsersFlow(anyOrNull())) doReturn availableUsersFlow
        handler = ProfileSelectionHandler(mockUserRepository)
    }

    private fun TestScope.startCollecting() {
        backgroundScope.launch {
            handler.getUserStateFlow(TEST_PACKAGE_NAME, CALLING_USER_ID).collect {
                resultsChannel.send(it)
            }
        }
    }

    private suspend fun receiveSuccessState(): PickerUserState.Success {
        val state = resultsChannel.receive()
        assertThat(state).isInstanceOf(PickerUserState.Success::class.java)
        return state as PickerUserState.Success
    }

    private fun getProfile(
        id: Int,
        type: UserType = UserType.PERSONAL,
        isSwitchable: Boolean = true,
        pausedReason: PausedReason? = null,
    ): UserProfile {
        return UserProfile(
            userId = id,
            userIdToQueryContacts = id,
            userType = type,
            switchableInfo = if (isSwitchable) SwitchableProfileInfo("Profile $id", null) else null,
            pausedInfo = pausedReason?.let { PausedProfileInfo(it) },
        )
    }

    @Test
    fun setSelectedUser_updatesSelectedId() = runTest {
        val primaryProfile = getProfile(PERSONAL_USER_ID)
        availableUsersFlow.emit(mapOf(PERSONAL_USER_ID to primaryProfile))

        startCollecting()
        val first = receiveSuccessState()
        assertThat(first.selectedUserId).isEqualTo(PERSONAL_USER_ID)

        val workProfile = getProfile(WORK_USER_ID, UserType.WORK)
        availableUsersFlow.emit(
            mapOf(PERSONAL_USER_ID to primaryProfile, WORK_USER_ID to workProfile)
        )
        receiveSuccessState() // Flush the emission from adding the work profile

        handler.setSelectedUser(WORK_USER_ID)
        val third = receiveSuccessState()
        assertThat(third.selectedUserId).isEqualTo(WORK_USER_ID)

        handler.setSelectedUser(999) // User explicitly selects a missing ID
        val fourth = receiveSuccessState()
        assertThat(fourth.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }

    @Test
    fun clearSelectedUser_resetsToDefault() = runTest {
        val primaryProfile = getProfile(PERSONAL_USER_ID)
        val workProfile = getProfile(WORK_USER_ID, UserType.WORK)
        availableUsersFlow.emit(
            mapOf(PERSONAL_USER_ID to primaryProfile, WORK_USER_ID to workProfile)
        )

        startCollecting()
        receiveSuccessState()

        handler.setSelectedUser(WORK_USER_ID)
        val workProfileId = receiveSuccessState()
        assertThat(workProfileId.selectedUserId).isEqualTo(WORK_USER_ID)

        handler.clearSelectedUser()
        val result = receiveSuccessState()
        assertThat(result.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }

    @Test
    fun getUserState_switchesToDefault_whenSelectedProfileIsPaused() = runTest {
        val primaryProfile = getProfile(PERSONAL_USER_ID)
        val workProfile = getProfile(WORK_USER_ID, UserType.WORK)
        availableUsersFlow.emit(
            mapOf(PERSONAL_USER_ID to primaryProfile, WORK_USER_ID to workProfile)
        )

        startCollecting()
        receiveSuccessState()

        // Select work profile
        handler.setSelectedUser(WORK_USER_ID)
        receiveSuccessState()

        // Make work profile paused
        val pausedWorkProfile =
            getProfile(WORK_USER_ID, UserType.WORK, pausedReason = PausedReason.QUIET_MODE)
        availableUsersFlow.emit(
            mapOf(PERSONAL_USER_ID to primaryProfile, WORK_USER_ID to pausedWorkProfile)
        )

        val result = receiveSuccessState()

        // Should switch back to PERSONAL_USER_ID because WORK_USER_ID is paused
        assertThat(result.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }

    @Test
    fun getUserState_switchesToDefault_whenSelectedProfileIsRemoved() = runTest {
        val primaryProfile = getProfile(PERSONAL_USER_ID)
        val workProfile = getProfile(WORK_USER_ID, UserType.WORK)
        availableUsersFlow.emit(
            mapOf(PERSONAL_USER_ID to primaryProfile, WORK_USER_ID to workProfile)
        )

        startCollecting()
        receiveSuccessState()

        // Select work profile
        handler.setSelectedUser(WORK_USER_ID)
        receiveSuccessState()

        // Remove work profile
        availableUsersFlow.emit(mapOf(PERSONAL_USER_ID to primaryProfile))

        val result = receiveSuccessState()

        // Should switch back to PERSONAL_USER_ID because WORK_USER_ID is removed
        assertThat(result.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }

    @Test
    fun getUserState_switchesToDefault_whenSelectedProfileIsNonSwitchable() = runTest {
        val primaryProfile = getProfile(PERSONAL_USER_ID)
        val switchableWorkProfile = getProfile(WORK_USER_ID, UserType.WORK, isSwitchable = true)
        availableUsersFlow.emit(
            mapOf(PERSONAL_USER_ID to primaryProfile, WORK_USER_ID to switchableWorkProfile)
        )

        startCollecting()
        receiveSuccessState()

        handler.setSelectedUser(WORK_USER_ID)
        val workSelectedState = receiveSuccessState()
        assertThat(workSelectedState.selectedUserId).isEqualTo(WORK_USER_ID)

        // Make the work profile non-switchable
        val nonSwitchableWorkProfile = getProfile(WORK_USER_ID, UserType.WORK, isSwitchable = false)
        availableUsersFlow.emit(
            mapOf(PERSONAL_USER_ID to primaryProfile, WORK_USER_ID to nonSwitchableWorkProfile)
        )

        val result = receiveSuccessState()

        // The selection falls back to the default profile
        assertThat(result.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }

    @Test
    fun getUserStateFlow_irrelevantProfileChange_maintainsSelection() = runTest {
        val cloneUserId = 20
        val primaryProfile = getProfile(PERSONAL_USER_ID)
        val workProfile = getProfile(WORK_USER_ID, UserType.WORK)
        availableUsersFlow.emit(
            mapOf(PERSONAL_USER_ID to primaryProfile, WORK_USER_ID to workProfile)
        )

        startCollecting()
        receiveSuccessState() // Initial emission

        // User explicitly selects the work profile
        handler.setSelectedUser(WORK_USER_ID)
        assertThat(receiveSuccessState().selectedUserId).isEqualTo(WORK_USER_ID)

        // Simulate the OS broadcasting that a new, unrelated Clone profile was just created
        val cloneProfile = getProfile(cloneUserId, UserType.CLONE, isSwitchable = false)
        availableUsersFlow.emit(
            mapOf(
                PERSONAL_USER_ID to primaryProfile,
                WORK_USER_ID to workProfile,
                cloneUserId to cloneProfile,
            )
        )

        val state = receiveSuccessState()

        // the active selection survives the unrelated map update
        assertThat(state.selectedUserId).isEqualTo(WORK_USER_ID)
    }

    @Test
    fun getUserStateFlow_callingUserIsMissing_fallsBackToCurrentProcessUser() = runTest {
        val availableUsers = mapOf(PERSONAL_USER_ID to getProfile(PERSONAL_USER_ID))
        whenever(mockUserRepository.getAvailableUsersFlow(anyOrNull()))
            .thenReturn(flowOf(availableUsers))

        val wrongCallingUserId = 99
        val state =
            handler.getUserStateFlow(TEST_PACKAGE_NAME, wrongCallingUserId).first()
                as PickerUserState.Success

        // gracefully falls back to the safe current process user
        assertThat(state.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }

    @Test
    fun setSelectedUser_toNonSwitchableProfile_ignoresSelectionAndFallsBack() = runTest {
        val primaryProfile = getProfile(PERSONAL_USER_ID)
        val nonSwitchableProfile = getProfile(WORK_USER_ID, UserType.CLONE, isSwitchable = false)
        availableUsersFlow.emit(
            mapOf(PERSONAL_USER_ID to primaryProfile, WORK_USER_ID to nonSwitchableProfile)
        )

        startCollecting()
        receiveSuccessState() // Initial

        // Attempt to select a profile that is not switchable
        handler.setSelectedUser(WORK_USER_ID)
        val state = receiveSuccessState()

        // the handler rejects the manual selection and falls back to primary profile
        assertThat(state.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }
}
