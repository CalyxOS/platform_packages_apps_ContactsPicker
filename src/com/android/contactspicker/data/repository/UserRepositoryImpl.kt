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
import com.android.contactspicker.data.model.PickerUserState
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.repository.utils.ProfileChangesMonitor
import com.android.contactspicker.data.repository.utils.UserProfileFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

@Singleton
class UserRepositoryImpl
@Inject
constructor(
    private val userManager: UserManager,
    private val userProfileFactory: UserProfileFactory,
    private val profileChangesMonitor: ProfileChangesMonitor,
) : UserRepository {

    private val _selectedUserId = MutableStateFlow<Int?>(null)

    // TODO(b/479464524): Optimize profile data reload during changes in profiles to only update
    // the modified profile
    override fun getUserState(
        callingPackageName: String?,
        callingUserId: Int,
    ): Flow<PickerUserState> {
        val profilesFlow =
            profileChangesMonitor
                .getProfileChangeFlow()
                .onStart { emit(Unit) }
                .map {
                    // Invalidate the cache to ensure fresh data when profiles change
                    userProfileFactory.clearCache()
                    loadAvailableUsersMap(callingPackageName)
                }
                .onEach { availableUsersMap ->
                    // If the explicitly selected profile becomes paused or unavailable, clear the
                    // explicit selection so the picker falls back permanently and doesn't jump back
                    // unexpectedly when the profile unpauses.
                    val currentSelected = _selectedUserId.value
                    if (currentSelected != null) {
                        val profile = availableUsersMap[currentSelected]
                        if (
                            profile == null ||
                                profile.pausedInfo != null ||
                                profile.switchableInfo == null
                        ) {
                            _selectedUserId.value = null
                        }
                    }
                }
                .flowOn(Dispatchers.IO)

        return combine(profilesFlow, _selectedUserId) { availableUsersMap, selectedUserId ->
            computePickerUserState(availableUsersMap, callingUserId, selectedUserId)
        }
    }

    override suspend fun setSelectedUser(userId: Int) {
        _selectedUserId.emit(userId)
    }

    override suspend fun clearSelectedUser() {
        _selectedUserId.emit(null)
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

    /**
     * Computes the user state for the picker, determining the selected user ID.
     *
     * If an explicit selection exists, is valid (in the map), and is not paused, use it. If the
     * profile is paused (e.g. Quiet Mode), fallback to the default profile.
     */
    private fun computePickerUserState(
        userIdToAvailableUsersMap: Map<Int, UserProfile>,
        callingUserId: Int,
        userSelectedUserId: Int?,
    ): PickerUserState.Success {
        val currentProcessUserId = UserHandle.myUserId()

        val selectedProfile = userSelectedUserId?.let { userIdToAvailableUsersMap[it] }
        val isSelectionValid = selectedProfile != null && selectedProfile.pausedInfo == null

        val targetUserId =
            if (userSelectedUserId != null && isSelectionValid) {
                userSelectedUserId
            } else {
                // Ensure the fallback profile is also not paused before defaulting to it.
                // If the calling app's profile is paused, fallback to the current process user.
                userIdToAvailableUsersMap[callingUserId]
                    ?.takeIf { it.pausedInfo == null }
                    ?.userIdToQueryContacts ?: currentProcessUserId
            }

        return PickerUserState.Success(
            userIdToAvailableUsersMap = userIdToAvailableUsersMap,
            selectedUserId = targetUserId,
        )
    }
}
