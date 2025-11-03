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

package com.android.contactspicker.testdata

import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.EmailEntry
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.data.model.PhoneEntry

/** A central factory for creating test [Contact] objects for tests. */
object ContactTestDataFactory {

    // --- Re-usable static instances ---

    val GENERIC_DISPLAY_NAME_CONTACT: DisplayNameContact =
        createDisplayNameContact(id = 1, displayName = "Alice Wonderland")

    val GENERIC_PHONE_CONTACT: PhoneContact =
        createPhoneContact(id = 2, displayName = "Bob the Builder")

    val GENERIC_EMAIL_CONTACT: EmailContact =
        createEmailContact(id = 3, displayName = "Charlie Chaplin")

    val GENERIC_MULTI_PHONE_CONTACT: PhoneContact =
        createPhoneContact(id = 4, displayName = "David Copperfield", phoneCount = 3)

    val GENERIC_MULTI_EMAIL_CONTACT: EmailContact =
        createEmailContact(id = 5, displayName = "Emily Dickinson", emailCount = 3)

    val GENERIC_DISPLAY_NAME_CONTACT_LIST =
        listOf(
            createDisplayNameContact(6, "Frank Sinatra"),
            createDisplayNameContact(7, "Grace Hopper"),
            createDisplayNameContact(8, "Henry Ford"),
            createDisplayNameContact(9, "Ivy Lee"),
        )

    // --- Factory Methods for custom contacts ---

    /** Creates a [DisplayNameContact] with standard defaults. */
    fun createDisplayNameContact(
        id: Long,
        displayName: String,
        isFavorite: Boolean = false,
        lookupKey: String = "key_$id",
        profilePictureUri: String? = null,
    ): DisplayNameContact {
        return DisplayNameContact(
            id = id,
            displayName = displayName,
            isFavorite = isFavorite,
            profilePictureUri = profilePictureUri,
            lookupKey = lookupKey,
        )
    }

    /** Creates a [PhoneContact] with standard defaults. */
    fun createPhoneContact(
        id: Long,
        displayName: String,
        isFavorite: Boolean = false,
        phones: List<PhoneEntry>,
        profilePictureUri: String? = null,
    ): PhoneContact {
        return PhoneContact(
            id = id,
            displayName = displayName,
            isFavorite = isFavorite,
            phones = phones,
            profilePictureUri = profilePictureUri,
        )
    }

    /** Creates a [EmailContact] with standard defaults. */
    fun createEmailContact(
        id: Long,
        displayName: String,
        isFavorite: Boolean = false,
        emails: List<EmailEntry>,
        profilePictureUri: String? = null,
    ): EmailContact {
        return EmailContact(
            id = id,
            displayName = displayName,
            isFavorite = isFavorite,
            emails = emails,
            profilePictureUri = profilePictureUri,
        )
    }

    /** Creates a [PhoneContact] with multiple phone entries. */
    fun createPhoneContact(
        id: Long,
        displayName: String,
        phoneCount: Int = 1,
        profilePictureUri: String? = null,
    ): PhoneContact {
        val phones =
            (1..phoneCount).map {
                val dataId = (id * 10) + it
                PhoneEntry(dataId, "555-${1000 + dataId}", if (it == 1) "Home" else "Work$it")
            }
        return createPhoneContact(
            id = id,
            displayName = displayName,
            phones = phones,
            profilePictureUri = profilePictureUri,
        )
    }

    /** Creates an [EmailContact] with multiple email entries. */
    fun createEmailContact(
        id: Long,
        displayName: String,
        emailCount: Int = 1,
        profilePictureUri: String? = null,
    ): EmailContact {
        val emails =
            (1..emailCount).map {
                val dataId = (id * 10) + it
                val address =
                    if (it == 1) "${displayName.replace(" ", "")}@home.com"
                    else "${displayName.replace(" ", "")}_$it@work.com"
                EmailEntry(dataId, address, if (it == 1) "Home" else "Work$it")
            }
        return createEmailContact(
            id = id,
            displayName = displayName,
            emails = emails,
            profilePictureUri = profilePictureUri,
        )
    }

    /**
     * Generates a list of [DisplayNameContact]s for testing lists.
     *
     * @param count The number of contacts to create.
     * @param namePrefix A prefix for each contact's name (e.g., "User").
     */
    fun createContactList(count: Int, namePrefix: String = "Contact"): List<Contact> {
        return (0 until count).map {
            val id = (it + 1).toLong()
            createDisplayNameContact(id = id, displayName = "$namePrefix $id")
        }
    }
}
