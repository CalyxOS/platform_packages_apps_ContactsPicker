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
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.android.contactspicker.data.model.PausedProfileInfo
import com.android.contactspicker.data.model.PausedReason
import com.android.contactspicker.data.model.SwitchableProfileInfo
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.model.UserType
import javax.inject.Inject
import javax.inject.Provider

private const val TAG = "UserProfileFactory"

open class UserProfileFactory
@Inject
constructor(
    private val userManager: UserManager,
    private val profileInfoCache: ProfileInfoCache,
    private val devicePolicyManager: Provider<DevicePolicyManager>,
) {

    open fun createProfile(userInfo: UserInfo, callingPackage: String?): UserProfile? {
        val userProperties = userManager.getUserProperties(userInfo.userHandle)
        val currentProcessUserId = UserHandle.myUserId()

        // Determine if the profile should be excluded from the picker entirely.
        if (userInfo.id != currentProcessUserId) {
            val isVisibleInSharingSurfaces =
                userProperties.showInSharingSurfaces != UserProperties.SHOW_IN_SHARING_SURFACES_NO
            val isHiddenInQuietMode =
                userInfo.isQuietModeEnabled &&
                    userProperties.showInQuietMode == UserProperties.SHOW_IN_QUIET_MODE_HIDDEN

            if (!isVisibleInSharingSurfaces || isHiddenInQuietMode) {
                return null
            }
        }

        val userIdToQueryContacts = getUserIdToQueryContacts(userInfo, userProperties)
        val pausedReason = getPausedReason(userInfo, userProperties, callingPackage)
        val switchableInfo = getSwitchableInfo(userInfo, userProperties)

        return UserProfile(
            userId = userInfo.id,
            userIdToQueryContacts = userIdToQueryContacts,
            userType = mapToInternalUserType(userInfo),
            switchableInfo = switchableInfo,
            pausedInfo = pausedReason?.let { PausedProfileInfo(it) },
        )
    }

    private fun getUserIdToQueryContacts(userInfo: UserInfo, userProperties: UserProperties?): Int {
        // TODO(b/479451420): Use UserProperties.getUseParentsContacts() once the permission
        // MANAGE_USERS is granted to the ContactsPicker process.
        val useParentsContacts = userInfo.userType == UserManager.USER_TYPE_PROFILE_CLONE
        if (!useParentsContacts) {
            return userInfo.id
        }

        val parentHandle = userManager.getProfileParent(userInfo.userHandle)
        return parentHandle?.identifier ?: userInfo.id
    }

    private fun getPausedReason(
        userInfo: UserInfo,
        userProperties: UserProperties?,
        callingPackage: String?,
    ): PausedReason? {

        if (userInfo.userType == UserManager.USER_TYPE_PROFILE_MANAGED) {
            if (!hasManagedProfileContactsAccess(userInfo, callingPackage)) {
                return PausedReason.MANAGED_PROFILE_CONTACTS_BLOCKED
            }
        }

        val showInQuietMode =
            userProperties?.showInQuietMode ?: UserProperties.SHOW_IN_QUIET_MODE_DEFAULT

        if (userInfo.isQuietModeEnabled) {
            if (showInQuietMode == UserProperties.SHOW_IN_QUIET_MODE_PAUSED) {
                return PausedReason.QUIET_MODE
            } else if (showInQuietMode == UserProperties.SHOW_IN_QUIET_MODE_DEFAULT) {
                // For profiles with default quiet mode property settings, fall back to pausing the
                // profile with unknown paused reason in the UI.
                return PausedReason.UNKNOWN_REASON
            }
        }

        return null
    }

    private fun getSwitchableInfo(
        userInfo: UserInfo,
        userProperties: UserProperties?,
    ): SwitchableProfileInfo? {
        // Profiles that share parent contacts (e.g., Clones) don't need a separate tab since they
        // display the same contacts as the parent.
        // TODO(b/479451420): Use UserProperties.getUseParentsContacts() once the permission
        // MANAGE_USERS is granted to the ContactsPicker process.
        val useParentsContacts = userInfo.userType == UserManager.USER_TYPE_PROFILE_CLONE
        if (useParentsContacts) {
            return null
        }

        // Default: Show the profile tab (e.g., Work Profiles, or unlocked Private Profiles).
        return profileInfoCache.getSwitchableProfileInfo(userInfo)
    }

    /**
     * Checks if the calling app has access to the managed profile's contacts. This method should
     * only be called with userInfo of a managed profile.
     *
     * @param userInfo The user info of the managed profile.
     * @param callingPackage The package name of the calling app.
     * @return True if the calling app has access to the managed profile's contacts, false
     *   otherwise.
     */
    private fun hasManagedProfileContactsAccess(
        userInfo: UserInfo,
        callingPackage: String?,
    ): Boolean {
        if (userInfo.id == UserHandle.myUserId()) {
            return true
        }
        if (callingPackage == null) {
            Log.w(
                TAG,
                "Access denied to managed profile: callingPackage is null. User ID: ${userInfo.id}",
            )
            return false
        }
        return devicePolicyManager
            .get()
            .hasManagedProfileContactsAccess(userInfo.userHandle, callingPackage)
    }

    private fun mapToInternalUserType(userInfo: UserInfo): UserType {
        return when (userInfo.userType) {
            UserManager.USER_TYPE_PROFILE_MANAGED -> UserType.WORK
            UserManager.USER_TYPE_PROFILE_CLONE -> UserType.CLONE
            UserManager.USER_TYPE_PROFILE_PRIVATE -> UserType.PRIVATE
            else -> UserType.PERSONAL
        }
    }

    /** Clears the profile info cache. */
    open fun clearCache() {
        profileInfoCache.clear()
    }
}
