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

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.UserInfo
import android.os.UserManager
import com.android.contactspicker.data.model.PickerUserStates
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
    }

    private val mockContext: Context = mock()
    private val mockUserManager: UserManager = mock()
    private val mockUserProfileFactory: UserProfileFactory = mock()
    private val mockProfileChangesMonitor: ProfileChangesMonitor = mock()
    private val mockPackageManager: PackageManager = mock()

    private val profileChangesFlow = MutableSharedFlow<Unit>(replay = 1)
    private lateinit var userRepository: UserRepositoryImpl
    private val callingAppUid = 12345
    private val resultsChannel = Channel<PickerUserStates>(Channel.UNLIMITED)

    @Before
    fun setUp() {
        whenever(mockContext.packageManager) doReturn mockPackageManager
        whenever(mockProfileChangesMonitor.getProfileChangeFlow()) doReturn profileChangesFlow
        whenever(mockPackageManager.getNameForUid(callingAppUid)) doReturn TEST_PACKAGE_NAME

        userRepository =
            UserRepositoryImpl(
                mockContext,
                mockUserManager,
                mockUserProfileFactory,
                mockProfileChangesMonitor,
            )
    }

    @Test
    fun getUserStates_returnsInitialState() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(0)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile
        startCollecting()

        val result = resultsChannel.receive()

        assertThat(result.userIdToAvailableUsersMap).hasSize(1)
        assertThat(result.userIdToAvailableUsersMap[0]).isEqualTo(primaryProfile)
        assertThat(result.selectedUserId).isEqualTo(0)
        verify(mockUserProfileFactory).clearCache()
    }

    @Test
    fun getUserStates_updatesOnProfileChange() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(0)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile
        startCollecting()
        resultsChannel.receive() // Initial state

        val (workUser, workProfile) = getUserInfoAndProfile(10)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser, workUser)
        whenever(mockUserProfileFactory.createProfile(workUser, TEST_PACKAGE_NAME)) doReturn
            workProfile
        profileChangesFlow.emit(Unit)

        val result = resultsChannel.receive()

        assertThat(result.userIdToAvailableUsersMap).hasSize(2)
        assertThat(result.userIdToAvailableUsersMap[0]).isEqualTo(primaryProfile)
        assertThat(result.userIdToAvailableUsersMap[10]).isEqualTo(workProfile)
        assertThat(result.selectedUserId).isEqualTo(0)
        verify(mockUserProfileFactory, times(2)).clearCache()
    }

    @Test
    fun setSelectedUser_updatesSelectedId() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(0)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser)
        whenever(mockUserProfileFactory.createProfile(any(), any())) doReturn primaryProfile
        startCollecting()

        val first = resultsChannel.receive()
        assertThat(first.selectedUserId).isEqualTo(0)
        val (workUser, workProfile) = getUserInfoAndProfile(10)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser, workUser)
        whenever(mockUserProfileFactory.createProfile(workUser, null)) doReturn workProfile
        profileChangesFlow.emit(Unit)
        resultsChannel.receive()

        userRepository.setSelectedUser(10)
        val third = resultsChannel.receive()

        assertThat(third.selectedUserId).isEqualTo(10)

        userRepository.setSelectedUser(999)
        val fourth = resultsChannel.receive()

        assertThat(fourth.selectedUserId).isEqualTo(0)
    }

    @Test
    fun clearSelectedUser_resetsToDefault() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(0)
        val (workUser, workProfile) = getUserInfoAndProfile(10)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser, workUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile
        whenever(mockUserProfileFactory.createProfile(workUser, TEST_PACKAGE_NAME)) doReturn
            workProfile
        startCollecting()

        resultsChannel.receive() // Initial state (selected=0)

        userRepository.setSelectedUser(10)
        val workProfileId = resultsChannel.receive()
        assertThat(workProfileId.selectedUserId).isEqualTo(10)

        userRepository.clearSelectedUser()
        val result = resultsChannel.receive()
        assertThat(result.selectedUserId).isEqualTo(0)
    }

    private fun TestScope.startCollecting() {
        backgroundScope.launch {
            userRepository.getUserStates(callingAppUid).collect { resultsChannel.send(it) }
        }
    }

    private fun getUserInfoAndProfile(id: Int): Pair<UserInfo, UserProfile> {
        val userInfo = UserInfo(id, "User $id", 0)
        val userProfile =
            UserProfile(
                userId = id,
                userIdToQueryContacts = id,
                userType = UserType.PERSONAL,
                switchableInfo = null,
            )
        return Pair(userInfo, userProfile)
    }
}
