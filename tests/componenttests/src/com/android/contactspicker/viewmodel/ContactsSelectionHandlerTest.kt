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
import com.android.contactspicker.data.model.SelectionSource
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

    private val SELECTION_SOURCE_MAIN_LIST = SelectionSource.MAIN_LIST
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

        handler.toggleContactSelection(contact, SELECTION_SOURCE_MAIN_LIST)

        assertThat(handler.selectedContacts.value.containsKey(contact.id)).isTrue()
    }

    @Test
    fun toggleContactSelection_multiSelect_multiEntryContact_selectsAllEntries() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT

        handler.toggleContactSelection(contact, SELECTION_SOURCE_MAIN_LIST)

        val selection = handler.selectedContacts.value
        assertThat(selection.containsKey(contact.id)).isTrue()
        // Verify all phone IDs are present
        assertThat(selection[contact.id]).containsExactlyElementsIn(contact.phones.map { it.id })
    }

    @Test
    fun toggleContactSelection_multiSelect_removesFromSelection() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT

        handler.toggleContactSelection(contact, SELECTION_SOURCE_MAIN_LIST) // Select
        handler.toggleContactSelection(contact, SELECTION_SOURCE_MAIN_LIST) // Deselect

        assertThat(handler.selectedContacts.value.isEmpty()).isTrue()
    }

    @Test
    fun toggleContactSelection_singleSelect_replacesPreviousSelection() {
        initHandler(isMultiSelect = false)
        val contacts = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST

        // Select first
        handler.toggleContactSelection(contacts[0], SELECTION_SOURCE_MAIN_LIST)
        assertThat(handler.selectedContacts.value.containsKey(contacts[0].id)).isTrue()

        // Select second
        handler.toggleContactSelection(contacts[1], SELECTION_SOURCE_MAIN_LIST)

        val selection = handler.selectedContacts.value
        assertThat(selection.containsKey(contacts[1].id)).isTrue()
        assertThat(selection.containsKey(contacts[0].id)).isFalse() // Previous removed
        assertThat(selection.size).isEqualTo(1)
    }

    @Test
    fun toggleContactSelection_singleSelect_multiEntryContact_selectsOnlyFirstEntry() {
        initHandler(isMultiSelect = false)
        val contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT

        handler.toggleContactSelection(contact, SELECTION_SOURCE_MAIN_LIST)

        val selection = handler.selectedContacts.value
        assertThat(selection[contact.id]).containsExactly(contact.phones.first().id)
    }

    @Test
    fun toggleEntrySelection_singleSelect_replacesPreviousSelection() {
        initHandler(isMultiSelect = false)
        val contact1 = ContactTestDataFactory.GENERIC_PHONE_CONTACT
        val contact2 = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT

        handler.toggleContactSelection(contact1, SELECTION_SOURCE_MAIN_LIST)

        // Select entry from second contact
        val entryToSelect = contact2.phones.first()
        handler.toggleEntrySelection(contact2.id, entryToSelect.id, SELECTION_SOURCE_MAIN_LIST)

        val selection = handler.selectedContacts.value
        assertThat(selection.containsKey(contact2.id)).isTrue()
        assertThat(selection.containsKey(contact1.id)).isFalse()
    }

    @Test
    fun toggleEntrySelection_removesContactId_whenLastEntryIsDeselected() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        val entry = contact.emails.first()

        handler.toggleEntrySelection(contact.id, entry.id, SELECTION_SOURCE_MAIN_LIST)
        assertThat(handler.selectedContacts.value.containsKey(contact.id)).isTrue()

        handler.toggleEntrySelection(contact.id, entry.id, SELECTION_SOURCE_MAIN_LIST) // Deselect
        assertThat(handler.selectedContacts.value.containsKey(contact.id)).isFalse()
    }

    @Test
    fun toggleContactSelection_whenLimitExceeded_rejectsAndSendsEvent() {
        val limit = 2
        initHandler(isMultiSelect = true, limit = limit)
        val contacts = ContactTestDataFactory.createContactList(limit + 1)

        // Fill up to limit
        handler.toggleContactSelection(contacts[0], SELECTION_SOURCE_MAIN_LIST)
        handler.toggleContactSelection(contacts[1], SELECTION_SOURCE_MAIN_LIST)

        assertThat(handler.selectedContacts.value.totalElementCount()).isEqualTo(limit)
        assertThat(emittedEvents).isEmpty()

        // Try to add one more
        handler.toggleContactSelection(contacts[2], SELECTION_SOURCE_MAIN_LIST)

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
        handler.toggleEntrySelection(contact.id, contact.phones[0].id, SELECTION_SOURCE_MAIN_LIST)
        handler.toggleEntrySelection(contact.id, contact.phones[1].id, SELECTION_SOURCE_MAIN_LIST)

        assertThat(handler.selectedContacts.value.totalElementCount()).isEqualTo(limit)
        assertThat(emittedEvents).isEmpty()

        // Try to add one more entry
        handler.toggleEntrySelection(contact.id, contact.phones[2].id, SELECTION_SOURCE_MAIN_LIST)

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

        handler.toggleEntrySelection(contact.id, entry.id, SELECTION_SOURCE_MAIN_LIST)

        val uris = handler.resolveSelectedUris(listOf(contact))

        val expectedUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, entry.id)
        assertThat(uris).containsExactly(expectedUri)
    }

    @Test
    fun toggleContactSelection_singleSelect_deselectsWhenTappedTwice() {
        initHandler(isMultiSelect = false)
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT

        // select
        handler.toggleContactSelection(contact, SELECTION_SOURCE_MAIN_LIST)
        assertThat(handler.selectedContacts.value.containsKey(contact.id)).isTrue()
        // deselect
        handler.toggleContactSelection(contact, SELECTION_SOURCE_MAIN_LIST)

        assertThat(handler.selectedContacts.value.containsKey(contact.id)).isFalse()
        assertThat(handler.selectedContacts.value.isEmpty()).isTrue()
    }

    @Test
    fun toggleEntrySelection_singleSelect_deselectsWhenTappedTwice() {
        initHandler(isMultiSelect = false)
        val contact = ContactTestDataFactory.GENERIC_PHONE_CONTACT
        val entry = contact.phones.first()

        // select entry
        handler.toggleEntrySelection(contact.id, entry.id, SELECTION_SOURCE_MAIN_LIST)
        assertThat(handler.selectedContacts.value[contact.id]).containsExactly(entry.id)

        // deselect entry
        handler.toggleEntrySelection(contact.id, entry.id, SELECTION_SOURCE_MAIN_LIST)

        assertThat(handler.selectedContacts.value.containsKey(contact.id)).isFalse()
        assertThat(handler.selectedContacts.value.isEmpty()).isTrue()
    }

    @Test
    fun toggleContactSelection_singleSelect_multiEntryContact_canBeDeselected() {
        initHandler(isMultiSelect = false)

        val contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT

        handler.toggleEntrySelection(
            contact.id,
            contact.phones.first().id,
            SELECTION_SOURCE_MAIN_LIST,
        )
        assertThat(handler.selectedContacts.value.containsKey(contact.id)).isTrue()

        // Select whole contact to deselect
        handler.toggleContactSelection(contact, SELECTION_SOURCE_MAIN_LIST)

        assertThat(handler.selectedContacts.value.containsKey(contact.id)).isFalse()
        assertThat(handler.selectedContacts.value.isEmpty()).isTrue()
    }

    @Test
    fun clearSelection_emptiesMap() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        handler.toggleContactSelection(contact, SELECTION_SOURCE_MAIN_LIST)

        handler.clearSelection()

        assertThat(handler.selectedContacts.value.isEmpty()).isTrue()
    }

    @Test
    fun getSelectedIds_returnsFlattenedListOfIds() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        handler.toggleContactSelection(contact, SELECTION_SOURCE_MAIN_LIST)

        val ids = handler.getSelectedIds()

        val expectedIds = contact.phones.map { it.id }
        assertThat(ids).containsExactlyElementsIn(expectedIds)
    }

    @Test
    fun toggleContactSelection_tracksSourceCorrectly() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT

        handler.toggleContactSelection(contact, SelectionSource.SEARCH)

        assertThat(handler.wasSelectedFrom(SelectionSource.SEARCH)).isTrue()
        assertThat(handler.wasSelectedFrom(SelectionSource.MAIN_LIST)).isFalse()
    }

    @Test
    fun toggleContactSelection_deselectingRemovesSource() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT

        // Select
        handler.toggleContactSelection(contact, SelectionSource.FAVORITES)
        // Deselect
        handler.toggleContactSelection(contact, SelectionSource.MAIN_LIST)

        // Source should be wiped
        assertThat(handler.wasSelectedFrom(SelectionSource.FAVORITES)).isFalse()
    }

    @Test
    fun toggleContactSelection_singleSelect_replacesSelectionAndClearsOldSource() {
        initHandler(isMultiSelect = false)
        val contacts = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST

        // select contact from search
        handler.toggleContactSelection(contacts[0], SelectionSource.SEARCH)
        assertThat(handler.wasSelectedFrom(SelectionSource.SEARCH)).isTrue()

        // select second contact from fav
        handler.toggleContactSelection(contacts[1], SelectionSource.FAVORITES)

        // search source should be cleared
        assertThat(handler.wasSelectedFrom(SelectionSource.SEARCH)).isFalse()
        assertThat(handler.wasSelectedFrom(SelectionSource.FAVORITES)).isTrue()
    }

    @Test
    fun toggleContactSelection_doesNotReplaceExistingInterestingSource() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        val firstPhone = contact.phones[0]

        // select one entry from search
        handler.toggleEntrySelection(contact.id, firstPhone.id, SelectionSource.SEARCH)

        // select the whole contact from the main list
        handler.toggleContactSelection(contact, SelectionSource.MAIN_LIST)

        // the search source for that specific entry should be preserved
        assertThat(handler.wasSelectedFrom(SelectionSource.SEARCH)).isTrue()
        assertThat(handler.wasSelectedFrom(SelectionSource.MAIN_LIST)).isTrue()
    }

    @Test
    fun toggleEntrySelection_singleSelect_deselectingClearsSource() {
        initHandler(isMultiSelect = false)
        val contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        val entryToToggle = contact.phones.first()

        // select entry from search
        handler.toggleEntrySelection(contact.id, entryToToggle.id, SelectionSource.SEARCH)
        assertThat(handler.selectedContacts.value.containsKey(contact.id)).isTrue()
        assertThat(handler.wasSelectedFrom(SelectionSource.SEARCH)).isTrue()

        // deselect
        handler.toggleEntrySelection(contact.id, entryToToggle.id, SelectionSource.MAIN_LIST)

        assertThat(handler.selectedContacts.value.isEmpty()).isTrue()
        assertThat(handler.wasSelectedFrom(SelectionSource.SEARCH)).isFalse()
        assertThat(handler.wasSelectedFrom(SelectionSource.MAIN_LIST)).isFalse()
    }

    @Test
    fun toggleEntrySelection_singleSelect_replacesSelectionAndClearsOldSource() {
        initHandler(isMultiSelect = false)
        val contact1 = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        val contact2 = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT

        // select entry from search
        handler.toggleEntrySelection(
            contact1.id,
            contact1.phones.first().id,
            SelectionSource.SEARCH,
        )
        assertThat(handler.wasSelectedFrom(SelectionSource.SEARCH)).isTrue()

        // select a different entry from fav
        handler.toggleEntrySelection(
            contact2.id,
            contact2.emails.first().id,
            SelectionSource.FAVORITES,
        )

        // search source should be cleared and only favorites be tracked
        assertThat(handler.wasSelectedFrom(SelectionSource.SEARCH)).isFalse()
        assertThat(handler.wasSelectedFrom(SelectionSource.FAVORITES)).isTrue()
    }

    @Test
    fun clearSelection_wipesSourceTracking() {
        initHandler(isMultiSelect = true)
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT

        handler.toggleContactSelection(contact, SelectionSource.SEARCH)
        handler.clearSelection()

        // assert the selection source is empty
        SelectionSource.entries.forEach { assertThat(handler.wasSelectedFrom(it)).isFalse() }
    }
}
