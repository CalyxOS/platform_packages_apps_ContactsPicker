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

import android.net.Uri

/** Interface for managing contact picker sessions. */
interface ContactsPickerSessionProviderRepository {
    /**
     * Creates a new contacts picker session.
     *
     * @param dataUris List of URIs for the selected contact data.
     * @param callingUid The UID of the calling application.
     * @return The URI of the created session.
     */
    suspend fun createSession(dataUris: List<Uri>, callingUid: Int): Uri
}
