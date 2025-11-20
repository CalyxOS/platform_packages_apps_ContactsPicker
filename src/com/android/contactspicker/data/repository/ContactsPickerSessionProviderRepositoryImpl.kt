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

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsPickerSessionContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of [SessionProviderRepository] that uses [ContactsPickerSessionContract] to create
 * sessions.
 */
@Singleton
class ContactsPickerSessionProviderRepositoryImpl
@Inject
constructor(@param:ApplicationContext private val context: Context) :
    ContactsPickerSessionProviderRepository {

    override suspend fun createSession(dataUris: List<Uri>, callingUid: Int): Uri =
        withContext(Dispatchers.IO) {
            validateDataUris(dataUris)

            val dataIds = dataUris.joinToString(",") { it.lastPathSegment.toString() }
            val values =
                ContentValues().apply {
                    put(ContactsPickerSessionContract.Session.CONTACT_DATA_IDS, dataIds)
                    put(ContactsPickerSessionContract.Session.SESSION_REQUESTER_UID, callingUid)
                }

            context.contentResolver.insert(
                ContactsPickerSessionContract.Session.CONTENT_URI,
                values,
            ) as Uri
        }

    private fun validateDataUris(dataUris: List<Uri>) {
        if (dataUris.isEmpty()) {
            throw IllegalArgumentException("Empty dataUris passed")
        }
        val hasInvalidIds =
            dataUris.any { uri ->
                val id = uri.lastPathSegment?.toLongOrNull()
                id == null || id <= 0
            }
        // Verify that all IDs are numeric, positive integers
        if (hasInvalidIds) {
            throw IllegalArgumentException("All data URIs must contain positive numeric IDs")
        }
    }
}
