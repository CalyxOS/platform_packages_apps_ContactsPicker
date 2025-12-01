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

import androidx.collection.LongObjectMap
import androidx.collection.MutableLongObjectMap
import androidx.collection.buildLongObjectMap
import androidx.collection.longObjectMapOf

/**
 * A map representing the current selection, where the key is the contact ID and the value is a set
 * of selected entry IDs. For a [DisplayNameContact] that has no entries, its own contact.id is
 * used.
 */
typealias ContactsSelection = LongObjectMap<Set<Long>>

/** Returns an empty [ContactsSelection]. */
fun emptyContactsSelection(): ContactsSelection = longObjectMapOf()

/** Returns [ContactsSelection] containing the given [contactId] and [entryIds]. */
fun contactsSelectionOf(contactId: Long, entryIds: Set<Long>): ContactsSelection =
    longObjectMapOf(contactId, entryIds)

fun buildContactsSelection(
    builderAction: MutableLongObjectMap<Set<Long>>.() -> Unit
): ContactsSelection = buildLongObjectMap(builderAction)

/** Calculates the total number of selected contacts. */
internal fun ContactsSelection.totalElementCount(): Int {
    var count = 0
    forEachValue { value -> count += value.size }
    return count
}
