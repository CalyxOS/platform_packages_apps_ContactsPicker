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
import com.android.contactspicker.data.model.PhoneContact
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
        val projection = arrayOf(Email.CONTACT_ID, Email.DISPLAY_NAME_PRIMARY, Email.ADDRESS)
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

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex)
                val address = it.getString(addressIndex)
                if (!name.isNullOrBlank() && !address.isNullOrBlank()) {
                    if (contacts.containsKey(id)) {
                        val existingContact = contacts[id]!!
                        // TODO(b/441477119): change to a list of email addresses
                        contacts[id] =
                            existingContact.copy(email = existingContact.email + ", " + address)
                    } else {
                        contacts[id] = EmailContact(id, name, address)
                    }
                }
            }
        }
        return contacts.values.toList()
    }

    private fun fetchPhoneContacts(): List<Contact> {
        val contacts = mutableMapOf<Long, PhoneContact>()
        val projection = arrayOf(Phone.CONTACT_ID, Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER)
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

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)
                if (!name.isNullOrBlank() && !number.isNullOrBlank()) {
                    if (contacts.containsKey(id)) {
                        val existingContact = contacts[id]!!
                        // TODO(b/441477119): change to a list of phone numbers
                        contacts[id] =
                            existingContact.copy(phone = existingContact.phone + ", " + number)
                    } else {
                        contacts[id] = PhoneContact(id, name, number)
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
