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

import android.content.Context
import android.content.pm.UserInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.UserHandle
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ProfileInfoCacheTest {

    private val mockUserManager: UserManager = mock()
    private val mockUserProfileManager: UserManager = mock()
    private val mockUserProfileManagerFactory: UserProfileManagerFactory = mock()

    private lateinit var profileInfoCache: ProfileInfoCache
    private lateinit var context: Context

    companion object {
        private const val USER_ID_PERSONAL = 0
        private const val USER_ID_WORK = 10
        private const val USER_ID_PRIVATE = 11
        private const val USER_ID_CLONE = 12
        private const val USER_NAME_PERSONAL = "Personal"
        private const val USER_NAME_WORK = "Work"
        private const val USER_NAME_PRIVATE = "Private"
        private const val USER_NAME_CLONE = "Clone"
        private const val LABEL_WORK = "Work Profile Label"
        private const val LABEL_PRIVATE = "Private Profile Label"
        private const val LABEL_PERSONAL = "Personal"
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        profileInfoCache = ProfileInfoCache(context, mockUserManager, mockUserProfileManagerFactory)
        whenever(mockUserProfileManagerFactory.getProfileUserManager(any())) doReturn
            mockUserProfileManager
    }

    @Test
    fun getSwitchableProfileInfo_cachesResult() {
        val userId = USER_ID_WORK
        val userInfo =
            UserInfo(
                userId,
                "User $userId",
                null,
                UserInfo.FLAG_PROFILE,
                UserManager.USER_TYPE_PROFILE_MANAGED,
            )

        val result1 = profileInfoCache.getSwitchableProfileInfo(userInfo)
        val result2 = profileInfoCache.getSwitchableProfileInfo(userInfo)

        verify(mockUserProfileManagerFactory, times(1)).getProfileUserManager(userInfo)
        assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun getSwitchableProfileInfo_returnsCorrectLabel_forProfile() {
        val userId = USER_ID_WORK
        val userInfo =
            UserInfo(
                userId,
                USER_NAME_WORK,
                null,
                UserInfo.FLAG_PROFILE,
                UserManager.USER_TYPE_PROFILE_MANAGED,
            )
        whenever(mockUserManager.getProfileParent(userInfo.userHandle))
            .thenReturn(UserHandle.of(USER_ID_PERSONAL))
        whenever(mockUserProfileManager.profileLabel) doReturn LABEL_WORK

        val result = profileInfoCache.getSwitchableProfileInfo(userInfo)

        assertThat(result.label).isEqualTo(LABEL_WORK)
    }

    @Test
    fun getSwitchableProfileInfo_fetchesIcon_forProfile() {
        val userId = USER_ID_WORK
        val userInfo =
            UserInfo(
                userId,
                USER_NAME_WORK,
                null,
                UserInfo.FLAG_PROFILE,
                UserManager.USER_TYPE_PROFILE_MANAGED,
            )
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val drawable = BitmapDrawable(context.resources, bitmap)
        whenever(mockUserProfileManager.userBadge) doReturn drawable

        val result = profileInfoCache.getSwitchableProfileInfo(userInfo)

        assertThat(result.icon).isNotNull()
    }

    @Test
    fun getSwitchableProfileInfo_returnsNullIcon_whenBadgeIsNull() {
        val userId = USER_ID_WORK
        val userInfo =
            UserInfo(
                userId,
                USER_NAME_WORK,
                null,
                UserInfo.FLAG_PROFILE,
                UserManager.USER_TYPE_PROFILE_MANAGED,
            )
        doReturn(null).whenever(mockUserProfileManager).userBadge

        val result = profileInfoCache.getSwitchableProfileInfo(userInfo)

        assertThat(result.icon).isNull()
    }

    @Test
    fun getSwitchableProfileInfo_returnsCorrectLabel_forPersonalProfile() {
        val userId = USER_ID_PERSONAL
        val userInfo =
            UserInfo(userId, USER_NAME_PERSONAL, null, 0, UserManager.USER_TYPE_FULL_SYSTEM)
        whenever(mockUserManager.getProfileParent(userInfo.userHandle)).thenReturn(null)

        val result = profileInfoCache.getSwitchableProfileInfo(userInfo)

        assertThat(result.label).isEqualTo(context.getString(R.string.user_type_personal))
    }

    @Test
    fun getSwitchableProfileInfo_returnsUnknownLabel_forNonPrimaryProfile_whenLabelIsNull() {
        val userId = USER_ID_CLONE
        val userInfo =
            UserInfo(
                userId,
                USER_NAME_CLONE,
                null,
                UserInfo.FLAG_PROFILE,
                UserManager.USER_TYPE_PROFILE_CLONE,
            )
        whenever(mockUserManager.getProfileParent(userInfo.userHandle))
            .thenReturn(UserHandle.of(USER_ID_PERSONAL))
        whenever(mockUserProfileManager.profileLabel).thenReturn(null as String?)

        val result = profileInfoCache.getSwitchableProfileInfo(userInfo)

        assertThat(result.label).isEqualTo(context.getString(R.string.user_type_unknown_label))
    }

    @Test
    fun getSwitchableProfileInfo_handlesResourcesNotFoundException() {
        val userId = USER_ID_CLONE
        val userInfo =
            UserInfo(
                userId,
                USER_NAME_CLONE,
                null,
                UserInfo.FLAG_PROFILE,
                UserManager.USER_TYPE_PROFILE_CLONE,
            )
        whenever(mockUserManager.getProfileParent(userInfo.userHandle))
            .thenReturn(UserHandle.of(USER_ID_PERSONAL))
        whenever(mockUserProfileManager.profileLabel)
            .doThrow(Resources.NotFoundException("Resource not found"))

        val result = profileInfoCache.getSwitchableProfileInfo(userInfo)

        assertThat(result.label).isEqualTo(context.getString(R.string.user_type_unknown_label))
    }

    @Test
    fun getSwitchableProfileInfo_handlesResourcesNotFoundException_forIcon() {
        val userId = USER_ID_WORK
        val userInfo =
            UserInfo(
                userId,
                USER_NAME_WORK,
                null,
                UserInfo.FLAG_PROFILE,
                UserManager.USER_TYPE_PROFILE_MANAGED,
            )
        whenever(mockUserProfileManager.userBadge)
            .doThrow(Resources.NotFoundException("Badge not found"))
        whenever(mockUserProfileManager.profileLabel).thenReturn(LABEL_WORK)

        val result = profileInfoCache.getSwitchableProfileInfo(userInfo)

        assertThat(result.icon).isNull()
    }

    @Test
    fun clear_clearsCache() {
        val userId = USER_ID_WORK
        val userInfo =
            UserInfo(
                userId,
                "User $userId",
                null,
                UserInfo.FLAG_PROFILE,
                UserManager.USER_TYPE_PROFILE_MANAGED,
            )

        profileInfoCache.getSwitchableProfileInfo(userInfo)
        profileInfoCache.clear()
        profileInfoCache.getSwitchableProfileInfo(userInfo)

        verify(mockUserProfileManagerFactory, times(2)).getProfileUserManager(userInfo)
    }
}
