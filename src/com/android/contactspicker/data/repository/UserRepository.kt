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

import com.android.contactspicker.data.model.PickerUserStates
import kotlinx.coroutines.flow.Flow

/** Repository to manage user profiles and their states. */
interface UserRepository {
    /**
     * Gets a flow of states of all profiles associated with the foreground user.
     *
     * @param callingAppUid The UID of the app that invoked the picker.
     */
    fun getUserStates(callingAppUid: Int): Flow<PickerUserStates>

    /**
     * Updates the currently selected user profile.
     *
     * @param userId The ID of the user to select.
     */
    suspend fun setSelectedUser(userId: Int)

    /**
     * Clears the last selected user-profile, this will reset the selection to display the
     * user-profile in which contacts picker is launched.
     */
    suspend fun clearSelectedUser()
}
