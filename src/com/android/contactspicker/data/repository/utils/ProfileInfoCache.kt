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
import android.os.UserManager
import android.util.Log
import androidx.annotation.OpenForTesting
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.android.contactspicker.R
import com.android.contactspicker.data.model.SwitchableProfileInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProfileInfoCache"

/**
 * Caches static profile information (label and icon) to avoid expensive Context creation
 * operations.
 */
@Singleton
@OpenForTesting
open class ProfileInfoCache
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val userManager: UserManager,
    private val userProfileManagerFactory: UserProfileManagerFactory,
) {

    private val cache = ConcurrentHashMap<Int, SwitchableProfileInfo>()

    open fun getSwitchableProfileInfo(userInfo: UserInfo): SwitchableProfileInfo {
        return cache.computeIfAbsent(userInfo.id) {
            val profileUserManager = userProfileManagerFactory.getProfileUserManager(userInfo)
            SwitchableProfileInfo(
                label = getProfileLabel(context, userInfo, profileUserManager),
                icon = getProfileIcon(userInfo, profileUserManager),
            )
        }
    }

    /**
     * Returns the profile icon. If null is returned, the UI will fall back to the default icon (see
     *
     * @see com.android.contactspicker.ui.pickerscreen.ProfileSwitcher#getFallbackIcon()
     */
    private fun getProfileIcon(userInfo: UserInfo, profileUserManager: UserManager?): ImageBitmap? {
        try {
            val badge = profileUserManager?.userBadge
            if (badge != null && badge.intrinsicWidth > 0 && badge.intrinsicHeight > 0) {
                return badge.toBitmap().asImageBitmap()
            }
        } catch (e: Resources.NotFoundException) {
            Log.w(TAG, "Profile icon resource not found for user ${userInfo.id}", e)
        }
        return null
    }

    private fun getProfileLabel(
        context: Context,
        userInfo: UserInfo,
        profileUserManager: UserManager?,
    ): String {
        if (userInfo.isProfile) {
            try {
                val label = profileUserManager?.profileLabel
                if (label != null) {
                    return label
                }
            } catch (e: Resources.NotFoundException) {
                Log.w(TAG, "Profile label resource not found for user ${userInfo.id}", e)
            }

            Log.w(TAG, "Falling back to unknown label for user ${userInfo.id}")
            return context.getString(R.string.user_type_unknown_label)
        }
        return context.getString(R.string.user_type_personal)
    }

    open fun clear() {
        cache.clear()
    }
}
