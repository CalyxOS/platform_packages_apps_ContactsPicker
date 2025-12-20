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
package com.android.democontactspickerclientapp37

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsPickerSessionContract
import android.widget.Toast

// TODO(b/442397528): support all mime types for ACTION_PICK_CONTACTS
enum class MimeType(val label: String, val mimeTypeString: String) {
    STRUCTURED_NAME("Name", ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
    EMAIL("Email Addresses", ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE),
    PHONE("Phone Numbers", ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
    ADDRESS("Addresses", ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE),
    ORGANIZATION("Company", ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
    RELATION("Related people", ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE),
    EVENT("Birthday", ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE),
    PHOTO("Profile picture", ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
    GROUP_MEMBERSHIP(
        "Contact group",
        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
    ),
    WEBSITE("Website", ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE),
    NICKNAME("Nickname", ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE),
}

internal fun buildActionPickContactsIntent(
    context: Context,
    selectedMimeTypes: Set<MimeType>,
    allowMultiple: Boolean,
    overrideSelectionLimit: Boolean,
    selectionLimit: Int,
): Intent? {
    val intent = Intent(ContactsPickerSessionContract.ACTION_PICK_CONTACTS)
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
    if (overrideSelectionLimit) {
        intent.putExtra(
            ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT,
            selectionLimit,
        )
    }

    val requestedDataFields = ArrayList(selectedMimeTypes.map { it.mimeTypeString })
    if (requestedDataFields.isEmpty()) {
        Toast.makeText(
                context,
                "Select at least one data field for the new picker.",
                Toast.LENGTH_SHORT,
            )
            .show()
        return null
    }
    intent.putStringArrayListExtra(
        ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
        requestedDataFields,
    )
    return intent
}

data class SessionDataRow(val id: Long, val mimeType: String, val value: String?)

data class SessionContact(
    val contactId: Long,
    val displayName: String?,
    val dataRows: List<SessionDataRow>,
)

/** Queries the session URI for the contact data and converts it to a list of [SessionContact]. */
internal fun parseSessionResult(context: Context, uri: Uri): List<SessionContact> {
    val projection =
        arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.MIMETYPE,
            // TODO(b/452020367): check if DATA1 is sufficient for all mimetypes
            ContactsContract.Data.DATA1,
            ContactsContract.Data._ID,
        )

    // TODO(b/452020367): add another view that shows the order of the returned data rows.
    // TODO(b/452020367): verify the desired order of returned values.
    val sortOrder = "${ContactsContract.Data.CONTACT_ID} ASC"

    val contentResolver = context.contentResolver
    val contactsMap = mutableMapOf<Long, MutableList<SessionDataRow>>()
    val namesMap = mutableMapOf<Long, String?>()

    contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
        val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME_PRIMARY)
        val mimeCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
        val dataCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
        val dataIdCol = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID)

        while (cursor.moveToNext()) {
            val contactId = cursor.getLong(idCol)
            val name = cursor.getString(nameCol)
            val mimeType = cursor.getString(mimeCol)
            val value = cursor.getString(dataCol)
            val dataId = cursor.getLong(dataIdCol)

            namesMap.putIfAbsent(contactId, name)
            contactsMap
                .computeIfAbsent(contactId) { mutableListOf() }
                .add(SessionDataRow(dataId, mimeType, value))
        }
    }

    return contactsMap.map { (id, rows) -> SessionContact(id, namesMap[id], rows) }
}
