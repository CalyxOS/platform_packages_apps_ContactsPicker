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

import androidx.compose.ui.graphics.ImageBitmap

/** Represents a user profile in the Contacts Picker. */
data class UserProfile(
    /** The Android user ID for this profile. */
    val userId: Int,
    /**
     * The Android user ID that stores the contacts data for this profile. Usually same as [userId],
     * but different for Clone profiles which use their parent's contacts.
     */
    val userIdToQueryContacts: Int,
    /** The type of the user profile (e.g., Personal, Work). */
    val userType: UserType,
    /** Information required to display the profile in the switcher. Null if not shown. */
    val switchableInfo: SwitchableProfileInfo?,
    /** Information about why the profile is paused. Null if not paused. */
    // TODO(b/479456070): Rename pausedInfo to inaccessibleInfo
    val pausedInfo: PausedProfileInfo? = null,
)

/** Contains UI information for a switchable profile. */
data class SwitchableProfileInfo(
    /** The display label for the profile. */
    val label: String,
    /** The badge icon associated with the profile. */
    val icon: ImageBitmap?,
)

/** Contains information about a paused profile. */
data class PausedProfileInfo(
    /** The reason why the profile is paused. */
    val pausedReason: PausedReason
)

/** Type of the user profile. */
enum class UserType {
    PERSONAL,
    WORK,
    CLONE,
    PRIVATE,
}

enum class PausedReason {
    UNKNOWN_REASON,
    QUIET_MODE,
    MANAGED_PROFILE_CONTACTS_BLOCKED,
}
