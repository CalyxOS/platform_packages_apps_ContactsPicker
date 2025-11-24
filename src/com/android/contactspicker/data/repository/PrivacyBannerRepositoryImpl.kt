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

import android.util.Log
import com.android.contactspicker.room.dao.PrivacyBannerShownDao
import com.android.contactspicker.room.entity.PrivacyBannerShown
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PrivacyBannerRepository"
// TODO(b/462100085): Validate the privacy banner's show/hide logic during exception handling
private const val DEFAULT_PRIVACY_BANNER_SHOWN_STATE_FALLBACK = false

/**
 * A repository for persisting the state of the privacy banner shown state. This class uses DAO
 * object to store whether the banner has been shown to the user.
 */
@Singleton
class PrivacyBannerRepositoryImpl @Inject constructor(private val dao: PrivacyBannerShownDao) :
    PrivacyBannerRepository {

    override suspend fun wasPrivacyBannerShown(appUid: Int, mimeTypes: List<String>): Boolean {
        try {
            if (appUid != -1 && mimeTypes.isNotEmpty()) {
                val wasPrivacyBannerShown = dao.wasPrivacyBannerShown(appUid, mimeTypes)
                Log.i(TAG, "Privacy banner shown : $wasPrivacyBannerShown")
                if (!wasPrivacyBannerShown) {
                    dao.insert(PrivacyBannerShown(appUid = appUid, mimeTypes = mimeTypes))
                }
                return wasPrivacyBannerShown
            } else {
                return DEFAULT_PRIVACY_BANNER_SHOWN_STATE_FALLBACK
            }
        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred while checking privacy banner shown state.", e)
            return DEFAULT_PRIVACY_BANNER_SHOWN_STATE_FALLBACK
        }
    }
}
