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
import android.os.UserManager
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

/**
 * Caches static profile information (label and icon) to avoid expensive Context creation
 * operations.
 */
@Singleton
@OpenForTesting
// TODO(b/479451420): Re evaluate the APIs used to show profile icon and label
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
            val userProfileManager = userProfileManagerFactory.getProfileUserManager(userInfo)
            SwitchableProfileInfo(
                label = getProfileLabel(context, userProfileManager, userManager, userInfo),
                icon = getProfileIcon(userProfileManager, userInfo),
            )
        }
    }

    // TODO(b/479447282): Refactor profile icon logic to use SHOW_IN_SHARING_SURFACES user property
    private fun getProfileIcon(userProfileManager: UserManager?, userInfo: UserInfo): ImageBitmap? {
        val isIconSupported = userInfo.isManagedProfile || userInfo.isPrivateProfile

        if (isIconSupported) {
            val badge = userProfileManager?.userBadge
            if (badge != null && badge.intrinsicWidth > 0 && badge.intrinsicHeight > 0) {
                return badge.toBitmap().asImageBitmap()
            }
        }
        return null
    }

    private fun getProfileLabel(
        context: Context,
        userProfileManager: UserManager?,
        userManager: UserManager,
        userInfo: UserInfo,
    ): String {
        val isPrimaryProfile = userManager.getProfileParent(userInfo.userHandle) == null
        if (isPrimaryProfile) {
            return context.getString(R.string.user_type_personal)
        }

        val unknownUserLabel = context.getString(R.string.user_type_unknown_label)
        val isLabelSupported = userInfo.isManagedProfile || userInfo.isPrivateProfile
        if (isLabelSupported) {
            return userProfileManager?.profileLabel ?: unknownUserLabel
        }

        return unknownUserLabel
    }

    open fun clear() {
        cache.clear()
    }
}
