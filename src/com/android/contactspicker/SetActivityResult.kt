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
package com.android.contactspicker

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Creates a result intent for a single selected contact URI.
 *
 * @param context The context.
 * @param intent The original intent that was used to launch the Contacts Picker.
 * @param uri The selected contact URI.
 * @param callingUid The UID of the calling app.
 * @return The result intent to be returned to the calling app.
 */
fun createSingleSelectionResult(
    context: Context,
    intent: Intent,
    uri: Uri,
    callingUid: Int,
): Intent {
    if (intent.action != Intent.ACTION_PICK) {
        // TODO(b/441478451): Support ACTION_PICK_CONTACTS later.
        throw IllegalArgumentException("Unsupported intent action: ${intent.action}")
    }
    val resultIntent = Intent()
    resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    resultIntent.data = uri
    return resultIntent
}

/**
 * Creates a result intent for multiple selected contact URIs.
 *
 * @param context The context.
 * @param intent The original intent that was used to launch the Contacts Picker.
 * @param uris The list of selected contact URIs.
 * @param callingUid The UID of the calling app.
 * @return The result intent to be returned to the calling app.
 */
fun createMultiSelectionResult(
    context: Context,
    intent: Intent,
    uris: List<Uri>,
    callingUid: Int,
): Intent {
    if (intent.action != Intent.ACTION_PICK) {
        // TODO(b/441478451): Support ACTION_PICK_CONTACTS later.
        throw IllegalArgumentException("Unsupported intent action: ${intent.action}")
    }
    return Intent().apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (uris.isNotEmpty()) {
            clipData =
                ClipData.newUri(context.contentResolver, "uri", uris.first()).apply {
                    uris.drop(1).forEach { addItem(ClipData.Item(it)) }
                }
        } else {
            throw IllegalArgumentException("Empty uriList passed")
        }
    }
}
