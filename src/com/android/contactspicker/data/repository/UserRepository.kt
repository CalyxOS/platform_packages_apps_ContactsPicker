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

import android.os.UserHandle
import android.os.UserManager
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.repository.utils.ProfileChangesMonitor
import com.android.contactspicker.data.repository.utils.UserProfileFactory
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class UserRepository
@Inject
constructor(
    private val userManager: UserManager,
    private val userProfileFactory: UserProfileFactory,
    private val profileChangesMonitor: ProfileChangesMonitor,
) {

    /**
     * Emits a map of available [UserProfile]s keyed by user ID, updating automatically on system
     * profile changes. Profiles that are hidden or unauthorized for the calling app are excluded.
     *
     * @param callingPackageName The client app's package name, used to verify cross-profile access.
     */
    // TODO(b/479464524): Optimize profile data reload during changes in profiles to only update
    // the modified profile
    fun getAvailableUsersFlow(callingPackageName: String?): Flow<Map<Int, UserProfile>> {
        return profileChangesMonitor
            .getProfileChangeFlow()
            .onStart { emit(Unit) }
            .map {
                userProfileFactory.clearCache()
                loadAvailableUsersMap(callingPackageName)
            }
            .flowOn(Dispatchers.IO)
    }

    private fun loadAvailableUsersMap(callingPackageName: String?): Map<Int, UserProfile> {
        val currentProcessUserId = UserHandle.myUserId()
        val allProfiles = userManager.getProfiles(currentProcessUserId)

        return allProfiles
            .mapNotNull { userInfo ->
                userProfileFactory.createProfile(userInfo, callingPackageName)
            }
            .associateBy { it.userId }
    }
}
