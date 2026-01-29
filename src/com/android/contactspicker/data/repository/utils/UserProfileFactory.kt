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
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.android.contactspicker.data.model.PausedProfileInfo
import com.android.contactspicker.data.model.PausedReason
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.model.UserType
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val TAG = "UserProfileFactory"

@Singleton
open class UserProfileFactory
@Inject
constructor(
    private val userManager: UserManager,
    private val profileInfoCache: ProfileInfoCache,
    private val devicePolicyManager: Provider<DevicePolicyManager>,
) {

    open fun createProfile(userInfo: UserInfo, callingPackage: String?): UserProfile {
        return when {
            userInfo.isManagedProfile -> createWorkProfile(userInfo, callingPackage)
            userInfo.isCloneProfile -> createCloneProfile(userInfo)
            userInfo.isPrivateProfile -> createPrivateProfile(userInfo)
            else -> createPersonalProfile(userInfo)
        }
    }

    private fun createWorkProfile(userInfo: UserInfo, callingPackage: String?): UserProfile {
        val isManagedProfileContactsAccessAllowed =
            // Check if the target profile is the same as the current process user.
            // If so, we always allow access (local access), bypassing the cross-profile check.
            if (userInfo.id == UserHandle.myUserId()) {
                true
            } else if (callingPackage != null) {
                devicePolicyManager
                    .get()
                    .hasManagedProfileContactsAccess(userInfo.userHandle, callingPackage)
            } else {
                // Deny access by default if we cannot verify the caller to prevent potential data
                // leaks.
                Log.w(
                    TAG,
                    "Access denied to managed profile: callingPackage is null. User ID: ${userInfo.id}",
                )
                false
            }

        val pausedReason =
            if (!isManagedProfileContactsAccessAllowed) {
                PausedReason.MANAGED_PROFILE_CONTACTS_BLOCKED
            } else if (userInfo.isQuietModeEnabled) {
                PausedReason.QUIET_MODE
            } else {
                PausedReason.UNDEFINED
            }

        return UserProfile(
            userId = userInfo.id,
            userIdToQueryContacts = userInfo.id,
            userType = UserType.WORK,
            switchableInfo = profileInfoCache.getSwitchableProfileInfo(userInfo),
            pausedInfo =
                if (pausedReason != PausedReason.UNDEFINED) {
                    PausedProfileInfo(pausedReason)
                } else {
                    null
                },
        )
    }

    private fun createCloneProfile(userInfo: UserInfo): UserProfile {
        // Clone profiles rely on the Contacts Provider of their parent profile.
        // We use the parent's user ID to query the data so the user sees their main contact list.
        // If no parent is found, fallback to the clone's ID (which results in an empty list).
        val parentUserProfileId = userManager.getProfileParent(userInfo.userHandle)?.identifier
        val delegatedUserId = parentUserProfileId ?: userInfo.id

        return UserProfile(
            userId = userInfo.id,
            userIdToQueryContacts = delegatedUserId,
            userType = UserType.CLONE,
            switchableInfo = null,
        )
    }

    private fun createPrivateProfile(userInfo: UserInfo): UserProfile {
        return UserProfile(
            userId = userInfo.id,
            userIdToQueryContacts = userInfo.id,
            userType = UserType.PRIVATE,
            switchableInfo =
                if (userInfo.isQuietModeEnabled) {
                    null
                } else {
                    profileInfoCache.getSwitchableProfileInfo(userInfo)
                },
        )
    }

    private fun createPersonalProfile(userInfo: UserInfo): UserProfile {
        return UserProfile(
            userId = userInfo.id,
            userIdToQueryContacts = userInfo.id,
            userType = UserType.PERSONAL,
            switchableInfo = profileInfoCache.getSwitchableProfileInfo(userInfo),
        )
    }

    /** Clears the profile info cache. */
    open fun clearCache() {
        profileInfoCache.clear()
    }
}
