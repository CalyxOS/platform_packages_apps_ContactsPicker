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
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
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
private const val CONTACTS_URI_PATH = "contacts"
private const val MIMES_URI_PATH = "mimes"
private const val CONTACTS_DATA_FILTER_URI_PATH = "filter"

@Singleton
class ContactsRepositoryImpl
@Inject
constructor(@param:ApplicationContext private val context: Context) : ContactsRepository {

    private val contentResolver = context.contentResolver

    companion object {
        @VisibleForTesting
        internal val DISPLAY_NAME_FETCH_PROJECTION =
            arrayOf(
                Contacts._ID,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.PHOTO_THUMBNAIL_URI,
                Contacts.STARRED,
                Contacts.LOOKUP_KEY,
            )

        @VisibleForTesting
        internal val EMAIL_FETCH_PROJECTION =
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
        @VisibleForTesting
        internal val PHONE_FETCH_PROJECTION =
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

    private class ContactBuilder(
        val displayName: String,
        val profilePictureUri: String?,
        val isFavorite: Boolean,
    ) {
        val emails = mutableListOf<EmailEntry>()
        val phones = mutableListOf<PhoneEntry>()

        fun toEmailContact(id: Long) =
            EmailContact(
                id = id,
                displayName = displayName,
                profilePictureUri = profilePictureUri,
                isFavorite = isFavorite,
                emails = emails,
            )

        fun toPhoneContact(id: Long) =
            PhoneContact(
                id = id,
                displayName = displayName,
                profilePictureUri = profilePictureUri,
                isFavorite = isFavorite,
                phones = phones,
            )
    }

    override suspend fun getContacts(queryMode: ContactsQueryMode, userId: Int): List<Contact> =
        withContext(Dispatchers.IO) {
            when (queryMode) {
                ContactsQueryMode.EmailsOnly -> getEmailContacts(userId)
                ContactsQueryMode.PhonesOnly -> getPhoneContacts(userId)
                ContactsQueryMode.DisplayNamesOnly -> getDisplayNameContacts(userId)
                is ContactsQueryMode.Custom -> {
                    getContactsWithMimetypes(
                        queryMode.mimetypes,
                        queryMode.matchAllRequestedMimeTypes,
                        userId,
                    )
                }
            }
        }

    override suspend fun searchContacts(
        query: String,
        queryMode: ContactsQueryMode,
        userId: Int,
    ): List<Contact> {
        if (query.isBlank()) {
            return emptyList()
        }
        return withContext(Dispatchers.IO) {
            when (queryMode) {
                ContactsQueryMode.EmailsOnly -> searchEmails(query, userId)
                ContactsQueryMode.PhonesOnly -> searchPhones(query, userId)
                ContactsQueryMode.DisplayNamesOnly -> searchDisplayNames(query, userId)
                is ContactsQueryMode.Custom -> {
                    searchContactsByMimeTypes(
                        query,
                        queryMode.mimetypes,
                        queryMode.matchAllRequestedMimeTypes,
                        userId,
                    )
                }
            }
        }
    }

    override suspend fun getDataRowIds(
        contactIds: List<Long>,
        mimeTypes: List<MimeType>,
        userId: Int,
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
            Array(contactIds.size + mimeTypes.size) { index ->
                if (index < contactIds.size) contactIds[index].toString()
                else mimeTypes[index - contactIds.size].value
            }

        return buildList {
            contentResolver
                .query(
                    ContentProvider.maybeAddUserId(Data.CONTENT_URI, userId),
                    arrayOf(Data._ID),
                    selection,
                    selectionArgs,
                    null,
                )
                ?.use { cursor ->
                    val idColumnIndex = cursor.getColumnIndexOrThrow(Data._ID)
                    while (cursor.moveToNext()) {
                        add(cursor.getLong(idColumnIndex))
                    }
                }
        }
    }

    override suspend fun hasAnyContacts(userId: Int): Boolean {
        val cursor =
            contentResolver.query(
                ContentProvider.maybeAddUserId(Contacts.CONTENT_URI, userId),
                arrayOf(Contacts._ID), // Minimal projection
                null, // No selection
                null, // No selection args
                "${Contacts._ID} LIMIT 1", // Sort order and limit to 1 row (crucial for performance)
            )

        // moveToFirst() returns true if the cursor is not empty
        return cursor?.use { it.moveToFirst() } ?: false
    }

    private fun getEmailContacts(userId: Int): List<Contact> {
        val cursor =
            contentResolver.query(
                ContentProvider.maybeAddUserId(Email.CONTENT_URI, userId),
                EMAIL_FETCH_PROJECTION,
                null, // No specific selection
                null, // No selection args
                Data.SORT_KEY_PRIMARY + " ASC",
            )

        return cursor?.use { c ->
            val builders = mutableMapOf<Long, ContactBuilder>()

            val idIndex = c.getColumnIndex(Email.CONTACT_ID)
            val nameIndex = c.getColumnIndex(Email.DISPLAY_NAME_PRIMARY)
            val profilePictureUriIndex = c.getColumnIndex(Email.PHOTO_THUMBNAIL_URI)
            val starredIndex = c.getColumnIndex(Email.STARRED)
            val addressIndex = c.getColumnIndex(Email.ADDRESS)
            val dataIdIndex = c.getColumnIndex(Email._ID)
            val typeIndex = c.getColumnIndex(Email.TYPE)
            val labelIndex = c.getColumnIndex(Email.LABEL)

            while (c.moveToNext()) {
                val id = c.getLong(idIndex)
                val name = c.getString(nameIndex)
                val address = c.getString(addressIndex)

                // TODO(b/436818961): support displaying contacts that do not have display name
                if (!name.isNullOrBlank() && !address.isNullOrBlank()) {
                    val profilePictureUri = c.getString(profilePictureUriIndex)
                    val isFavorite = c.getInt(starredIndex) == 1
                    val dataId = c.getLong(dataIdIndex)
                    val type = c.getInt(typeIndex)
                    val customLabel = c.getString(labelIndex)

                    val label = Email.getTypeLabel(context.resources, type, customLabel).toString()
                    val emailEntry = EmailEntry(dataId, address, label)

                    builders
                        .getOrPut(id) {
                            ContactBuilder(
                                displayName = name,
                                profilePictureUri = uriStringWithUserId(profilePictureUri, userId),
                                isFavorite = isFavorite,
                            )
                        }
                        .emails
                        .add(emailEntry)
                }
            }
            builders.map { (id, builder) -> builder.toEmailContact(id) }
        } ?: emptyList()
    }

    private fun getPhoneContacts(userId: Int): List<Contact> {
        val cursor =
            contentResolver.query(
                ContentProvider.maybeAddUserId(Phone.CONTENT_URI, userId),
                PHONE_FETCH_PROJECTION,
                null, // No specific selection
                null, // No selection args
                Data.SORT_KEY_PRIMARY + " ASC",
            )

        return cursor?.use { c ->
            val builders = mutableMapOf<Long, ContactBuilder>()

            val idIndex = c.getColumnIndex(Phone.CONTACT_ID)
            val nameIndex = c.getColumnIndex(Phone.DISPLAY_NAME_PRIMARY)
            val profilePictureUriIndex = c.getColumnIndex(Phone.PHOTO_THUMBNAIL_URI)
            val starredIndex = c.getColumnIndex(Phone.STARRED)
            val numberIndex = c.getColumnIndex(Phone.NUMBER)
            val dataIdIndex = c.getColumnIndex(Phone._ID)
            val typeIndex = c.getColumnIndex(Phone.TYPE)
            val labelIndex = c.getColumnIndex(Phone.LABEL)

            while (c.moveToNext()) {
                val id = c.getLong(idIndex)
                val name = c.getString(nameIndex)
                val number = c.getString(numberIndex)

                if (!name.isNullOrBlank() && !number.isNullOrBlank()) {
                    val profilePictureUri = c.getString(profilePictureUriIndex)
                    val isFavorite = c.getInt(starredIndex) == 1
                    val dataId = c.getLong(dataIdIndex)
                    val type = c.getInt(typeIndex)
                    val customLabel = c.getString(labelIndex)

                    val label = Phone.getTypeLabel(context.resources, type, customLabel).toString()
                    val phoneEntry = PhoneEntry(dataId, number, label)

                    builders
                        .getOrPut(id) {
                            ContactBuilder(
                                displayName = name,
                                profilePictureUri = uriStringWithUserId(profilePictureUri, userId),
                                isFavorite = isFavorite,
                            )
                        }
                        .phones
                        .add(phoneEntry)
                }
            }
            builders.map { (id, builder) -> builder.toPhoneContact(id) }
        } ?: emptyList()
    }

    private fun getDisplayNameContacts(userId: Int): List<Contact> {
        val cursor =
            contentResolver.query(
                ContentProvider.maybeAddUserId(Contacts.CONTENT_URI, userId),
                DISPLAY_NAME_FETCH_PROJECTION,
                null, // No specific selection
                null, // No selection args
                Data.SORT_KEY_PRIMARY + " ASC",
            )

        return cursor?.use { parseDisplayNameContacts(it, userId) } ?: emptyList()
    }

    private fun getContactsWithMimetypes(
        mimetypes: List<MimeType>,
        matchAllRequestedMimetypes: Boolean,
        userId: Int,
    ): List<Contact> {
        if (mimetypes.isEmpty()) {
            return emptyList()
        }

        // TODO(467326511#comment3): consider fix in the CP2 matcher and change the used URI
        val uri =
            ContactsContract.AUTHORITY_URI.buildUpon()
                .appendPath(CONTACTS_URI_PATH)
                .appendPath(MIMES_URI_PATH)
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
                ContentProvider.maybeAddUserId(uri, userId),
                DISPLAY_NAME_FETCH_PROJECTION,
                null,
                null,
                Contacts.SORT_KEY_PRIMARY + " ASC",
            )

        return cursor?.use { parseDisplayNameContacts(it, userId) } ?: emptyList()
    }

    private fun searchContactsByMimeTypes(
        query: String,
        mimetypes: List<MimeType>,
        matchAllRequestedMimetypes: Boolean,
        userId: Int,
    ): List<Contact> {

        // TODO(467326511#comment3): consider fix in the CP2 matcher and change the used URI
        val uri =
            ContactsContract.AUTHORITY_URI.buildUpon()
                .appendPath(CONTACTS_URI_PATH)
                .appendPath(MIMES_URI_PATH)
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

        val cursor =
            contentResolver.query(
                ContentProvider.maybeAddUserId(uri, userId),
                DISPLAY_NAME_FETCH_PROJECTION,
                null,
                null,
                null,
            )

        return cursor?.use { parseDisplayNameContacts(it, userId) } ?: emptyList()
    }

    private fun parseDisplayNameContacts(cursor: Cursor, userId: Int): List<DisplayNameContact> {
        val idIndex = cursor.getColumnIndex(Contacts._ID)
        val nameIndex = cursor.getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY)
        val profilePictureUriIndex = cursor.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI)
        val starredIndex = cursor.getColumnIndex(Contacts.STARRED)
        val lookupKeyIndex = cursor.getColumnIndex(Contacts.LOOKUP_KEY)

        return buildList {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name =
                    cursor.getString(nameIndex)?.takeIf { nameStr -> nameStr.isNotBlank() }
                        ?: context.getString(R.string.no_name_placeholder)
                val profilePictureUri = cursor.getString(profilePictureUriIndex)
                val isFavorite = cursor.getInt(starredIndex) == 1
                val lookupKey = cursor.getString(lookupKeyIndex)

                add(
                    DisplayNameContact(
                        id = id,
                        displayName = name,
                        profilePictureUri = uriStringWithUserId(profilePictureUri, userId),
                        isFavorite = isFavorite,
                        lookupKey = lookupKey,
                    )
                )
            }
        }
    }

    private fun searchPhones(query: String, userId: Int): List<Contact> {
        return searchWithFilter(
            query,
            Phone.CONTENT_FILTER_URI,
            PHONE_FILTER_PROJECTION,
            Phone.NUMBER,
            userId,
        ) { id, displayName, profilePictureUri, dataId, number ->
            PhoneContact(
                id = id,
                displayName = displayName,
                profilePictureUri = uriStringWithUserId(profilePictureUri, userId),
                isFavorite = false,
                phones = listOf(PhoneEntry(dataId, number)),
            )
        }
    }

    private fun searchEmails(query: String, userId: Int): List<Contact> {
        return searchWithFilter(
            query,
            Email.CONTENT_FILTER_URI,
            EMAIL_FILTER_PROJECTION,
            Email.ADDRESS,
            userId,
        ) { id, displayName, profilePictureUri, dataId, address ->
            EmailContact(
                id = id,
                displayName = displayName,
                profilePictureUri = uriStringWithUserId(profilePictureUri, userId),
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
    private fun searchDisplayNames(query: String, userId: Int): List<Contact> {
        val uri = Contacts.CONTENT_FILTER_URI.buildUpon().appendPath(query).build()
        return buildList {
            contentResolver
                .query(
                    ContentProvider.maybeAddUserId(uri, userId),
                    DISPLAY_NAME_FILTER_PROJECTION,
                    null,
                    null,
                    null,
                )
                ?.use { c ->
                    val idIndex = c.getColumnIndex(Contacts._ID)
                    val nameIndex = c.getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY)
                    val profilePictureUriIndex = c.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI)
                    val lookupKeyIndex = c.getColumnIndex(Contacts.LOOKUP_KEY)

                    while (c.moveToNext()) {
                        val contactId = c.getLong(idIndex)
                        val displayName = c.getString(nameIndex)
                        val profilePictureUri = c.getString(profilePictureUriIndex)
                        val lookupKey = c.getString(lookupKeyIndex)

                        if (!displayName.isNullOrBlank() && !lookupKey.isNullOrBlank()) {
                            add(
                                DisplayNameContact(
                                    id = contactId,
                                    displayName = displayName,
                                    profilePictureUri =
                                        uriStringWithUserId(profilePictureUri, userId),
                                    isFavorite = false,
                                    lookupKey = lookupKey,
                                )
                            )
                        }
                    }
                }
        }
    }

    private fun searchWithFilter(
        query: String,
        filterUri: Uri,
        projection: Array<String>,
        dataColumnName: String,
        userId: Int,
        parseContact:
            (
                id: Long,
                displayName: String,
                profilePictureUri: String?,
                dataId: Long,
                dataValue: String,
            ) -> Contact,
    ): List<Contact> {
        val uri = filterUri.buildUpon().appendPath(query).build()

        return buildList {
            contentResolver
                .query(ContentProvider.maybeAddUserId(uri, userId), projection, null, null, null)
                ?.use { c ->
                    val idIndex = c.getColumnIndex(Data.CONTACT_ID)
                    val nameIndex = c.getColumnIndex(Data.DISPLAY_NAME_PRIMARY)
                    val profilePictureUriIndex = c.getColumnIndex(Data.PHOTO_THUMBNAIL_URI)
                    val dataValueIndex = c.getColumnIndex(dataColumnName)
                    val dataIdIndex = c.getColumnIndex(Data._ID)

                    while (c.moveToNext()) {
                        val id = c.getLong(idIndex)
                        val name = c.getString(nameIndex)
                        val profilePictureUri = c.getString(profilePictureUriIndex)
                        val dataValue = c.getString(dataValueIndex)
                        val dataId = c.getLong(dataIdIndex)

                        if (!name.isNullOrBlank() && !dataValue.isNullOrBlank()) {
                            // TODO(b/451963918) Confirm if we return aggregated contacts or single
                            // data
                            // rows
                            add(parseContact(id, name, profilePictureUri, dataId, dataValue))
                        }
                    }
                }
        }
    }

    private fun uriStringWithUserId(photoUriStr: String?, userId: Int): String? =
        photoUriStr
            ?.takeIf { it.isNotBlank() }
            ?.let { ContentProvider.maybeAddUserId(it.toUri(), userId).toString() }
}
