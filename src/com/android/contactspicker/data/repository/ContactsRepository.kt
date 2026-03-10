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

import com.android.contactspicker.config.ContactsQueryMode
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.MimeType
import com.android.contactspicker.viewmodel.GroupedContactsData

/** Interface for accessing contact data from the Android ContactsProvider. */
interface ContactsRepository {

    /**
     * Retrieves the initial list of contacts appropriate for the given [ContactsQueryMode].
     *
     * @param queryMode The mode of query to perform.
     * @param userId The user ID to query contacts for.
     * @return A [GroupedContactsData] object containing the list of contacts and the grouping
     *   information.
     */
    suspend fun getContacts(queryMode: ContactsQueryMode, userId: Int): GroupedContactsData

    /**
     * Searches for contacts that match the given query and have at least one of the requested mime
     * types.
     *
     * @param query The text to search for in contact names, emails, and phone numbers.
     * @param queryMode The mode of query to perform.
     * @param userId The user ID to query contacts for.
     * @return A list of matching [Contact]s.
     */
    suspend fun searchContacts(
        query: String,
        queryMode: ContactsQueryMode,
        userId: Int,
    ): List<Contact>

    /**
     * Retrieves Data Row IDs for specific contact lookup keys and mime types.
     *
     * @param contactIds List of contact lookup keys.
     * @param mimeTypes List of mime types.
     * @param userId The user ID to query contacts for.
     * @return A list of unique data row IDs.
     */
    suspend fun getDataRowIds(
        contactIds: List<Long>,
        mimeTypes: List<MimeType>,
        userId: Int,
    ): List<Long>

    /** Returns true if the user has at least one contact. */
    suspend fun hasAnyContacts(userId: Int): Boolean
}
