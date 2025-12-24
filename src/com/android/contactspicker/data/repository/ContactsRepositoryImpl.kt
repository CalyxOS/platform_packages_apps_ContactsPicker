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

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Contacts.MATCH_ALL_MIMETYPES_PARAM_KEY
import android.provider.ContactsContract.Contacts.REQUESTED_MIMETYPES_PARAM_KEY
import android.provider.ContactsContract.Data
import com.android.contactspicker.R
import com.android.contactspicker.config.ContactsQueryMode
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.EmailEntry
import com.android.contactspicker.data.model.MimeType
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.data.model.PhoneEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO(b/463918621): Define these inside CP2 as hidden APIs
private const val CONTACTS_DATA_URI_PATH = "contacts_data"
private const val CONTACTS_DATA_FILTER_URI_PATH = "contacts_data/filter"

@Singleton
class ContactsRepositoryImpl
@Inject
constructor(@param:ApplicationContext private val context: Context) : ContactsRepository {

    private val contentResolver = context.contentResolver

    companion object {
        private val DISPLAY_NAME_FETCH_PROJECTION =
            arrayOf(
                Contacts._ID,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.PHOTO_THUMBNAIL_URI,
                Contacts.STARRED,
                Contacts.LOOKUP_KEY,
            )
        private val PHONE_FILTER_PROJECTION =
            arrayOf(
                Phone.CONTACT_ID,
                Phone.DISPLAY_NAME_PRIMARY,
                Phone.PHOTO_THUMBNAIL_URI,
                Phone.NUMBER,
                Phone._ID,
            )
        private val EMAIL_FILTER_PROJECTION =
            arrayOf(
                Email.CONTACT_ID,
                Email.DISPLAY_NAME_PRIMARY,
                Email.PHOTO_THUMBNAIL_URI,
                Email.ADDRESS,
                Email._ID,
            )
        private val DISPLAY_NAME_FILTER_PROJECTION =
            arrayOf(
                Contacts._ID,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.PHOTO_THUMBNAIL_URI,
                Contacts.LOOKUP_KEY,
            )
    }

    override suspend fun getContacts(queryMode: ContactsQueryMode): List<Contact> =
        withContext(Dispatchers.IO) {
            when (queryMode) {
                ContactsQueryMode.EmailsOnly -> getEmailContacts()
                ContactsQueryMode.PhonesOnly -> getPhoneContacts()
                ContactsQueryMode.DisplayNamesOnly -> getDisplayNameContacts()
                is ContactsQueryMode.Custom -> {
                    getContactsWithMimetypes(
                        queryMode.mimetypes,
                        queryMode.matchAllRequestedMimeTypes,
                    )
                }
            }
        }

    override suspend fun searchContacts(
        query: String,
        queryMode: ContactsQueryMode,
    ): List<Contact> {
        if (query.isBlank()) {
            return emptyList()
        }
        return withContext(Dispatchers.IO) {
            when (queryMode) {
                ContactsQueryMode.EmailsOnly -> searchEmails(query)
                ContactsQueryMode.PhonesOnly -> searchPhones(query)
                ContactsQueryMode.DisplayNamesOnly -> searchDisplayNames(query)
                is ContactsQueryMode.Custom -> {
                    searchContactsByMimeTypes(
                        query,
                        queryMode.mimetypes,
                        queryMode.matchAllRequestedMimeTypes,
                    )
                }
            }
        }
    }

    override suspend fun getDataRowIds(
        contactIds: List<Long>,
        mimeTypes: List<MimeType>,
    ): List<Long> {
        if (contactIds.isEmpty() || mimeTypes.isEmpty()) return emptyList()

        val keySelection = "${Data.CONTACT_ID} IN (${contactIds.joinToString(",") { "?" }})"
        val mimeSelection = "${Data.MIMETYPE} IN (${mimeTypes.joinToString(",") { "?" }})"

        val selection = "$keySelection AND $mimeSelection"

        // TODO(b/452020367): add json to work-around the 999 limit, see comment on ag/37450004.
        //  (lookupKeys.size + mimeTypes.size) must never exceed 999 (the Android SQLite limit).
        //  Max size of contactIds is 100 (max selection limit) and max size of mimeTypes is ~11
        //  (number of supported mime types).
        val selectionArgs =
            (contactIds.map { it.toString() } + mimeTypes.map { it.value }).toTypedArray()

        return buildList {
            contentResolver
                .query(Data.CONTENT_URI, arrayOf(Data._ID), selection, selectionArgs, null)
                ?.use { cursor ->
                    val idColumnIndex = cursor.getColumnIndexOrThrow(Data._ID)
                    while (cursor.moveToNext()) {
                        add(cursor.getLong(idColumnIndex))
                    }
                }
        }
    }

    private fun getEmailContacts(): List<Contact> {
        val contacts = mutableMapOf<Long, EmailContact>()
        val projection =
            arrayOf(
                Email.CONTACT_ID,
                Email.DISPLAY_NAME_PRIMARY,
                Email.PHOTO_THUMBNAIL_URI,
                Email.STARRED,
                Email.ADDRESS,
                Email._ID,
                Email.TYPE,
                Email.LABEL,
            )
        val cursor =
            contentResolver.query(
                Email.CONTENT_URI,
                projection,
                null, // No specific selection
                null, // No selection args
                Data.SORT_KEY_PRIMARY + " ASC",
            )

        cursor?.use {
            val idIndex = it.getColumnIndex(Email.CONTACT_ID)
            val nameIndex = it.getColumnIndex(Email.DISPLAY_NAME_PRIMARY)
            val profilePictureUriIndex = it.getColumnIndex(Email.PHOTO_THUMBNAIL_URI)
            val starredIndex = it.getColumnIndex(Email.STARRED)
            val addressIndex = it.getColumnIndex(Email.ADDRESS)
            val dataIdIndex = it.getColumnIndex(Email._ID)
            val typeIndex = it.getColumnIndex(Email.TYPE)
            val labelIndex = it.getColumnIndex(Email.LABEL)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex)
                val profilePictureUri = it.getString(profilePictureUriIndex)
                val isFavorite = it.getInt(starredIndex) == 1
                val address = it.getString(addressIndex)
                val dataId = it.getLong(dataIdIndex)
                val type = it.getInt(typeIndex)
                val customLabel = it.getString(labelIndex)

                // TODO(b/436818961): support displaying contacts that do not have display name
                if (!name.isNullOrBlank() && !address.isNullOrBlank()) {
                    val label = Email.getTypeLabel(context.resources, type, customLabel).toString()
                    val emailEntry = EmailEntry(dataId, address, label)

                    if (contacts.containsKey(id)) {
                        val existingContact = contacts[id]!!
                        contacts[id] =
                            existingContact.copy(emails = existingContact.emails + emailEntry)
                    } else {
                        contacts[id] =
                            EmailContact(
                                id = id,
                                displayName = name,
                                profilePictureUri = profilePictureUri,
                                isFavorite = isFavorite,
                                emails = listOf(emailEntry),
                            )
                    }
                }
            }
        }
        return contacts.values.toList()
    }

    private fun getPhoneContacts(): List<Contact> {
        val contacts = mutableMapOf<Long, PhoneContact>()
        val projection =
            arrayOf(
                Phone.CONTACT_ID,
                Phone.DISPLAY_NAME_PRIMARY,
                Phone.PHOTO_THUMBNAIL_URI,
                Phone.STARRED,
                Phone.NUMBER,
                Phone._ID,
                Phone.TYPE,
                Phone.LABEL,
            )
        val cursor =
            contentResolver.query(
                Phone.CONTENT_URI,
                projection,
                null, // No specific selection
                null, // No selection args
                Data.SORT_KEY_PRIMARY + " ASC",
            )

        cursor?.use {
            val idIndex = it.getColumnIndex(Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(Phone.DISPLAY_NAME_PRIMARY)
            val profilePictureUriIndex = it.getColumnIndex(Phone.PHOTO_THUMBNAIL_URI)
            val starredIndex = it.getColumnIndex(Phone.STARRED)
            val numberIndex = it.getColumnIndex(Phone.NUMBER)
            val dataIdIndex = it.getColumnIndex(Phone._ID)
            val typeIndex = it.getColumnIndex(Phone.TYPE)
            val labelIndex = it.getColumnIndex(Phone.LABEL)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex)
                val profilePictureUri = it.getString(profilePictureUriIndex)
                val isFavorite = it.getInt(starredIndex) == 1
                val number = it.getString(numberIndex)
                val dataId = it.getLong(dataIdIndex)
                val type = it.getInt(typeIndex)
                val customLabel = it.getString(labelIndex)

                if (!name.isNullOrBlank() && !number.isNullOrBlank()) {
                    val label = Phone.getTypeLabel(context.resources, type, customLabel).toString()
                    val phoneEntry = PhoneEntry(dataId, number, label)

                    if (contacts.containsKey(id)) {
                        val existingContact = contacts[id]!!
                        contacts[id] =
                            existingContact.copy(phones = existingContact.phones + phoneEntry)
                    } else {
                        contacts[id] =
                            PhoneContact(
                                id = id,
                                displayName = name,
                                profilePictureUri = profilePictureUri,
                                isFavorite = isFavorite,
                                phones = listOf(phoneEntry),
                            )
                    }
                }
            }
        }
        return contacts.values.toList()
    }

    private fun getDisplayNameContacts(): List<Contact> {
        val cursor =
            contentResolver.query(
                Contacts.CONTENT_URI,
                DISPLAY_NAME_FETCH_PROJECTION,
                null, // No specific selection
                null, // No selection args
                Data.SORT_KEY_PRIMARY + " ASC",
            )

        return cursor?.use(::parseDisplayNameContacts) ?: emptyList()
    }

    private fun getContactsWithMimetypes(
        mimetypes: List<MimeType>,
        matchAllRequestedMimetypes: Boolean,
    ): List<Contact> {
        if (mimetypes.isEmpty()) {
            return emptyList()
        }

        // TODO(467326511#comment3): consider fix in the CP2 matcher and change the used URI
        val uri =
            ContactsContract.AUTHORITY_URI.buildUpon()
                .appendPath(CONTACTS_DATA_URI_PATH)
                .appendQueryParameter(
                    REQUESTED_MIMETYPES_PARAM_KEY,
                    mimetypes.joinToString(",") { it.value },
                )
                .appendQueryParameter(
                    MATCH_ALL_MIMETYPES_PARAM_KEY,
                    matchAllRequestedMimetypes.toString(),
                )
                .build()

        val cursor =
            contentResolver.query(
                uri,
                DISPLAY_NAME_FETCH_PROJECTION,
                null,
                null,
                Contacts.SORT_KEY_PRIMARY + " ASC",
            )

        return cursor?.use(::parseDisplayNameContacts) ?: emptyList()
    }

    private fun searchContactsByMimeTypes(
        query: String,
        mimetypes: List<MimeType>,
        matchAllRequestedMimetypes: Boolean,
    ): List<Contact> {

        // TODO(467326511#comment3): consider fix in the CP2 matcher and change the used URI
        val uri =
            ContactsContract.AUTHORITY_URI.buildUpon()
                .appendPath(CONTACTS_DATA_FILTER_URI_PATH)
                .appendPath(query)
                .appendQueryParameter(
                    REQUESTED_MIMETYPES_PARAM_KEY,
                    mimetypes.joinToString(",") { it.value },
                )
                .appendQueryParameter(
                    MATCH_ALL_MIMETYPES_PARAM_KEY,
                    matchAllRequestedMimetypes.toString(),
                )
                .build()

        val cursor = contentResolver.query(uri, DISPLAY_NAME_FETCH_PROJECTION, null, null, null)

        return cursor?.use(::parseDisplayNameContacts) ?: emptyList()
    }

    private fun parseDisplayNameContacts(cursor: Cursor): List<DisplayNameContact> {
        val contacts = mutableListOf<DisplayNameContact>()
        val idIndex = cursor.getColumnIndex(Contacts._ID)
        val nameIndex = cursor.getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY)
        val profilePictureUriIndex = cursor.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI)
        val starredIndex = cursor.getColumnIndex(Contacts.STARRED)
        val lookupKeyIndex = cursor.getColumnIndex(Contacts.LOOKUP_KEY)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            val name = cursor.getString(nameIndex)?.takeIf{ it.isNotBlank() } ?: context.getString(R.string.no_name_placeholder)
            val profilePictureUri = cursor.getString(profilePictureUriIndex)
            val isFavorite = cursor.getInt(starredIndex) == 1
            val lookupKey = cursor.getString(lookupKeyIndex)
            if (name != null) {
                contacts.add(
                    DisplayNameContact(
                        id = id,
                        displayName = name,
                        profilePictureUri = profilePictureUri,
                        isFavorite = isFavorite,
                        lookupKey = lookupKey,
                    )
                )
            }
        }
        return contacts
    }

    private fun searchPhones(query: String): List<Contact> {
        return searchWithFilter(
            query,
            Phone.CONTENT_FILTER_URI,
            PHONE_FILTER_PROJECTION,
            Phone.NUMBER,
        ) { id, displayName, profilePictureUri, dataId, number ->
            PhoneContact(
                id = id,
                displayName = displayName,
                profilePictureUri = profilePictureUri,
                isFavorite = false,
                phones = listOf(PhoneEntry(dataId, number)),
            )
        }
    }

    private fun searchEmails(query: String): List<Contact> {
        return searchWithFilter(
            query,
            Email.CONTENT_FILTER_URI,
            EMAIL_FILTER_PROJECTION,
            Email.ADDRESS,
        ) { id, displayName, profilePictureUri, dataId, address ->
            EmailContact(
                id = id,
                displayName = displayName,
                profilePictureUri = profilePictureUri,
                isFavorite = false,
                emails = listOf(EmailEntry(dataId, address)),
            )
        }
    }

    /**
     * Searches contacts using [Contacts.CONTENT_FILTER_URI], which matches against name, phone,
     * email, and other data fields.
     *
     * @return A list of [DisplayNameContact] matching the search query.
     */
    private fun searchDisplayNames(query: String): List<Contact> {
        val filterUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, query)
        val contacts = mutableListOf<DisplayNameContact>()
        contentResolver.query(filterUri, DISPLAY_NAME_FILTER_PROJECTION, null, null, null)?.use {
            cursor ->
            val idIndex = cursor.getColumnIndex(Contacts._ID)
            val nameIndex = cursor.getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY)
            val profilePictureUriIndex = cursor.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI)
            val lookupKeyIndex = cursor.getColumnIndex(Contacts.LOOKUP_KEY)

            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idIndex)
                val displayName = cursor.getString(nameIndex)
                val profilePictureUri = cursor.getString(profilePictureUriIndex)
                val lookupKey = cursor.getString(lookupKeyIndex)

                if (!displayName.isNullOrBlank() && !lookupKey.isNullOrBlank()) {
                    contacts.add(
                        DisplayNameContact(
                            id = contactId,
                            displayName = displayName,
                            profilePictureUri = profilePictureUri,
                            isFavorite = false,
                            lookupKey = lookupKey,
                        )
                    )
                }
            }
        }
        return contacts
    }

    private fun searchWithFilter(
        query: String,
        filterUri: Uri,
        projection: Array<String>,
        dataColumnName: String,
        parseContact:
            (
                id: Long,
                displayName: String,
                profilePictureUri: String?,
                dataId: Long,
                dataValue: String,
            ) -> Contact,
    ): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val uri = filterUri.buildUpon().appendPath(query).build()

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(Data.CONTACT_ID)
            val nameIndex = cursor.getColumnIndex(Data.DISPLAY_NAME_PRIMARY)
            val profilePictureUriIndex = cursor.getColumnIndex(Data.PHOTO_THUMBNAIL_URI)
            val dataValueIndex = cursor.getColumnIndex(dataColumnName)
            val dataIdIndex = cursor.getColumnIndex(Data._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex)
                val profilePictureUri = cursor.getString(profilePictureUriIndex)
                val dataValue = cursor.getString(dataValueIndex)
                val dataId = cursor.getLong(dataIdIndex)

                if (!name.isNullOrBlank() && !dataValue.isNullOrBlank()) {
                    // TODO(b/451963918) Confirm if we return aggregated contacts or single data
                    // rows
                    contacts.add(parseContact(id, name, profilePictureUri, dataId, dataValue))
                }
            }
        }
        return contacts
    }
}
