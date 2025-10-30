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
package com.android.democontactspickerclientapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.result.ActivityResult

fun buildLegacyPickerIntent(
    config: LegacyDemoConfigState,
    allowMultiple: Boolean,
    useSystemPicker: Boolean = false,
): Intent {
    val intent = Intent(Intent.ACTION_PICK)
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
    intent.putExtra(Intent.EXTRA_USE_SYSTEM_CONTACTS_PICKER, useSystemPicker)
    intent.type =
        when (config.legacyPickerType) {
            LegacyPickerType.EMAIL -> ContactsContract.CommonDataKinds.Email.CONTENT_TYPE
            LegacyPickerType.PHONE -> ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
            LegacyPickerType.CONTACT -> ContactsContract.Contacts.CONTENT_TYPE
        }
    return intent
}

// TODO(b/447114080): Access the URIs and display the requested mime types: display name, email, etc
fun handlePickerResult(result: ActivityResult): String {
    if (result.resultCode == Activity.RESULT_OK) {
        val data: Intent? = result.data
        val uris = mutableListOf<Uri>()
        data?.data?.let { uris.add(it) }
        data?.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                uris.add(clipData.getItemAt(i).uri)
            }
        }
        return if (uris.isNotEmpty()) {
            "Success! Received ${uris.size} URI(s):\n" + uris.joinToString("\n")
        } else {
            "Picker returned OK, but no URI was found."
        }
    } else {
        return "Picker was canceled or failed."
    }
}
