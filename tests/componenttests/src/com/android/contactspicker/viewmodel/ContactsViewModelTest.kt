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

import android.content.Intent
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.EmailEntry
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.data.model.PhoneEntry
import com.android.contactspicker.fakes.FakeContactsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
class ContactsViewModelTest {

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeContactsRepository
    private lateinit var viewModel: ContactsViewModel

    // Test Data
    private val displayNameContact = DisplayNameContact(id = 1, displayName = "Just Name")
    private val singleEmailContact =
        EmailContact(
            id = 2,
            displayName = "Single Email",
            emails = listOf(EmailEntry(id = 20, address = "one@email.com", label = "Home")),
        )
    private val multiPhoneContact =
        PhoneContact(
            id = 3,
            displayName = "Multi Phone",
            phones =
                listOf(
                    PhoneEntry(id = 30, number = "111-111-1111", label = "Home"),
                    PhoneEntry(id = 31, number = "222-222-2222", label = "Work"),
                ),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeContactsRepository()
        viewModel = ContactsViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun processIntent_setsLoadingThenSuccessState() = runTest {
        val testContacts = listOf(DisplayNameContact(1L, "Test"))
        fakeRepository.setInitialContacts(testContacts)
        val collectedStates = mutableListOf<ContactsUiState>()
        val job = launch { viewModel.uiState.toList(collectedStates) }

        viewModel.processIntent(Intent.ACTION_PICK, Phone.CONTENT_TYPE)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(collectedStates).hasSize(2)
        assertThat(collectedStates[0] is ContactsUiState.Loading).isTrue()
        assertThat(collectedStates[1] is ContactsUiState.Success).isTrue()

        job.cancel()
    }

    @Test
    fun processIntent_whenRepositorySucceeds_setsSuccessState() = runTest {
        loadViewModelWithInitialContacts(listOf(displayNameContact))

        val successState = viewModel.currentSuccessState
        assertThat(successState.availableContacts).containsExactly(displayNameContact)
        assertThat(successState.selectedContacts.isEmpty()).isTrue()
    }

    @Test
    fun processIntent_withInvalidAction_setsErrorState() = runTest {
        val testException = IllegalArgumentException("Unsupported action")
        fakeRepository.setException(testException)

        viewModel.processIntent("INVALID_ACTION", null)
        testDispatcher.scheduler.advanceUntilIdle()

        val errorState = viewModel.uiState.value as ContactsUiState.Error
        assertThat(errorState.message).isEqualTo("Unsupported action")
    }

    @Test
    fun toggleContactSelection_selectsDisplayNameContact() {
        loadViewModelWithInitialContacts(listOf(displayNameContact))

        viewModel.toggleContactSelection(displayNameContact)

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.containsKey(displayNameContact.id)).isTrue()
        assertThat(selection[displayNameContact.id]).containsExactly(displayNameContact.id)
    }

    @Test
    fun toggleContactSelection_deselectsDisplayNameContact() {
        loadViewModelWithInitialContacts(listOf(displayNameContact))

        // Select first
        viewModel.toggleContactSelection(displayNameContact)
        // Then deselect
        viewModel.toggleContactSelection(displayNameContact)

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.isEmpty()).isTrue()
    }

    @Test
    fun toggleContactSelection_selectsAllEntriesForMultiPhoneContact() {
        loadViewModelWithInitialContacts(listOf(multiPhoneContact))

        viewModel.toggleContactSelection(multiPhoneContact)

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.containsKey(multiPhoneContact.id)).isTrue()
        assertThat(selection[multiPhoneContact.id]).containsExactly(30L, 31L)
    }

    @Test
    fun toggleContactSelection_deselectsAllEntriesForMultiPhoneContact() {
        loadViewModelWithInitialContacts(listOf(multiPhoneContact))

        // Select first
        viewModel.toggleContactSelection(multiPhoneContact)
        // Then deselect
        viewModel.toggleContactSelection(multiPhoneContact)

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.isEmpty()).isTrue()
    }

    @Test
    fun toggleEntrySelection_selectsOneEntry() {
        loadViewModelWithInitialContacts(listOf(multiPhoneContact))

        val entryToSelect = multiPhoneContact.phones.first()
        viewModel.toggleEntrySelection(multiPhoneContact.id, entryToSelect.id)

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.containsKey(multiPhoneContact.id)).isTrue()
        assertThat(selection[multiPhoneContact.id]).containsExactly(entryToSelect.id)
    }

    @Test
    fun toggleEntrySelection_deselectsOneEntry() {
        loadViewModelWithInitialContacts(listOf(multiPhoneContact))

        val entryToToggle = multiPhoneContact.phones.first()
        // Select first
        viewModel.toggleEntrySelection(multiPhoneContact.id, entryToToggle.id)
        // Then deselect
        viewModel.toggleEntrySelection(multiPhoneContact.id, entryToToggle.id)

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.isEmpty()).isTrue()
    }

    @Test
    fun toggleEntrySelection_removesContactId_whenLastEntryIsDeselected() {
        loadViewModelWithInitialContacts(listOf(singleEmailContact))

        val entryToToggle = singleEmailContact.emails.first()
        // Select the only entry
        viewModel.toggleEntrySelection(singleEmailContact.id, entryToToggle.id)
        var selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.isNotEmpty()).isTrue()
        // Deselect the only entry
        viewModel.toggleEntrySelection(singleEmailContact.id, entryToToggle.id)

        selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.containsKey(singleEmailContact.id)).isFalse()
    }

    @Test
    fun clearSelection_emptiesTheSelectionMap() {
        loadViewModelWithInitialContacts(listOf(displayNameContact, multiPhoneContact))

        viewModel.toggleContactSelection(multiPhoneContact)
        viewModel.toggleContactSelection(displayNameContact)
        var selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.count()).isEqualTo(2)

        viewModel.clearSelection()

        selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.isEmpty()).isTrue()
    }

    /**
     * Helper function to put the ViewModel into a Success state with a predefined list of contacts.
     */
    private fun loadViewModelWithInitialContacts(contacts: List<Contact>) {
        fakeRepository.setInitialContacts(contacts)
        viewModel.processIntent(Intent.ACTION_PICK, null)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    /**
     * A helper property to safely access the `Success` state for assertions. Fails the test if the
     * current state is not `Success`.
     */
    private val ContactsViewModel.currentSuccessState: ContactsUiState.Success
        get() {
            val state = this.uiState.value
            assertThat(state).isInstanceOf(ContactsUiState.Success::class.java)
            return state as ContactsUiState.Success
        }
}
