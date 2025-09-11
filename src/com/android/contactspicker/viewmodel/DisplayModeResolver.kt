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
package com.android.contactspicker.viewmodel

import android.content.Intent
import android.provider.ContactsContract
import com.android.contactspicker.DisplayMode

object DisplayModeResolver {
    /**
     * Resolves the display mode from an intent action and type. Returns null if the intent is not
     * supported.
     */
    fun resolve(intentAction: String?, intentType: String?): DisplayMode? {
        // TODO(b/442397528): revisit that all mime types are correctly supported
        return when (intentAction) {
            Intent.ACTION_PICK ->
                when (intentType) {
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE ->
                        DisplayMode.EMAIL_SELECTION
                    ContactsContract.CommonDataKinds.Email.CONTENT_TYPE ->
                        DisplayMode.EMAIL_SELECTION
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE ->
                        DisplayMode.PHONE_SELECTION
                    ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE ->
                        DisplayMode.PHONE_SELECTION
                    ContactsContract.Contacts.CONTENT_TYPE -> DisplayMode.CONTACT_SELECTION
                    ContactsContract.Contacts.CONTENT_ITEM_TYPE -> DisplayMode.CONTACT_SELECTION
                    else -> null
                }
            else -> null
        }
    }
}
