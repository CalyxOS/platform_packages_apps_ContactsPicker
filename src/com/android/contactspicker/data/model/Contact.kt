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
package com.android.contactspicker.data.model

/**
 * Represents a single contact entity.
 *
 * This sealed class ensures that a [Contact] can only be one of the well-defined subtypes:
 * - [DisplayNameContact]: Contains only the basic information of a contact.
 * - [PhoneContact]: Contains base info plus a non-empty list of phone numbers.
 * - [EmailContact]: Contains base info plus a non-empty list of email addresses.
 */
sealed class Contact {
    /** A unique, stable identifier for the contact. */
    abstract val id: Long

    /** The name of the contact, suitable for display. */
    abstract val displayName: String

    /**
     * Returns true if all entries of the [Contact] are present in the [selectedEntries] set.
     *
     * For [DisplayNameContact], this means the contact ID is in the selected set. For
     * [EmailContact] and [PhoneContact], it means all associated entry IDs are in the set.
     */
    fun isFullySelected(selectedEntries: Set<Long>?): Boolean =
        if (selectedEntries.isNullOrEmpty()) {
            false
        } else
            when (this) {
                is DisplayNameContact -> selectedEntries.isNotEmpty()
                is EmailContact -> selectedEntries.size == emails.size
                is PhoneContact -> selectedEntries.size == phones.size
            }
}

/**
 * A [Contact] that contains only basic information.
 *
 * @param id A unique identifier for the contact.
 * @param displayName The name of the contact. Must not be blank.
 * @param lookupKey A unique, stable identifier for the contact.
 * @throws IllegalArgumentException if [displayName] is blank.
 */
data class DisplayNameContact(
    override val id: Long,
    override val displayName: String,
    val lookupKey: String? = null,
) : Contact() {
    init {
        require(displayName.isNotBlank()) { "Display name must not be blank." }
    }
}

/**
 * A [Contact] that includes a non-blank phone number.
 *
 * @param id A unique identifier for the contact.
 * @param displayName The name of the contact. Must not be blank.
 * @param phones The non-empty list of phone entries.
 * @throws IllegalArgumentException if [displayName] is blank or [phones] is empty.
 */
data class PhoneContact(
    override val id: Long,
    override val displayName: String,
    val phones: List<PhoneEntry>,
) : Contact() {
    init {
        require(displayName.isNotBlank()) {
            "A PhoneContact must be created with a non blank display name."
        }
        require(phones.isNotEmpty()) {
            "A PhoneContact must be created with at least one phone number."
        }
    }
}

/**
 * A [Contact] that includes a non-blank email address.
 *
 * @param id A unique identifier for the contact.
 * @param displayName The name of the contact. Must not be blank.
 * @param emails The non-empty list of email entries.
 * @throws IllegalArgumentException if [displayName] is blank or [emails] is empty.
 */
data class EmailContact(
    override val id: Long,
    override val displayName: String,
    val emails: List<EmailEntry>,
) : Contact() {
    init {
        require(displayName.isNotBlank()) {
            "A EmailContact must be created with a non blank display name."
        }
        require(emails.isNotEmpty()) {
            "An EmailContact must be created with at least one email address."
        }
    }
}

/**
 * Represents a single phone entry with its unique ID corresponding to the ID in the Data._ID table,
 * number, and a user-readable label (e.g., "Home", "Work").
 */
data class PhoneEntry(val id: Long, val number: String, val label: String? = null)

/**
 * Represents a single email entry with its unique ID corresponding to the ID in the Data._ID table,
 * address, and a user-readable label (e.g., "Home", "Work").
 */
data class EmailEntry(val id: Long, val address: String, val label: String? = null)
