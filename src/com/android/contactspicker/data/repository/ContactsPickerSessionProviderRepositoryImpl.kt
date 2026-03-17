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

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsPickerSessionContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of [SessionProviderRepository] that uses [ContactsPickerSessionContract] to create
 * sessions.
 */
class ContactsPickerSessionProviderRepositoryImpl
@Inject
constructor(@param:ApplicationContext private val context: Context) :
    ContactsPickerSessionProviderRepository {

    override suspend fun createSession(
        dataIds: List<Long>,
        callingUid: Int,
        sourceUserId: Int,
    ): Uri =
        withContext(Dispatchers.IO) {
            if (dataIds.isEmpty()) {
                throw IllegalArgumentException("Empty dataIds passed")
            }

            val idsString = dataIds.joinToString(",")

            val values =
                ContentValues().apply {
                    put(ContactsPickerSessionContract.Session.CONTACT_DATA_IDS, idsString)
                    put(ContactsPickerSessionContract.Session.SESSION_REQUESTER_UID, callingUid)
                }

            context.contentResolver.insert(
                ContentProvider.maybeAddUserId(
                    ContactsPickerSessionContract.Session.CONTENT_URI,
                    sourceUserId,
                ),
                values,
            ) as Uri
        }
}
