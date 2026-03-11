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

package com.android.contactspicker.data.repository

import android.content.pm.UserInfo
import android.content.pm.UserProperties
import android.os.UserManager
import com.android.contactspicker.data.model.PausedProfileInfo
import com.android.contactspicker.data.model.PausedReason
import com.android.contactspicker.data.model.PickerUserState
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.model.UserType
import com.android.contactspicker.data.repository.utils.ProfileChangesMonitor
import com.android.contactspicker.data.repository.utils.UserProfileFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class UserRepositoryImplTest {

    companion object {
        private const val TEST_PACKAGE_NAME = "com.test"
        private const val PERSONAL_USER_ID = 0
        private const val WORK_USER_ID = 10
        private const val PRIVATE_USER_ID = 12
    }

    private val mockUserManager: UserManager = mock()
    private val mockUserProfileFactory: UserProfileFactory = mock()
    private val mockProfileChangesMonitor: ProfileChangesMonitor = mock()
    private val profileChangesFlow = MutableSharedFlow<Unit>(replay = 1)
    private val callingUserId = 0
    private val resultsChannel = Channel<PickerUserState>(Channel.UNLIMITED)
    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setUp() {
        whenever(mockProfileChangesMonitor.getProfileChangeFlow()) doReturn profileChangesFlow

        userRepository =
            UserRepositoryImpl(mockUserManager, mockUserProfileFactory, mockProfileChangesMonitor)

        // Default UserProperties for all users unless overridden in tests
        val defaultProperties = UserProperties.Builder().build()
        whenever(mockUserManager.getUserProperties(any())).thenReturn(defaultProperties)
    }

    @Test
    fun getUserState_filtersProfilesWhenFactoryReturnsNull() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(PERSONAL_USER_ID)
        val (privateUser, _) = getUserInfoAndProfile(PRIVATE_USER_ID, UserType.PRIVATE)

        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser, privateUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile
        whenever(mockUserProfileFactory.createProfile(privateUser, TEST_PACKAGE_NAME)) doReturn null

        startCollecting()

        val result = receiveSuccessState()

        assertThat(result.userIdToAvailableUsersMap).hasSize(1)
        assertThat(result.userIdToAvailableUsersMap.containsKey(PRIVATE_USER_ID)).isFalse()
        assertThat(result.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }

    @Test
    fun getUserState_returnsInitialState() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(PERSONAL_USER_ID)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile
        startCollecting()

        val result = receiveSuccessState()

        assertThat(result.userIdToAvailableUsersMap).hasSize(1)
        assertThat(result.userIdToAvailableUsersMap[PERSONAL_USER_ID]).isEqualTo(primaryProfile)
        assertThat(result.selectedUserId).isEqualTo(PERSONAL_USER_ID)
        verify(mockUserProfileFactory).clearCache()
    }

    @Test
    fun getUserState_updatesOnProfileChange() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(PERSONAL_USER_ID)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile
        startCollecting()
        receiveSuccessState() // Initial state

        val (workUser, workProfile) = getUserInfoAndProfile(WORK_USER_ID)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser, workUser)
        whenever(mockUserProfileFactory.createProfile(workUser, TEST_PACKAGE_NAME)) doReturn
            workProfile
        profileChangesFlow.emit(Unit)

        val result = receiveSuccessState()

        assertThat(result.userIdToAvailableUsersMap).hasSize(2)
        assertThat(result.userIdToAvailableUsersMap[PERSONAL_USER_ID]).isEqualTo(primaryProfile)
        assertThat(result.userIdToAvailableUsersMap[WORK_USER_ID]).isEqualTo(workProfile)
        assertThat(result.selectedUserId).isEqualTo(PERSONAL_USER_ID)
        verify(mockUserProfileFactory, times(2)).clearCache()
    }

    @Test
    fun setSelectedUser_updatesSelectedId() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(PERSONAL_USER_ID)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser)
        whenever(mockUserProfileFactory.createProfile(any(), any())) doReturn primaryProfile
        startCollecting()

        val first = receiveSuccessState()
        assertThat(first.selectedUserId).isEqualTo(PERSONAL_USER_ID)
        val (workUser, workProfile) = getUserInfoAndProfile(WORK_USER_ID)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser, workUser)
        whenever(mockUserProfileFactory.createProfile(workUser, TEST_PACKAGE_NAME)) doReturn
            workProfile
        profileChangesFlow.emit(Unit)
        receiveSuccessState()

        userRepository.setSelectedUser(WORK_USER_ID)
        val third = receiveSuccessState()

        assertThat(third.selectedUserId).isEqualTo(WORK_USER_ID)

        userRepository.setSelectedUser(999)
        val fourth = receiveSuccessState()

        assertThat(fourth.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }

    @Test
    fun clearSelectedUser_resetsToDefault() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(PERSONAL_USER_ID)
        val (workUser, workProfile) = getUserInfoAndProfile(WORK_USER_ID)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser, workUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile
        whenever(mockUserProfileFactory.createProfile(workUser, TEST_PACKAGE_NAME)) doReturn
            workProfile
        startCollecting()

        receiveSuccessState() // Initial state (selected=0)

        userRepository.setSelectedUser(WORK_USER_ID)
        val workProfileId = receiveSuccessState()
        assertThat(workProfileId.selectedUserId).isEqualTo(WORK_USER_ID)

        userRepository.clearSelectedUser()
        val result = receiveSuccessState()
        assertThat(result.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }

    @Test
    fun getUserState_switchesToDefault_whenSelectedProfileIsPaused() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(PERSONAL_USER_ID)
        val (workUser, workProfile) = getUserInfoAndProfile(WORK_USER_ID)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser, workUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile
        whenever(mockUserProfileFactory.createProfile(workUser, TEST_PACKAGE_NAME)) doReturn
            workProfile
        startCollecting()
        receiveSuccessState() // Initial

        // Select work profile
        userRepository.setSelectedUser(WORK_USER_ID)
        receiveSuccessState()

        // Make work profile paused
        val pausedWorkProfile =
            workProfile.copy(pausedInfo = PausedProfileInfo(PausedReason.QUIET_MODE))
        whenever(mockUserProfileFactory.createProfile(workUser, TEST_PACKAGE_NAME)) doReturn
            pausedWorkProfile

        // Trigger update
        profileChangesFlow.emit(Unit)

        val result = receiveSuccessState()

        // Should switch back to PERSONAL_USER_ID because WORK_USER_ID is paused
        assertThat(result.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }

    @Test
    fun getUserState_switchesToDefault_whenSelectedProfileIsRemoved() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(PERSONAL_USER_ID)
        val (workUser, workProfile) = getUserInfoAndProfile(WORK_USER_ID)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser, workUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile
        whenever(mockUserProfileFactory.createProfile(workUser, TEST_PACKAGE_NAME)) doReturn
            workProfile
        startCollecting()
        receiveSuccessState() // Initial

        // Select work profile
        userRepository.setSelectedUser(WORK_USER_ID)
        receiveSuccessState()

        // Remove work profile
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser)

        // Trigger update
        profileChangesFlow.emit(Unit)

        val result = receiveSuccessState()

        // Should switch back to PERSONAL_USER_ID because WORK_USER_ID is removed
        assertThat(result.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }

    @Test
    fun getUserState_staysOnDefault_whenPausedProfileReturns() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(PERSONAL_USER_ID)
        val (workUser, workProfile) = getUserInfoAndProfile(WORK_USER_ID)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser, workUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile
        whenever(mockUserProfileFactory.createProfile(workUser, TEST_PACKAGE_NAME)) doReturn
            workProfile
        startCollecting()
        receiveSuccessState() // Initial

        // Select work profile
        userRepository.setSelectedUser(WORK_USER_ID)
        receiveSuccessState()

        // Make work profile paused
        val pausedWorkProfile =
            workProfile.copy(pausedInfo = PausedProfileInfo(PausedReason.QUIET_MODE))
        whenever(mockUserProfileFactory.createProfile(workUser, TEST_PACKAGE_NAME)) doReturn
            pausedWorkProfile

        // Trigger update (Paused)
        profileChangesFlow.emit(Unit)
        val resultPaused = receiveSuccessState()
        assertThat(resultPaused.selectedUserId).isEqualTo(PERSONAL_USER_ID)

        // Make work profile available again
        whenever(mockUserProfileFactory.createProfile(workUser, TEST_PACKAGE_NAME)) doReturn
            workProfile

        // Trigger update (Available)
        profileChangesFlow.emit(Unit)
        val resultAvailable = receiveSuccessState()

        // Should STAY on PERSONAL_USER_ID
        assertThat(resultAvailable.selectedUserId).isEqualTo(PERSONAL_USER_ID)
    }

    private fun TestScope.startCollecting() {
        backgroundScope.launch {
            userRepository.getUserState(TEST_PACKAGE_NAME, callingUserId).collect {
                resultsChannel.send(it)
            }
        }
    }

    private fun getUserInfoAndProfile(
        id: Int,
        type: UserType = UserType.PERSONAL,
    ): Pair<UserInfo, UserProfile> {
        val userTypeString =
            when (type) {
                UserType.PERSONAL -> UserManager.USER_TYPE_FULL_SYSTEM
                UserType.WORK -> UserManager.USER_TYPE_PROFILE_MANAGED
                UserType.CLONE -> UserManager.USER_TYPE_PROFILE_CLONE
                UserType.PRIVATE -> UserManager.USER_TYPE_PROFILE_PRIVATE
            }
        val userInfo = UserInfo(id, "User $id", null, 0, userTypeString)
        val userProfile =
            UserProfile(
                userId = id,
                userIdToQueryContacts = id,
                userType = type,
                switchableInfo = null,
            )
        return Pair(userInfo, userProfile)
    }

    private suspend fun receiveSuccessState(): PickerUserState.Success {
        val state = resultsChannel.receive()
        assertThat(state).isInstanceOf(PickerUserState.Success::class.java)
        return state as PickerUserState.Success
    }
}
