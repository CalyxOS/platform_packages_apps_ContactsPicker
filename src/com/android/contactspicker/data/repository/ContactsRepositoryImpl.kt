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
import androidx.collection.mutableLongObjectMapOf
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
import com.android.contactspicker.viewmodel.ContactGroupingMetadata
import com.android.contactspicker.viewmodel.FALLBACK_SECTION_HEADER
import com.android.contactspicker.viewmodel.GroupedContactsData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.LinkedHashMap
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO(b/463918621): Define these inside CP2 as hidden APIs
private const val CONTACTS_URI_PATH = "contacts"
private const val MIMES_URI_PATH = "mimes"
private const val CONTACTS_DATA_FILTER_URI_PATH = "filter"

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
                Contacts.DISPLAY_NAME_SOURCE,
            )

        @VisibleForTesting
        internal val EMAIL_FETCH_PROJECTION =
            arrayOf(
                Email.CONTACT_ID,
                Email.DISPLAY_NAME_PRIMARY,
                Email.PHOTO_THUMBNAIL_URI,
                Email.STARRED,
                Email.LOOKUP_KEY,
                Email.ADDRESS,
                Email._ID,
                Email.TYPE,
                Email.LABEL,
                Email.DISPLAY_NAME_SOURCE,
            )
        @VisibleForTesting
        internal val PHONE_FETCH_PROJECTION =
            arrayOf(
                Phone.CONTACT_ID,
                Phone.DISPLAY_NAME_PRIMARY,
                Phone.PHOTO_THUMBNAIL_URI,
                Phone.STARRED,
                Phone.LOOKUP_KEY,
                Phone.NUMBER,
                Phone._ID,
                Phone.TYPE,
                Phone.LABEL,
                Phone.DISPLAY_NAME_SOURCE,
            )
        internal val PHONE_FILTER_PROJECTION =
            arrayOf(
                Phone.CONTACT_ID,
                Phone.DISPLAY_NAME_PRIMARY,
                Phone.PHOTO_THUMBNAIL_URI,
                Phone.LOOKUP_KEY,
                Phone.NUMBER,
                Phone._ID,
                Phone.DISPLAY_NAME_SOURCE,
            )
        internal val EMAIL_FILTER_PROJECTION =
            arrayOf(
                Email.CONTACT_ID,
                Email.DISPLAY_NAME_PRIMARY,
                Email.PHOTO_THUMBNAIL_URI,
                Email.LOOKUP_KEY,
                Email.ADDRESS,
                Email._ID,
                Email.DISPLAY_NAME_SOURCE,
            )
        internal val DISPLAY_NAME_FILTER_PROJECTION =
            arrayOf(
                Contacts._ID,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.PHOTO_THUMBNAIL_URI,
                Contacts.LOOKUP_KEY,
                Contacts.DISPLAY_NAME_SOURCE,
            )

        @VisibleForTesting
        internal fun createHeaderIterator(
            titles: Array<String>,
            counts: IntArray,
        ): Iterator<String> {
            return sequence {
                    val numSections = min(titles.size, counts.size)
                    for (i in 0 until numSections) {
                        repeat(counts[i]) { yield(titles[i]) }
                    }
                }
                .iterator()
        }
    }

    private class ContactBuilder(
        val displayName: String,
        val profilePictureUri: String?,
        val isFavorite: Boolean,
        val lookupKey: String,
        val displayNameSource: Int,
    ) {
        val emails = mutableListOf<EmailEntry>()
        val phones = mutableListOf<PhoneEntry>()

        fun toEmailContact(id: Long) =
            EmailContact(
                id = id,
                displayName = displayName,
                profilePictureUri = profilePictureUri,
                lookupKey = lookupKey,
                isFavorite = isFavorite,
                displayNameSource = displayNameSource,
                emails = emails,
            )

        fun toPhoneContact(id: Long) =
            PhoneContact(
                id = id,
                displayName = displayName,
                profilePictureUri = profilePictureUri,
                isFavorite = isFavorite,
                lookupKey = lookupKey,
                displayNameSource = displayNameSource,
                phones = phones,
            )
    }

    override suspend fun getContacts(
        queryMode: ContactsQueryMode,
        userId: Int,
    ): GroupedContactsData =
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

    private fun getEmailContacts(userId: Int): GroupedContactsData {
        val uri =
            Email.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            contentResolver.query(
                ContentProvider.maybeAddUserId(uri, userId),
                EMAIL_FETCH_PROJECTION,
                null, // No specific selection
                null, // No selection args
                Data.SORT_KEY_PRIMARY + " ASC",
            )

        return cursor?.use { c ->
            val contactIdToContactBuilders = LinkedHashMap<Long, ContactBuilder>()
            val contactIdToSectionMap = mutableLongObjectMapOf<String>()

            val (titles, counts) = getRawContactGroupingData(c)
            val headerIterator = createHeaderIterator(titles, counts)

            val idIndex = c.getColumnIndex(Email.CONTACT_ID)
            val nameIndex = c.getColumnIndex(Email.DISPLAY_NAME_PRIMARY)
            val profilePictureUriIndex = c.getColumnIndex(Email.PHOTO_THUMBNAIL_URI)
            val starredIndex = c.getColumnIndex(Email.STARRED)
            val lookupKeyIndex = c.getColumnIndex(Email.LOOKUP_KEY)
            val addressIndex = c.getColumnIndex(Email.ADDRESS)
            val dataIdIndex = c.getColumnIndex(Email._ID)
            val typeIndex = c.getColumnIndex(Email.TYPE)
            val labelIndex = c.getColumnIndex(Email.LABEL)
            val sourceIndex = c.getColumnIndex(Email.DISPLAY_NAME_SOURCE)

            while (c.moveToNext()) {
                val sectionHeader =
                    if (headerIterator.hasNext()) headerIterator.next() else FALLBACK_SECTION_HEADER

                val contactId = c.getLong(idIndex)
                val name = c.getString(nameIndex).displayNameOrNoNamePlaceholder(context)
                val address = c.getString(addressIndex)
                val lookupKey = c.getString(lookupKeyIndex)

                if (!address.isNullOrBlank() && !lookupKey.isNullOrBlank()) {
                    val profilePictureUri = c.getString(profilePictureUriIndex)
                    val isFavorite = c.getInt(starredIndex) == 1
                    val dataId = c.getLong(dataIdIndex)
                    val type = c.getInt(typeIndex)
                    val customLabel = c.getString(labelIndex)
                    val displayNameSource = c.getInt(sourceIndex)

                    val label = Email.getTypeLabel(context.resources, type, customLabel).toString()
                    val emailEntry = EmailEntry(dataId, address, label)

                    contactIdToSectionMap[contactId] = sectionHeader
                    contactIdToContactBuilders
                        .getOrPut(contactId) {
                            ContactBuilder(
                                displayName = name,
                                profilePictureUri = uriStringWithUserId(profilePictureUri, userId),
                                isFavorite = isFavorite,
                                lookupKey = lookupKey,
                                displayNameSource = displayNameSource,
                            )
                        }
                        .emails
                        .add(emailEntry)
                }
            }

            val aggregatedContacts =
                contactIdToContactBuilders.map { (contactId, builder) ->
                    builder.toEmailContact(contactId)
                }

            GroupedContactsData(
                contacts = aggregatedContacts,
                groupingMetadata = ContactGroupingMetadata(contactIdToSectionMap),
            )
        } ?: GroupedContactsData.EMPTY
    }

    private fun getPhoneContacts(userId: Int): GroupedContactsData {
        val uri =
            Phone.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            contentResolver.query(
                ContentProvider.maybeAddUserId(uri, userId),
                PHONE_FETCH_PROJECTION,
                null, // No specific selection
                null, // No selection args
                Data.SORT_KEY_PRIMARY + " ASC",
            )

        return cursor?.use { c ->
            val (titles, counts) = getRawContactGroupingData(c)
            val headerIterator = createHeaderIterator(titles, counts)
            val contactIdToContactBuilders = LinkedHashMap<Long, ContactBuilder>()
            val contactIdToSectionMap = mutableLongObjectMapOf<String>()

            val idIndex = c.getColumnIndex(Phone.CONTACT_ID)
            val nameIndex = c.getColumnIndex(Phone.DISPLAY_NAME_PRIMARY)
            val profilePictureUriIndex = c.getColumnIndex(Phone.PHOTO_THUMBNAIL_URI)
            val starredIndex = c.getColumnIndex(Phone.STARRED)
            val lookupKeyIndex = c.getColumnIndex(Phone.LOOKUP_KEY)
            val numberIndex = c.getColumnIndex(Phone.NUMBER)
            val dataIdIndex = c.getColumnIndex(Phone._ID)
            val typeIndex = c.getColumnIndex(Phone.TYPE)
            val labelIndex = c.getColumnIndex(Phone.LABEL)
            val sourceIndex = c.getColumnIndex(Phone.DISPLAY_NAME_SOURCE)

            while (c.moveToNext()) {
                val sectionHeader =
                    if (headerIterator.hasNext()) headerIterator.next() else FALLBACK_SECTION_HEADER

                val contactId = c.getLong(idIndex)
                val name = c.getString(nameIndex).displayNameOrNoNamePlaceholder(context)
                val number = c.getString(numberIndex)
                val lookupKey = c.getString(lookupKeyIndex)

                if (!number.isNullOrBlank() && !lookupKey.isNullOrBlank()) {
                    val profilePictureUri = c.getString(profilePictureUriIndex)
                    val isFavorite = c.getInt(starredIndex) == 1
                    val dataId = c.getLong(dataIdIndex)
                    val type = c.getInt(typeIndex)
                    val customLabel = c.getString(labelIndex)
                    val displayNameSource = c.getInt(sourceIndex)

                    val label = Phone.getTypeLabel(context.resources, type, customLabel).toString()
                    val phoneEntry = PhoneEntry(dataId, number, label)

                    contactIdToSectionMap[contactId] = sectionHeader
                    contactIdToContactBuilders
                        .getOrPut(contactId) {
                            ContactBuilder(
                                displayName = name,
                                profilePictureUri = uriStringWithUserId(profilePictureUri, userId),
                                isFavorite = isFavorite,
                                lookupKey = lookupKey,
                                displayNameSource = displayNameSource,
                            )
                        }
                        .phones
                        .add(phoneEntry)
                }
            }

            val aggregatedContacts =
                contactIdToContactBuilders.map { (contactId, builder) ->
                    builder.toPhoneContact(contactId)
                }
            GroupedContactsData(
                contacts = aggregatedContacts,
                groupingMetadata = ContactGroupingMetadata(contactIdToSectionMap),
            )
        } ?: GroupedContactsData.EMPTY
    }

    private fun getDisplayNameContacts(userId: Int): GroupedContactsData {
        val uri =
            Contacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()
        val cursor =
            contentResolver.query(
                ContentProvider.maybeAddUserId(uri, userId),
                DISPLAY_NAME_FETCH_PROJECTION,
                null, // No specific selection
                null, // No selection args
                Data.SORT_KEY_PRIMARY + " ASC",
            )

        return cursor?.use { parseDisplayNameContactsGrouped(it, userId) }
            ?: GroupedContactsData.EMPTY
    }

    private fun getContactsWithMimetypes(
        mimetypes: List<MimeType>,
        matchAllRequestedMimetypes: Boolean,
        userId: Int,
    ): GroupedContactsData {
        if (mimetypes.isEmpty()) {
            return GroupedContactsData.EMPTY
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
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build()

        val cursor =
            contentResolver.query(
                ContentProvider.maybeAddUserId(uri, userId),
                DISPLAY_NAME_FETCH_PROJECTION,
                null,
                null,
                Contacts.SORT_KEY_PRIMARY + " ASC",
            )

        return cursor?.use { parseDisplayNameContactsGrouped(it, userId) }
            ?: GroupedContactsData.EMPTY
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

    private fun parseDisplayNameContactsGrouped(cursor: Cursor, userId: Int): GroupedContactsData {
        val (titles, counts) = getRawContactGroupingData(cursor)
        val headerIterator = createHeaderIterator(titles, counts)
        val contactIdToSectionMap = mutableLongObjectMapOf<String>()

        val contacts =
            parseDisplayNameContacts(cursor, userId) { contactId ->
                val sectionHeader =
                    if (headerIterator.hasNext()) headerIterator.next() else FALLBACK_SECTION_HEADER
                contactIdToSectionMap[contactId] = sectionHeader
            }

        return GroupedContactsData(
            contacts = contacts,
            groupingMetadata = ContactGroupingMetadata(contactIdToSectionMap),
        )
    }

    private fun parseDisplayNameContacts(
        cursor: Cursor,
        userId: Int,
        onRowVisited: (contactId: Long) -> Unit = {},
    ): List<Contact> {
        val idIndex = cursor.getColumnIndex(Contacts._ID)
        val nameIndex = cursor.getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY)
        val profilePictureUriIndex = cursor.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI)
        val starredIndex = cursor.getColumnIndex(Contacts.STARRED)
        val lookupKeyIndex = cursor.getColumnIndex(Contacts.LOOKUP_KEY)
        val sourceIndex = cursor.getColumnIndex(Contacts.DISPLAY_NAME_SOURCE)

        return buildList {
            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idIndex)
                onRowVisited(contactId)

                val name = cursor.getString(nameIndex).displayNameOrNoNamePlaceholder(context)
                val profilePictureUri = cursor.getString(profilePictureUriIndex)
                val isFavorite = cursor.getInt(starredIndex) == 1
                val lookupKey = cursor.getString(lookupKeyIndex)
                val displayNameSource = cursor.getInt(sourceIndex)

                add(
                    DisplayNameContact(
                        id = contactId,
                        displayName = name,
                        profilePictureUri = uriStringWithUserId(profilePictureUri, userId),
                        isFavorite = isFavorite,
                        displayNameSource = displayNameSource,
                        lookupKey = lookupKey,
                    )
                )
            }
        }
    }

    private fun getRawContactGroupingData(cursor: Cursor): Pair<Array<String>, IntArray> {
        val titles = cursor.extras.getStringArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_TITLES)
        val counts = cursor.extras.getIntArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS)
        return (titles ?: emptyArray()) to (counts ?: intArrayOf())
    }

    private fun searchPhones(query: String, userId: Int): List<Contact> {
        return searchWithFilter(
            query,
            Phone.CONTENT_FILTER_URI,
            PHONE_FILTER_PROJECTION,
            Phone.NUMBER,
            userId,
        ) { id, displayName, profilePictureUri, lookupKey, displayNameSource, dataId, number ->
            PhoneContact(
                id = id,
                displayName = displayName,
                profilePictureUri = uriStringWithUserId(profilePictureUri, userId),
                isFavorite = false,
                lookupKey = lookupKey,
                displayNameSource = displayNameSource,
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
        ) { id, displayName, profilePictureUri, lookupKey, displayNameSource, dataId, address ->
            EmailContact(
                id = id,
                displayName = displayName,
                profilePictureUri = uriStringWithUserId(profilePictureUri, userId),
                isFavorite = false,
                lookupKey = lookupKey,
                displayNameSource = displayNameSource,
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
                    val sourceIndex = c.getColumnIndex(Contacts.DISPLAY_NAME_SOURCE)

                    while (c.moveToNext()) {
                        val contactId = c.getLong(idIndex)
                        val displayName =
                            c.getString(nameIndex).displayNameOrNoNamePlaceholder(context)
                        val profilePictureUri = c.getString(profilePictureUriIndex)
                        val lookupKey = c.getString(lookupKeyIndex)
                        val displayNameSource = c.getInt(sourceIndex)

                        if (!lookupKey.isNullOrBlank()) {
                            add(
                                DisplayNameContact(
                                    id = contactId,
                                    displayName = displayName,
                                    profilePictureUri =
                                        uriStringWithUserId(profilePictureUri, userId),
                                    isFavorite = false,
                                    displayNameSource = displayNameSource,
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
                lookupKey: String,
                displayNameSource: Int,
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
                    val lookupKeyIndex = c.getColumnIndex(Data.LOOKUP_KEY)
                    val dataValueIndex = c.getColumnIndex(dataColumnName)
                    val dataIdIndex = c.getColumnIndex(Data._ID)
                    val sourceIndex = c.getColumnIndex(Data.DISPLAY_NAME_SOURCE)

                    while (c.moveToNext()) {
                        val id = c.getLong(idIndex)
                        val name = c.getString(nameIndex).displayNameOrNoNamePlaceholder(context)
                        val profilePictureUri = c.getString(profilePictureUriIndex)
                        val lookupKey = c.getString(lookupKeyIndex)
                        val dataValue = c.getString(dataValueIndex)
                        val dataId = c.getLong(dataIdIndex)
                        val displayNameSource = c.getInt(sourceIndex)

                        if (!dataValue.isNullOrBlank() && !lookupKey.isNullOrBlank()) {
                            add(
                                parseContact(
                                    id,
                                    name,
                                    profilePictureUri,
                                    lookupKey,
                                    displayNameSource,
                                    dataId,
                                    dataValue,
                                )
                            )
                        }
                    }
                }
        }
    }

    private fun uriStringWithUserId(photoUriStr: String?, userId: Int): String? =
        photoUriStr
            ?.takeIf { it.isNotBlank() }
            ?.let { ContentProvider.maybeAddUserId(it.toUri(), userId).toString() }

    /**
     * Returns the string if it is not null and not blank. Otherwise, returns the default
     * placeholder string for a missing name "(No name)".
     *
     * @param context The context to access string resources.
     */
    internal fun String?.displayNameOrNoNamePlaceholder(context: Context): String =
        this?.takeIf { it.isNotBlank() } ?: context.getString(R.string.no_name_placeholder)
}
