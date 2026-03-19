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
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class UserRepositoryTest {

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

    private val resultsChannel = Channel<Map<Int, UserProfile>>(Channel.UNLIMITED)
    private lateinit var userRepository: UserRepository

    @Before
    fun setUp() {
        whenever(mockProfileChangesMonitor.getProfileChangeFlow()) doReturn profileChangesFlow

        userRepository =
            UserRepository(mockUserManager, mockUserProfileFactory, mockProfileChangesMonitor)

        // Default UserProperties for all users unless overridden in tests
        val defaultProperties = UserProperties.Builder().build()
        whenever(mockUserManager.getUserProperties(any())).thenReturn(defaultProperties)
    }

    @Test
    fun getAvailableUsersFlow_filtersProfilesWhenFactoryReturnsNull() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(PERSONAL_USER_ID)
        val (privateUser, _) = getUserInfoAndProfile(PRIVATE_USER_ID, UserType.PRIVATE)

        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser, privateUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile
        whenever(mockUserProfileFactory.createProfile(privateUser, TEST_PACKAGE_NAME)) doReturn null

        startCollecting()

        val result = receiveUsersMap()

        assertThat(result).hasSize(1)
        assertThat(result.containsKey(PRIVATE_USER_ID)).isFalse()
    }

    @Test
    fun getAvailableUsersFlow_returnsInitialState() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(PERSONAL_USER_ID)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile

        startCollecting()

        val result = receiveUsersMap()

        assertThat(result).hasSize(1)
        assertThat(result[PERSONAL_USER_ID]).isEqualTo(primaryProfile)
        verify(mockUserProfileFactory).clearCache()
    }

    @Test
    fun getAvailableUsersFlow_updatesOnProfileChange() = runTest {
        val (primaryUser, primaryProfile) = getUserInfoAndProfile(PERSONAL_USER_ID)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser)
        whenever(mockUserProfileFactory.createProfile(primaryUser, TEST_PACKAGE_NAME)) doReturn
            primaryProfile

        startCollecting()
        receiveUsersMap() // Initial state

        val (workUser, workProfile) = getUserInfoAndProfile(WORK_USER_ID)
        whenever(mockUserManager.getProfiles(any())) doReturn listOf(primaryUser, workUser)
        whenever(mockUserProfileFactory.createProfile(workUser, TEST_PACKAGE_NAME)) doReturn
            workProfile

        profileChangesFlow.emit(Unit)

        val result = receiveUsersMap()

        assertThat(result).hasSize(2)
        assertThat(result[PERSONAL_USER_ID]).isEqualTo(primaryProfile)
        assertThat(result[WORK_USER_ID]).isEqualTo(workProfile)
        verify(mockUserProfileFactory, times(2)).clearCache()
    }

    private fun TestScope.startCollecting() {
        backgroundScope.launch {
            userRepository.getAvailableUsersFlow(TEST_PACKAGE_NAME).collect {
                resultsChannel.send(it)
            }
        }
    }

    private suspend fun receiveUsersMap(): Map<Int, UserProfile> {
        return resultsChannel.receive()
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
}
