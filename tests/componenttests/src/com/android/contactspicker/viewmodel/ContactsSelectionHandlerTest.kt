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

package com.android.contactspicker.viewmodel

import android.content.ContentUris
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.data.model.totalElementCount
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsSelectionHandlerTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private lateinit var handler: ContactsSelectionHandler
    private val emittedEvents = mutableListOf<SnackbarEvent>()

    @Before
    fun setUp() {
        emittedEvents.clear()
    }

    private fun initHandler(isMultiSelect: Boolean, limit: Int = 100) {
        handler =
            ContactsSelectionHandler(
                isMultiSelectEnabled = isMultiSelect,
                maxSelectionLimit = limit,
                eventListener = { event -> emittedEvents.add(event) },
            )
    }

    @Test
    fun toggleContactSelection_multiSelect_addsToSelection() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT

        handler.toggleContactSelection(contact)

        assertThat(handler.selectedContacts.value.containsKey(contact.id)).isTrue()
    }

    @Test
    fun toggleContactSelection_multiSelect_multiEntryContact_selectsAllEntries() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT

        handler.toggleContactSelection(contact)

        val selection = handler.selectedContacts.value
        assertThat(selection.containsKey(contact.id)).isTrue()
        // Verify all phone IDs are present
        assertThat(selection[contact.id]).containsExactlyElementsIn(contact.phones.map { it.id })
    }

    @Test
    fun toggleContactSelection_multiSelect_removesFromSelection() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT

        handler.toggleContactSelection(contact) // Select
        handler.toggleContactSelection(contact) // Deselect

        assertThat(handler.selectedContacts.value.isEmpty()).isTrue()
    }

    @Test
    fun toggleContactSelection_singleSelect_replacesPreviousSelection() {
        initHandler(isMultiSelect = false)
        val contacts = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST

        // Select first
        handler.toggleContactSelection(contacts[0])
        assertThat(handler.selectedContacts.value.containsKey(contacts[0].id)).isTrue()

        // Select second
        handler.toggleContactSelection(contacts[1])

        val selection = handler.selectedContacts.value
        assertThat(selection.containsKey(contacts[1].id)).isTrue()
        assertThat(selection.containsKey(contacts[0].id)).isFalse() // Previous removed
        assertThat(selection.size).isEqualTo(1)
    }

    @Test
    fun toggleContactSelection_singleSelect_multiEntryContact_selectsOnlyFirstEntry() {
        initHandler(isMultiSelect = false)
        val contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT

        handler.toggleContactSelection(contact)

        val selection = handler.selectedContacts.value
        assertThat(selection[contact.id]).containsExactly(contact.phones.first().id)
    }

    @Test
    fun toggleEntrySelection_singleSelect_replacesPreviousSelection() {
        initHandler(isMultiSelect = false)
        val contact1 = ContactTestDataFactory.GENERIC_PHONE_CONTACT
        val contact2 = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT

        handler.toggleContactSelection(contact1)

        // Select entry from second contact
        val entryToSelect = contact2.phones.first()
        handler.toggleEntrySelection(contact2.id, entryToSelect.id)

        val selection = handler.selectedContacts.value
        assertThat(selection.containsKey(contact2.id)).isTrue()
        assertThat(selection.containsKey(contact1.id)).isFalse()
    }

    @Test
    fun toggleEntrySelection_removesContactId_whenLastEntryIsDeselected() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        val entry = contact.emails.first()

        handler.toggleEntrySelection(contact.id, entry.id)
        assertThat(handler.selectedContacts.value.containsKey(contact.id)).isTrue()

        handler.toggleEntrySelection(contact.id, entry.id) // Deselect
        assertThat(handler.selectedContacts.value.containsKey(contact.id)).isFalse()
    }

    @Test
    fun toggleContactSelection_whenLimitExceeded_rejectsAndSendsEvent() {
        val limit = 2
        initHandler(isMultiSelect = true, limit = limit)
        val contacts = ContactTestDataFactory.createContactList(limit + 1)

        // Fill up to limit
        handler.toggleContactSelection(contacts[0])
        handler.toggleContactSelection(contacts[1])

        assertThat(handler.selectedContacts.value.totalElementCount()).isEqualTo(limit)
        assertThat(emittedEvents).isEmpty()

        // Try to add one more
        handler.toggleContactSelection(contacts[2])

        assertThat(handler.selectedContacts.value.totalElementCount())
            .isEqualTo(limit) // Count unchanged
        assertThat(emittedEvents).hasSize(1)
        assertThat((emittedEvents.first() as SnackbarEvent.ShowSelectionLimitReached).limit)
            .isEqualTo(limit)
    }

    @Test
    fun toggleEntrySelection_whenLimitExceeded_rejectsAndSendsEvent() {
        val limit = 2
        initHandler(isMultiSelect = true, limit = limit)
        val contact = ContactTestDataFactory.createPhoneContact(1, "Multi", 3)

        // Fill up to limit
        handler.toggleEntrySelection(contact.id, contact.phones[0].id)
        handler.toggleEntrySelection(contact.id, contact.phones[1].id)

        assertThat(handler.selectedContacts.value.totalElementCount()).isEqualTo(limit)
        assertThat(emittedEvents).isEmpty()

        // Try to add one more entry
        handler.toggleEntrySelection(contact.id, contact.phones[2].id)

        assertThat(handler.selectedContacts.value.totalElementCount()).isEqualTo(limit)
        assertThat(emittedEvents).hasSize(1)
        assertThat((emittedEvents.first() as SnackbarEvent.ShowSelectionLimitReached).limit)
            .isEqualTo(limit)
    }

    @Test
    fun resolveSelectedUris_mapsIdsToUrisCorrectly() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        val entry = contact.emails.first()

        handler.toggleEntrySelection(contact.id, entry.id)

        val uris = handler.resolveSelectedUris(listOf(contact))

        val expectedUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, entry.id)
        assertThat(uris).containsExactly(expectedUri)
    }

    @Test
    fun clearSelection_emptiesMap() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        handler.toggleContactSelection(contact)

        handler.clearSelection()

        assertThat(handler.selectedContacts.value.isEmpty()).isTrue()
    }

    @Test
    fun getSelectedIds_returnsFlattenedListOfIds() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        handler.toggleContactSelection(contact)

        val ids = handler.getSelectedIds()

        val expectedIds = contact.phones.map { it.id }
        assertThat(ids).containsExactlyElementsIn(expectedIds)
    }
}
