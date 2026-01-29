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

package com.android.contactspicker.data.model

/** Data class for the profile blocked dialog. */
data class ProfileBlockedDialogData(val title: String, val message: String)

/** State holding the available users/profiles and the current selection. */
data class PickerUserStates(
    /**
     * Map of user ID to [UserProfile] for all profiles associated with the current foreground user.
     * This includes the personal profile, work profile, and any other associated profiles.
     */
    val userIdToAvailableUsersMap: Map<Int, UserProfile>,
    /**
     * The user ID of the currently active profile in the picker UI. This determines which contacts
     * are currently displayed to the human user.
     */
    val selectedUserId: Int,
    /**
     * Data for a dialog to be shown when a profile is blocked or paused. If null, no dialog is
     * shown.
     */
    val profileBlockedDialogData: ProfileBlockedDialogData? = null,
)
