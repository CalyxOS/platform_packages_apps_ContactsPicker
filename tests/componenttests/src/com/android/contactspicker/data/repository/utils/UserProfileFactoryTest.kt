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

package com.android.contactspicker.data.repository.utils

import android.app.admin.DevicePolicyManager
import android.content.pm.UserInfo
import android.content.pm.UserProperties
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
import com.android.contactspicker.data.model.PausedProfileInfo
import com.android.contactspicker.data.model.PausedReason
import com.android.contactspicker.data.model.SwitchableProfileInfo
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.model.UserType
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class UserProfileFactoryTest {

    private val mockUserManager: UserManager = mock()
    private val mockProfileInfoCache: ProfileInfoCache = mock()
    private val mockDevicePolicyManager: DevicePolicyManager = mock()

    private lateinit var userProfileFactory: UserProfileFactory

    @Before
    fun setUp() {
        userProfileFactory =
            UserProfileFactory(mockUserManager, mockProfileInfoCache) { mockDevicePolicyManager }

        val defaultProperties = android.content.pm.UserProperties.Builder().build()
        whenever(mockUserManager.getUserProperties(any())).thenReturn(defaultProperties)
    }

    @Test
    fun createProfile_forPersonalUser_returnsPersonalProfile() {
        val userInfo = createUserInfo(id = USER_ID_PERSONAL, type = UserType.PERSONAL)
        val switchableInfo = SwitchableProfileInfo(USER_NAME_PERSONAL, mockIcon)
        whenever(mockProfileInfoCache.getSwitchableProfileInfo(userInfo)).thenReturn(switchableInfo)

        val result = userProfileFactory.createProfile(userInfo, CALLING_PACKAGE)

        assertThat(result).isEqualTo(EXPECTED_PERSONAL_PROFILE)
    }

    @Test
    fun createProfile_forWorkUser_whenAllowed_returnsWorkProfile() {
        val userInfo = createUserInfo(id = USER_ID_WORK, type = UserType.WORK)
        val switchableInfo = SwitchableProfileInfo(USER_NAME_WORK, mockIcon)
        whenever(mockProfileInfoCache.getSwitchableProfileInfo(userInfo)).thenReturn(switchableInfo)
        whenever(mockDevicePolicyManager.hasManagedProfileContactsAccess(any(), any()))
            .thenReturn(true)

        val result = userProfileFactory.createProfile(userInfo, CALLING_PACKAGE)

        assertThat(result).isEqualTo(EXPECTED_WORK_PROFILE_ALLOWED)
    }

    @Test
    fun createProfile_forWorkUser_whenAccessDenied_returnsPausedProfile() {
        val userInfo = createUserInfo(id = USER_ID_WORK, type = UserType.WORK)
        whenever(mockDevicePolicyManager.hasManagedProfileContactsAccess(any(), any()))
            .thenReturn(false)

        val result = userProfileFactory.createProfile(userInfo, CALLING_PACKAGE)

        assertThat(result).isEqualTo(EXPECTED_WORK_PROFILE_BLOCKED)
    }

    @Test
    fun createProfile_forWorkUser_whenQuietModeEnabled_returnsPausedProfile() {
        val userInfo = createUserInfo(id = USER_ID_WORK, type = UserType.WORK, isQuietMode = true)
        val userProperties =
            android.content.pm.UserProperties.Builder()
                .setShowInQuietMode(android.content.pm.UserProperties.SHOW_IN_QUIET_MODE_PAUSED)
                .build()
        whenever(mockUserManager.getUserProperties(userInfo.userHandle)).thenReturn(userProperties)
        whenever(mockDevicePolicyManager.hasManagedProfileContactsAccess(any(), any()))
            .thenReturn(true)
        val switchableInfo = SwitchableProfileInfo(USER_NAME_WORK, mockIcon)
        whenever(mockProfileInfoCache.getSwitchableProfileInfo(userInfo)).thenReturn(switchableInfo)

        val result = userProfileFactory.createProfile(userInfo, CALLING_PACKAGE)

        assertThat(result).isEqualTo(EXPECTED_WORK_PROFILE_QUIET)
    }

    @Test
    fun createProfile_forWorkUser_whenQuietModeEnabled_andShowInQuietModeDefault_returnsUnknownPausedProfile() {
        val userInfo = createUserInfo(id = USER_ID_WORK, type = UserType.WORK, isQuietMode = true)
        val userProperties =
            android.content.pm.UserProperties.Builder()
                .setShowInQuietMode(android.content.pm.UserProperties.SHOW_IN_QUIET_MODE_DEFAULT)
                .build()
        whenever(mockUserManager.getUserProperties(userInfo.userHandle)).thenReturn(userProperties)
        whenever(mockDevicePolicyManager.hasManagedProfileContactsAccess(any(), any()))
            .thenReturn(true)
        val switchableInfo = SwitchableProfileInfo(USER_NAME_WORK, mockIcon)
        whenever(mockProfileInfoCache.getSwitchableProfileInfo(userInfo)).thenReturn(switchableInfo)

        val result = userProfileFactory.createProfile(userInfo, CALLING_PACKAGE)

        val expected =
            EXPECTED_WORK_PROFILE_ALLOWED.copy(
                pausedInfo = PausedProfileInfo(PausedReason.UNKNOWN_REASON)
            )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun createProfile_forCloneUser_usesParentIdForQuery() {
        val cloneId = USER_ID_CLONE
        val parentId = USER_ID_PERSONAL
        val cloneUserInfo = createUserInfo(id = cloneId, type = UserType.CLONE)
        val parentUserInfo = createUserInfo(id = parentId)

        whenever(mockUserManager.getProfileParent(cloneUserInfo.userHandle))
            .thenReturn(parentUserInfo.userHandle)

        val result = userProfileFactory.createProfile(cloneUserInfo, CALLING_PACKAGE)

        assertThat(result).isEqualTo(EXPECTED_CLONE_PROFILE)
    }

    @Test
    fun createProfile_forPrivateUser_returnsPrivateProfile() {
        val userInfo = createUserInfo(id = USER_ID_PRIVATE, type = UserType.PRIVATE)
        val switchableInfo = SwitchableProfileInfo(USER_NAME_PRIVATE, mockIcon)
        whenever(mockProfileInfoCache.getSwitchableProfileInfo(userInfo)).thenReturn(switchableInfo)

        val result = userProfileFactory.createProfile(userInfo, CALLING_PACKAGE)

        assertThat(result).isEqualTo(EXPECTED_PRIVATE_PROFILE)
    }

    @Test
    fun createProfile_whenShowInSharingSurfacesNo_returnsNull() {
        val userInfo = createUserInfo(id = USER_ID_WORK, type = UserType.WORK)
        val userProperties =
            UserProperties.Builder()
                .setShowInSharingSurfaces(UserProperties.SHOW_IN_SHARING_SURFACES_NO)
                .build()
        whenever(mockUserManager.getUserProperties(userInfo.userHandle)).thenReturn(userProperties)

        val result = userProfileFactory.createProfile(userInfo, CALLING_PACKAGE)

        assertThat(result).isNull()
    }

    @Test
    fun createProfile_forPrivateUser_whenQuietMode_andHidden_returnsNull() {
        val userInfo =
            createUserInfo(id = USER_ID_PRIVATE, type = UserType.PRIVATE, isQuietMode = true)
        val userProperties =
            UserProperties.Builder()
                .setShowInQuietMode(UserProperties.SHOW_IN_QUIET_MODE_HIDDEN)
                .build()
        whenever(mockUserManager.getUserProperties(userInfo.userHandle)).thenReturn(userProperties)

        val result = userProfileFactory.createProfile(userInfo, CALLING_PACKAGE)

        assertThat(result).isNull()
    }

    private fun createUserInfo(
        id: Int,
        type: UserType = UserType.PERSONAL,
        isQuietMode: Boolean = false,
    ): UserInfo {
        var flags = 0
        if (isQuietMode) {
            flags = flags or UserInfo.FLAG_QUIET_MODE
        }

        val userTypeString =
            when (type) {
                UserType.PERSONAL -> UserManager.USER_TYPE_FULL_SYSTEM
                UserType.WORK -> UserManager.USER_TYPE_PROFILE_MANAGED
                UserType.CLONE -> UserManager.USER_TYPE_PROFILE_CLONE
                UserType.PRIVATE -> UserManager.USER_TYPE_PROFILE_PRIVATE
            }

        return UserInfo(id, "User $id", null, flags, userTypeString)
    }

    companion object {
        private val mockIcon: ImageBitmap = mock()

        private const val USER_ID_PERSONAL = 0
        private const val USER_ID_WORK = 10
        private const val USER_ID_CLONE = 12
        private const val USER_ID_PRIVATE = 11
        private const val USER_NAME_PERSONAL = "Personal"
        private const val USER_NAME_WORK = "Work"
        private const val USER_NAME_PRIVATE = "Private"
        private const val CALLING_PACKAGE = "com.package"

        private val EXPECTED_PERSONAL_PROFILE =
            UserProfile(
                userId = USER_ID_PERSONAL,
                userIdToQueryContacts = USER_ID_PERSONAL,
                userType = UserType.PERSONAL,
                switchableInfo = SwitchableProfileInfo(USER_NAME_PERSONAL, mockIcon),
            )

        private val EXPECTED_WORK_PROFILE_ALLOWED =
            UserProfile(
                userId = USER_ID_WORK,
                userIdToQueryContacts = USER_ID_WORK,
                userType = UserType.WORK,
                switchableInfo = SwitchableProfileInfo(USER_NAME_WORK, mockIcon),
            )

        private val EXPECTED_WORK_PROFILE_BLOCKED =
            UserProfile(
                userId = USER_ID_WORK,
                userIdToQueryContacts = USER_ID_WORK,
                userType = UserType.WORK,
                switchableInfo = null,
                pausedInfo = PausedProfileInfo(PausedReason.MANAGED_PROFILE_CONTACTS_BLOCKED),
            )

        private val EXPECTED_WORK_PROFILE_QUIET =
            UserProfile(
                userId = USER_ID_WORK,
                userIdToQueryContacts = USER_ID_WORK,
                userType = UserType.WORK,
                switchableInfo = SwitchableProfileInfo(USER_NAME_WORK, mockIcon),
                pausedInfo = PausedProfileInfo(PausedReason.QUIET_MODE),
            )

        private val EXPECTED_CLONE_PROFILE =
            UserProfile(
                userId = USER_ID_CLONE,
                userIdToQueryContacts = USER_ID_PERSONAL,
                userType = UserType.CLONE,
                switchableInfo = null,
            )

        private val EXPECTED_PRIVATE_PROFILE =
            UserProfile(
                userId = USER_ID_PRIVATE,
                userIdToQueryContacts = USER_ID_PRIVATE,
                userType = UserType.PRIVATE,
                switchableInfo = SwitchableProfileInfo(USER_NAME_PRIVATE, mockIcon),
            )

        private val EXPECTED_PRIVATE_PROFILE_QUIET =
            UserProfile(
                userId = USER_ID_PRIVATE,
                userIdToQueryContacts = USER_ID_PRIVATE,
                userType = UserType.PRIVATE,
                switchableInfo = null,
            )
    }
}
