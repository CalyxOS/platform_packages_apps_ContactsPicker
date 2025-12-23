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

import com.android.contactspicker.data.model.MimeType

/** Repository to persist state of the privacy banner shown state. */
interface PrivacyBannerRepository {
    /**
     * Checks if the privacy banner has been shown for a given `appUid` and `mimeTypes` combination
     * and marks it as shown if it has not.
     *
     * This check-and-set operation ensures the banner is only shown once per unique context.
     *
     * @param appUid The unique identifier of the calling application.
     * @param mimeTypes The list of contact data MIME types for which the state is being checked.
     * @return `true` if the banner was previously shown, `false` if it was just marked as shown by
     *   this call.
     */
    suspend fun wasPrivacyBannerShown(appUid: Int, mimeTypes: List<MimeType>): Boolean
}
