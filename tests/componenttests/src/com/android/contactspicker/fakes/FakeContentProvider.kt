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

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsPickerSessionContract

/**
 * A fake [android.content.ContentProvider] for testing that allows setting a specific
 * [android.database.Cursor] to be returned for a given [android.net.Uri].
 */
class FakeContentProvider : ContentProvider() {

    private val cursorMap = mutableMapOf<Uri, Cursor>()
    private val sessionMap = mutableMapOf<Int, Uri>()
    private val sessionContentValuesMap = mutableMapOf<Int, ContentValues>()

    fun setCursorForUri(uri: Uri, cursor: Cursor) {
        cursorMap[uri] = cursor
    }

    fun insertContactsPickerSessionProviderUri(uid: Int, uri: Uri) {
        sessionMap[uid] = uri
    }

    fun getContactsPickerSessionProviderInsertContent(uid: Int): ContentValues? {
        return sessionContentValuesMap[uid]
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? {
        return cursorMap[uri]
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return values
            ?.getAsInteger(ContactsPickerSessionContract.Session.SESSION_REQUESTER_UID)
            ?.let { uid ->
                sessionContentValuesMap[uid] = values
                sessionMap[uid]
            }
    }

    // Unused abstract methods

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int {
        return 0
    }
}
