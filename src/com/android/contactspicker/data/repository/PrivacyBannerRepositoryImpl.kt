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
package com.android.contactspicker.data.repository

import com.android.contactspicker.room.dao.PrivacyBannerShownDao
import com.android.contactspicker.room.entity.PrivacyBannerShown
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A repository for persisting the state of the privacy banner shown state. This class uses DAO
 * object to store whether the banner has been shown to the user.
 */
@Singleton
class PrivacyBannerRepositoryImpl @Inject constructor(private val dao: PrivacyBannerShownDao) :
    PrivacyBannerRepository {
    override suspend fun hasPrivacyBannerBeenShown(
        appUid: String,
        mimeTypes: List<String>,
    ): Boolean {
        return dao.hasPrivacyBannerBeenShown(appUid, mimeTypes)
    }

    override suspend fun markPrivacyBannerShown(appUid: String, mimeTypes: List<String>) {
        dao.insert(PrivacyBannerShown(appUid = appUid, mimeTypes = mimeTypes))
    }
}
