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
package com.android.contactspicker.fakes

import com.android.contactspicker.data.repository.PrivacyBannerRepository

/** A fake implementation of PrivacyBannerRepository for use in tests. */
class FakePrivacyBannerRepository : PrivacyBannerRepository {

    private val privacyBannerShown = mutableMapOf<Pair<Int, List<String>>, Boolean>()

    fun markPrivacyBannerAsShown(appUid: Int, mimeTypes: List<String>) {
        privacyBannerShown[Pair(appUid, mimeTypes)] = true
    }

    override suspend fun wasPrivacyBannerShown(appUid: Int, mimeTypes: List<String>): Boolean {
        return privacyBannerShown.getOrDefault(Pair(appUid, mimeTypes), false)
    }
}
