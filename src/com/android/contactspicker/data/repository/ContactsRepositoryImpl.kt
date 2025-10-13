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
import android.content.Intent
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.EmailEntry
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.data.model.PhoneEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ContactsRepositoryImpl
@Inject
constructor(@param:ApplicationContext private val context: Context) : ContactsRepository {

    private val contentResolver = context.contentResolver

    /** Fetches contacts from the data source based on the intent action and type. */
    override suspend fun fetchContacts(intentAction: String?, intentType: String?): List<Contact> =
        withContext(Dispatchers.IO) {
            when (intentAction) {
                Intent.ACTION_PICK ->
                    when (intentType) {
                        Email.CONTENT_ITEM_TYPE,
                        Email.CONTENT_TYPE -> fetchEmailContacts()
                        Phone.CONTENT_ITEM_TYPE,
                        Phone.CONTENT_TYPE -> fetchPhoneContacts()
                        Contacts.CONTENT_TYPE,
                        Contacts.CONTENT_ITEM_TYPE -> fetchDisplayNameContacts()
                        else ->
                            throw IllegalArgumentException("Unsupported intent type: $intentType")
                    }
                else -> throw IllegalArgumentException("Unsupported intent action: $intentAction")
            }
        }

    private fun fetchEmailContacts(): List<Contact> {
        val contacts = mutableMapOf<Long, EmailContact>()
        val projection =
            arrayOf(
                Email.CONTACT_ID,
                Email.DISPLAY_NAME_PRIMARY,
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
            val addressIndex = it.getColumnIndex(Email.ADDRESS)
            val dataIdIndex = it.getColumnIndex(Email._ID)
            val typeIndex = it.getColumnIndex(Email.TYPE)
            val labelIndex = it.getColumnIndex(Email.LABEL)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex)
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
                        contacts[id] = EmailContact(id, name, listOf(emailEntry))
                    }
                }
            }
        }
        return contacts.values.toList()
    }

    private fun fetchPhoneContacts(): List<Contact> {
        val contacts = mutableMapOf<Long, PhoneContact>()
        val projection =
            arrayOf(
                Phone.CONTACT_ID,
                Phone.DISPLAY_NAME_PRIMARY,
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
            val numberIndex = it.getColumnIndex(Phone.NUMBER)
            val dataIdIndex = it.getColumnIndex(Phone._ID)
            val typeIndex = it.getColumnIndex(Phone.TYPE)
            val labelIndex = it.getColumnIndex(Phone.LABEL)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex)
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
                        contacts[id] = PhoneContact(id, name, listOf(phoneEntry))
                    }
                }
            }
        }
        return contacts.values.toList()
    }

    private fun fetchDisplayNameContacts(): List<Contact> {
        val contacts = mutableListOf<DisplayNameContact>()
        val projection = arrayOf(Contacts._ID, Contacts.DISPLAY_NAME_PRIMARY)
        val selection = "${Contacts.DISPLAY_NAME_PRIMARY} IS NOT NULL"

        val cursor =
            contentResolver.query(
                Contacts.CONTENT_URI,
                projection,
                selection,
                null, // No selection args
                Data.SORT_KEY_PRIMARY + " ASC",
            )

        cursor?.use {
            val idIndex = it.getColumnIndex(Contacts._ID)
            val nameIndex = it.getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex)
                if (name != null) {
                    contacts.add(DisplayNameContact(id, name))
                }
            }
        }
        return contacts
    }
}
