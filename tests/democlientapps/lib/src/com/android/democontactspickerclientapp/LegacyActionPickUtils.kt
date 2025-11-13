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
import android.content.Context
import android.content.Intent
import android.database.Cursor
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

/** Queries the ContentResolver to format the data for display based on the URI type. */
private fun formatUriData(context: Context, uri: Uri, pickerType: LegacyPickerType): ContactResult {
    val contentResolver = context.contentResolver
    val projection =
        when (pickerType) {
            LegacyPickerType.EMAIL ->
                arrayOf(
                    ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Email.ADDRESS,
                )
            LegacyPickerType.PHONE ->
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                )
            LegacyPickerType.CONTACT ->
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID)
        }

    val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

    return cursor.use { c ->
        if (c != null && c.moveToFirst()) {
            when (pickerType) {
                LegacyPickerType.EMAIL -> {
                    val nameIndex =
                        c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
                    val addressIndex =
                        c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    val name =
                        if (nameIndex != -1) c.getString(nameIndex) ?: "NULL"
                        else "No contact index"
                    val address =
                        if (addressIndex != -1) c.getString(addressIndex) ?: "NULL"
                        else "No address index"

                    ContactResult(name, address, "Email", uri.toString())
                }
                LegacyPickerType.PHONE -> {
                    val nameIndex =
                        c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex =
                        c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val name =
                        if (nameIndex != -1) c.getString(nameIndex) ?: "NULL"
                        else "No contact index"
                    val number =
                        if (numberIndex != -1) c.getString(numberIndex) ?: "NULL"
                        else "No phone index"
                    ContactResult(name, number, "Phone", uri.toString())
                }
                LegacyPickerType.CONTACT -> {
                    val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                    val name =
                        if (nameIndex != -1) c.getString(nameIndex) ?: "NULL"
                        else "No contact index"
                    val id =
                        if (idIndex != -1) c.getString(idIndex) ?: "NULL" else "No contact ID index"
                    ContactResult(name, id, "Contact ID", uri.toString())
                }
            }
        } else {
            ContactResult(
                contactName = "Error",
                detail = "Could not retrieve data for URI (Check read permission)",
                detailLabel = "Error Message",
                uri = uri.toString(),
            )
        }
    }
}

fun handlePickerResult(
    context: Context,
    result: ActivityResult,
    pickerType: LegacyPickerType,
): PickerResult {
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
            val formattedResults = uris.map { uri -> formatUriData(context, uri, pickerType) }
            PickerResult(statusText = "Received items: ${uris.size}", contacts = formattedResults)
        } else {
            PickerResult(
                statusText = "Picker returned OK, but no URI was found.",
                contacts = emptyList(),
            )
        }
    } else {
        return PickerResult(statusText = "Picker was canceled or failed.", contacts = emptyList())
    }
}
