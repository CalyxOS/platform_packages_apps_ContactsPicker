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
package com.android.contactspicker.viewmodel

import android.os.UserHandle
import com.android.contactspicker.data.model.PickerUserState
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.repository.UserRepository
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

@ViewModelScoped
class ProfileSelectionHandler @Inject constructor(private val userRepository: UserRepository) {
    private val _selectedUserId = MutableStateFlow<Int?>(null)

    /**
     * Gets a flow of states of all profiles associated with the foreground user.
     *
     * @param callingPackageName The package name of the app that invoked the picker.
     * @param callingUserId The user ID of the app that invoked the picker.
     */
    fun getUserStateFlow(callingPackageName: String?, callingUserId: Int): Flow<PickerUserState> {
        // UserRepository now ONLY returns the available profiles (Flow<Map<Int, UserProfile>>)
        val profilesFlow =
            userRepository.getAvailableUsersFlow(callingPackageName).onEach { availableUsersMap ->
                // If the explicitly selected profile becomes paused or unavailable,
                // clear the explicit selection so the picker falls back permanently.
                val currentSelected = _selectedUserId.value
                if (currentSelected != null) {
                    val profile = availableUsersMap[currentSelected]
                    if (profile?.isSelectable() != true) {
                        _selectedUserId.value = null
                    }
                }
            }

        return combine(profilesFlow, _selectedUserId) { availableUsersMap, selectedUserId ->
            computePickerUserState(availableUsersMap, callingUserId, selectedUserId)
        }
    }

    /**
     * Updates the currently selected user profile.
     *
     * @param userId The ID of the user to select.
     */
    fun setSelectedUser(userId: Int) {
        _selectedUserId.value = userId
    }

    /**
     * Clears the last selected user-profile, this will reset the selection to display the
     * user-profile in which contacts picker is launched.
     */
    fun clearSelectedUser() {
        _selectedUserId.value = null
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
        val isSelectionValid = selectedProfile?.isSelectable() == true

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

    private fun UserProfile.isSelectable(): Boolean {
        return pausedInfo == null && switchableInfo != null
    }
}
