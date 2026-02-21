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

import com.android.contactspicker.config.ContactsQueryMode
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.MimeType
import com.android.contactspicker.data.repository.ContactsRepository

/** A fake implementation of ContactsRepository for use in tests. */
class FakeContactsRepository : ContactsRepository {

    private var initialContacts: List<Contact> = emptyList()
    private var exceptionToThrow: Exception? = null
    private val searchResultsMap = mutableMapOf<String, List<Contact>>()
    private val searchExceptionMap = mutableMapOf<String, Exception>()
    private val searchInvocationsCountMap = mutableMapOf<String, Int>()
    private val dataRowIdsMap = mutableMapOf<Pair<Set<Long>, Set<MimeType>>, List<Long>>()
    private var getContactsInvocationsCount = 0
    private var hasAnyContacts = true

    var lastGetDataRowIdsUserId: Int? = null
        private set

    var lastSearchContactsUserId: Int? = null
        private set

    fun setInitialContacts(contacts: List<Contact>) {
        initialContacts = contacts
        exceptionToThrow = null
    }

    fun setException(exception: Exception) {
        exceptionToThrow = exception
    }

    fun setSearchResults(query: String, results: List<Contact>) {
        searchResultsMap[query] = results
        searchExceptionMap.remove(query)
    }

    fun setSearchException(query: String, exception: Exception) {
        searchExceptionMap[query] = exception
        searchResultsMap.remove(query)
    }

    fun setDataRowIdsResult(
        contactIds: List<Long>,
        mimeTypes: List<MimeType>,
        dataIds: List<Long>,
    ) {
        dataRowIdsMap[contactIds.toSet() to mimeTypes.toSet()] = dataIds
    }

    fun setHasAnyContacts(value: Boolean) {
        hasAnyContacts = value
    }

    /** Returns the number of times [searchContacts] has been invoked with the provided [query]. */
    fun searchInvocationsCountForQuery(query: String): Int {
        return searchInvocationsCountMap.getOrDefault(query, 0)
    }

    fun getContactsInvocationsCount(): Int {
        return getContactsInvocationsCount
    }

    override suspend fun getContacts(queryMode: ContactsQueryMode, userId: Int): List<Contact> {
        getContactsInvocationsCount++
        exceptionToThrow?.let { throw it }
        return initialContacts
    }

    override suspend fun searchContacts(
        query: String,
        queryMode: ContactsQueryMode,
        userId: Int,
    ): List<Contact> {
        lastSearchContactsUserId = userId
        searchInvocationsCountMap[query] = searchInvocationsCountMap.getOrDefault(query, 0) + 1
        searchExceptionMap[query]?.let { throw it }
        return searchResultsMap[query] ?: emptyList()
    }

    override suspend fun getDataRowIds(
        contactIds: List<Long>,
        mimeTypes: List<MimeType>,
        userId: Int,
    ): List<Long> {
        lastGetDataRowIdsUserId = userId
        return dataRowIdsMap[contactIds.toSet() to mimeTypes.toSet()] ?: emptyList()
    }

    override suspend fun hasAnyContacts(userId: Int): Boolean =
        if (initialContacts.isNotEmpty()) true else hasAnyContacts
}
