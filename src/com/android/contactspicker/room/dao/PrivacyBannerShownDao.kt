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
package com.android.contactspicker.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.contactspicker.data.model.MimeType
import com.android.contactspicker.room.entity.PrivacyBannerShown

/** Data access object for the [PrivacyBannerShown] table. */
@Dao
interface PrivacyBannerShownDao {
    /**
     * Returns whether the privacy banner has been shown for a given app uid and set of MIME types.
     *
     * @param appUid The unique identifier of the application.
     * @param mimeTypes The integer bitmask representing the set of contact data MIME types.
     * @return `true` if the banner has been shown, `false` otherwise.
     */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM privacy_banner_shown " +
            "WHERE app_uid = :appUid AND mime_types = :mimeTypes)"
    )
    suspend fun wasPrivacyBannerShown(appUid: Int, mimeTypes: List<MimeType>): Boolean

    /**
     * inserts PrivacyBannerShown object into database
     *
     * @param [PrivacyBannerShown] entity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(privacyBannerShown: PrivacyBannerShown)
}
