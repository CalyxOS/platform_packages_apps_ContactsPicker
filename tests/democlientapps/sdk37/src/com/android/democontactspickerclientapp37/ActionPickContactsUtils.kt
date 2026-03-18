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

enum class MimeType(
    val label: String,
    val mimeTypeString: String,
    val isSupported: Boolean = true,
) {
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
    // The unsupported mime type
    NOTE(
        "Notes (Unsupported)",
        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE,
        isSupported = false,
    ),
}

internal fun buildActionPickContactsIntent(
    context: Context,
    selectedMimeTypes: Set<MimeType>,
    allowMultiple: Boolean,
    matchAllDataFields: Boolean,
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
    intent.putExtra(
        ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_MATCH_ALL_DATA_FIELDS,
        matchAllDataFields,
    )

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

data class SessionDataRow(val id: Long, val mimeType: String, val value: Any?)

data class SessionFlatRow(val contactId: Long, val displayName: String?, val row: SessionDataRow)

data class SessionContact(
    val contactId: Long,
    val displayName: String?,
    val dataRows: List<SessionDataRow>,
)

data class SessionParseResult(
    val orderedRows: List<SessionFlatRow>,
    val aggregatedContacts: List<SessionContact>,
)

/** Queries the session URI for the contact data and converts it to a [SessionParseResult]. */
internal fun parseSessionResult(context: Context, uri: Uri): SessionParseResult {
    val projection =
        arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA15,
            ContactsContract.Data._ID,
        )

    val contentResolver = context.contentResolver

    val orderedRows = mutableListOf<SessionFlatRow>()
    val contactsMap = mutableMapOf<Long, MutableList<SessionDataRow>>()
    val namesMap = mutableMapOf<Long, String?>()

    contentResolver.query(uri, projection, null, null)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
        val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME_PRIMARY)
        val mimeCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
        val dataCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
        val data15Col = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA15)
        val dataIdCol = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID)

        while (cursor.moveToNext()) {
            val contactId = cursor.getLong(idCol)
            val name = cursor.getString(nameCol)
            val mimeType = cursor.getString(mimeCol)
            val dataId = cursor.getLong(dataIdCol)

            val value: Any? =
                if (mimeType.equals(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)) {
                    cursor.getBlob(data15Col)
                } else {
                    cursor.getString(dataCol)
                }

            val row = SessionDataRow(dataId, mimeType, value)

            orderedRows.add(SessionFlatRow(contactId, name, row))

            namesMap.putIfAbsent(contactId, name)
            contactsMap.computeIfAbsent(contactId) { mutableListOf() }.add(row)
        }
    }

    val aggregatedContacts =
        contactsMap.map { (id, rows) -> SessionContact(id, namesMap[id], rows) }
    return SessionParseResult(orderedRows, aggregatedContacts)
}
