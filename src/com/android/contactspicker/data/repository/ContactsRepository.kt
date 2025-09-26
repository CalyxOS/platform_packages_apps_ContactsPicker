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

import android.content.Intent
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.PhoneContact
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepository @Inject constructor() {

    /** Fetches contacts from the data source based on the intent action and type. */
    suspend fun fetchContacts(intentAction: String?, intentType: String?): List<Contact> {
        return when (intentAction) {
            Intent.ACTION_PICK ->
                when (intentType) {
                    Email.CONTENT_ITEM_TYPE,
                    Email.CONTENT_TYPE -> fetchEmailContacts()
                    Phone.CONTENT_ITEM_TYPE,
                    Phone.CONTENT_TYPE -> fetchPhoneContacts()
                    Contacts.CONTENT_TYPE,
                    Contacts.CONTENT_ITEM_TYPE -> fetchDisplayNameContacts()
                    else -> throw IllegalArgumentException("Unsupported intent type: $intentType")
                }
            else -> throw IllegalArgumentException("Unsupported intent action: $intentAction")
        }
    }

    // TODO(b/442966559): Implement real CP2 queries in these functions
    private fun fetchEmailContacts(): List<Contact> {
        return listOf(
            EmailContact(1, "Alice Wonderland", "alice@wonderland.com"),
            EmailContact(2, "Bob The Builder", "bobthebuilder@example.com"),
            EmailContact(3, "Charlie Chaplin", "charlie@chaplin.com"),
            EmailContact(4, "David Copperfield", "david@copperfield.com"),
            EmailContact(5, "Emily Dickinson", "emily@dickinson.com"),
            EmailContact(6, "Frank Sinatra", "frank@sinatra.com"),
            EmailContact(7, "Grace Hopper", "grace@hopper.com"),
            EmailContact(8, "Henry Ford", "henry@ford.com"),
            EmailContact(9, "Ivy Lee", "ivy@lee.com"),
            EmailContact(10, "Jack London", "jack@london.com"),
        )
    }

    private fun fetchPhoneContacts(): List<Contact> {
        return listOf(
            PhoneContact(1, "Alice Wonderland", "123-456-7890"),
            PhoneContact(2, "Bob The Builder", "987-654-3210"),
            PhoneContact(3, "Charlie Chaplin", "555-555-5555"),
            PhoneContact(4, "David Copperfield", "111-222-3333"),
            PhoneContact(5, "Emily Dickinson", "444-444-4444"),
            PhoneContact(6, "Frank Sinatra", "777-777-7777"),
            PhoneContact(7, "Grace Hopper", "888-888-8888"),
            PhoneContact(8, "Henry Ford", "999-999-9999"),
            PhoneContact(9, "Ivy Lee", "333-333-3333"),
            PhoneContact(10, "Jack London", "666-666-6666"),
        )
    }

    private fun fetchDisplayNameContacts(): List<Contact> {
        return listOf(
            DisplayNameContact(1, "Alice Wonderland"),
            DisplayNameContact(2, "Bob The Builder"),
            DisplayNameContact(3, "Charlie Chaplin"),
            DisplayNameContact(4, "David Copperfield"),
            DisplayNameContact(5, "Emily Dickinson"),
            DisplayNameContact(6, "Frank Sinatra"),
            DisplayNameContact(7, "Grace Hopper"),
            DisplayNameContact(8, "Henry Ford"),
            DisplayNameContact(9, "Ivy Lee"),
            DisplayNameContact(10, "Jack London"),
        )
    }
}
