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
}

/**
 * A [Contact] that contains only basic information.
 *
 * @param id A unique identifier for the contact.
 * @param displayName The name of the contact. Must not be blank.
 * @throws IllegalArgumentException if [displayName] is blank.
 */
data class DisplayNameContact(override val id: Long, override val displayName: String) : Contact() {
    init {
        require(displayName.isNotBlank()) { "Display name must not be blank." }
    }
}

/**
 * A [Contact] that includes a non-blank phone number.
 *
 * @param id A unique identifier for the contact.
 * @param displayName The name of the contact. Must not be blank.
 * @param phone The phone number of the contact. Must not be blank
 * @throws IllegalArgumentException if [displayName] or [phone] is blank.
 */
data class PhoneContact(
    override val id: Long,
    override val displayName: String,
    val phone: String,
) : Contact() {
    init {
        require(displayName.isNotBlank()) {
            "A PhoneContact must be created with a non blank display name."
        }
        require(phone.isNotBlank()) { "A PhoneContact must be created with a phone number." }
    }
}

/**
 * A [Contact] that includes a non-blank email address.
 *
 * @param id A unique identifier for the contact.
 * @param displayName The name of the contact. Must not be blank.
 * @param email The email address of the contact. Must not be blank.
 * @throws IllegalArgumentException if [displayName] or [email] is blank.
 */
data class EmailContact(
    override val id: Long,
    override val displayName: String,
    val email: String,
) : Contact() {
    init {
        require(displayName.isNotBlank()) {
            "A PhoneContact must be created with a non blank display name."
        }
        require(email.isNotBlank()) { "An EmailContact must be created with an email address." }
    }
}
