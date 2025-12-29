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
import android.os.UserHandle
import android.os.UserManager
import com.android.contactspicker.data.model.PickerUserStates
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.repository.utils.ProfileChangesMonitor
import com.android.contactspicker.data.repository.utils.UserProfileFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@Singleton
class UserRepositoryImpl
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val userManager: UserManager,
    private val userProfileFactory: UserProfileFactory,
    private val profileChangesMonitor: ProfileChangesMonitor,
) : UserRepository {

    private val _selectedUserId = MutableStateFlow<Int?>(null)

    // TODO(b/479443759): Replace manual UID-to-PackageName conversion with CallingPackageProvider
    override fun getUserStates(callingAppUid: Int): Flow<PickerUserStates> {
        val profilesFlow =
            profileChangesMonitor
                .getProfileChangeFlow()
                .onStart { emit(Unit) }
                .map {
                    // Invalidate the cache to ensure fresh data when profiles change
                    userProfileFactory.clearCache()
                    loadAvailableUsersMap(callingAppUid)
                }
                .flowOn(Dispatchers.IO)

        return combine(profilesFlow, _selectedUserId) { availableUsersMap, selectedUserId ->
            computePickerUserStates(availableUsersMap, callingAppUid, selectedUserId)
        }
    }

    override suspend fun setSelectedUser(userId: Int) {
        _selectedUserId.emit(userId)
    }

    override suspend fun clearSelectedUser() {
        _selectedUserId.emit(null)
    }

    private fun loadAvailableUsersMap(callingAppUid: Int): Map<Int, UserProfile> {
        val currentProcessUserId = UserHandle.myUserId()
        val allProfiles = userManager.getProfiles(currentProcessUserId)

        return allProfiles.associate { userInfo ->
            userInfo.id to
                userProfileFactory.createProfile(
                    userInfo,
                    context.packageManager.getNameForUid(callingAppUid),
                )
        }
    }

    private fun computePickerUserStates(
        userIdToAvailableUsersMap: Map<Int, UserProfile>,
        callingAppUid: Int,
        userSelectedUserId: Int?,
    ): PickerUserStates {
        val callingUserId = UserHandle.getUserId(callingAppUid)
        val currentProcessUserId = UserHandle.myUserId()

        // If an explicit selection exists and is valid, use it.
        // Otherwise, default to the calling user's profile (mapped via logic in factory/map) or
        // current process user.
        val targetUserId =
            if (
                userSelectedUserId != null &&
                    userIdToAvailableUsersMap.containsKey(userSelectedUserId)
            ) {
                userSelectedUserId
            } else {
                userIdToAvailableUsersMap[callingUserId]?.userIdToQueryContacts
                    ?: currentProcessUserId
            }

        return PickerUserStates(
            userIdToAvailableUsersMap = userIdToAvailableUsersMap,
            selectedUserId = targetUserId,
        )
    }
}
