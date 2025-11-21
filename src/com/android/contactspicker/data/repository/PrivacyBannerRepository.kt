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

/** Repository to persist state of the privacy banner shown state. */
interface PrivacyBannerRepository {
    /**
     * Returns whether the privacy banner has been shown for a given app uid and list of MIME types.
     *
     * @param appUid The unique identifier of the calling application. MimeTypes The list of contact
     *   data MIME types.
     * @return `true` if the banner has been shown, `false` otherwise.
     */
    suspend fun hasPrivacyBannerBeenShown(appUid: String, mimeTypes: List<String>): Boolean

    /**
     * Marks the privacy banner as shown for a given app uid and set of MIME types.
     *
     * @param appUid The unique identifier of the calling application.
     * @param mimeTypes The list of contact data MIME types.
     */
    suspend fun markPrivacyBannerShown(appUid: String, mimeTypes: List<String>)
}
