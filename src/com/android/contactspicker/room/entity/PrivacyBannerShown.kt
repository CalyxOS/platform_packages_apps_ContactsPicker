/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.contactspicker.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a Room entity that stores whether the privacy banner has been shown for a specific app
 * requesting a particular set of contact data fields (MIME types).
 *
 * This entity is used to prevent showing the same privacy banner repeatedly to the user for the
 * same app and mimeType combination.
 *
 * For example, if a banner is shown for an app with `appUid = "com.example.app"` requesting
 * `["phone", "email"]`, a row is inserted. If the same app later requests only `["phone"]`, a new,
 * distinct row will be inserted for that combination. This allows multiple entries for the same
 * `appUid` as long as the requested `mimeTypes` combinations are different.
 *
 * @property id The unique identifier for the database record.
 * @property appUid The unique identifier of the application for which the banner was shown.
 * @property mimeTypes An integer bitmask representing the set of contact data MIME types for which
 *   the banner was shown. This is managed by [MimeTypeConverter].
 */
@Entity(tableName = "privacy_banner_shown", indices = [Index(value = ["app_uid"])])
data class PrivacyBannerShown(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Int = 0,
    @ColumnInfo(name = "app_uid") val appUid: String,
    @ColumnInfo(name = "mime_types") val mimeTypes: Int,
)
