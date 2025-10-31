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
import android.content.Intent
import android.content.flags.Flags
import android.os.Bundle
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.collection.longObjectMapOf
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.SearchState
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.fakes.FakeContactsRepository
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
        val testContacts = listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT)
        fakeRepository.setInitialContacts(testContacts)
        val collectedStates = mutableListOf<ContactsUiState>()
        val job = launch { viewModel.uiState.toList(collectedStates) }

        viewModel.processIntent(
            intentAction = Intent.ACTION_PICK,
            intentType = Phone.CONTENT_TYPE,
            intentExtras = null,
            callingAppName = null,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(collectedStates).hasSize(2)
        assertThat(collectedStates[0] is ContactsListState.Loading).isTrue()
        assertThat(collectedStates[1] is ContactsListState.Success).isTrue()

        job.cancel()
    }

    @Test
    fun processIntent_whenRepositorySucceeds_setsSuccessState() = runTest {
        val displayNameContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        loadViewModelWithInitialContacts(listOf(displayNameContact))

        val successState = viewModel.uiState.value as ContactsListState.Success
        assertThat(successState.availableContacts).containsExactly(displayNameContact)
        assertThat(successState.selectedContacts.isEmpty()).isTrue()
    }

    @Test
    fun processIntent_withInvalidAction_setsErrorState() = runTest {
        val testException = IllegalArgumentException("Unsupported action")
        fakeRepository.setException(testException)

        viewModel.processIntent(
            intentAction = "INVALID_ACTION",
            intentType = null,
            intentExtras = null,
            callingAppName = null,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val errorState = viewModel.uiState.value as ContactsListState.Error
        assertThat(errorState.message).isEqualTo("Unsupported action")
    }

    @Test
    fun processIntent_withoutMultiSelectExtra_setsSingleSelectModeInState() = runTest {
        loadViewModelWithInitialContacts(
            listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT),
            intentExtras = null,
        )
        assertThat(viewModel.currentSuccessState.isMultiSelectEnabled).isFalse()
    }

    @Test
    fun processIntent_withMultiSelectExtraFalse_setsSingleSelectModeInState() = runTest {
        loadViewModelWithInitialContacts(
            listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT),
            intentExtras = buildIntentExtras(isMultiSelectEnabled = false),
        )
        assertThat(viewModel.currentSuccessState.isMultiSelectEnabled).isFalse()
    }

    @Test
    fun processIntent_withMultiSelectExtraTrue_setsMultiSelectModeInState() = runTest {
        loadViewModelWithInitialContacts(
            listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT),
            intentExtras = buildIntentExtras(isMultiSelectEnabled = true),
        )
        assertThat(viewModel.currentSuccessState.isMultiSelectEnabled).isTrue()
    }

    @Test
    fun getRequestedMimeTypesForIntent_actionPickWithValidType_returnsTypeList() {
        val intentAction = Intent.ACTION_PICK
        val intentType = Phone.CONTENT_TYPE
        val result = viewModel.getRequestedMimeTypesForIntent(intentAction, intentType)
        assertThat(result).containsExactly(intentType)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getRequestedMimeTypesForIntent_actionPickWithNullType_throwsException() {
        val intentAction = Intent.ACTION_PICK
        val intentType: String? = null
        viewModel.getRequestedMimeTypesForIntent(intentAction, intentType)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getRequestedMimeTypesForIntent_unsupportedAction_throwsException() {
        val intentAction = Intent.ACTION_VIEW
        val intentType = Phone.CONTENT_TYPE
        viewModel.getRequestedMimeTypesForIntent(intentAction, intentType)
    }

    @Test
    fun toggleContactSelection_selectsDisplayNameContact() {
        val displayNameContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        loadViewModelWithInitialContacts(listOf(displayNameContact))

        viewModel.toggleContactSelection(displayNameContact)

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.containsKey(displayNameContact.id)).isTrue()
        assertThat(selection[displayNameContact.id]).containsExactly(displayNameContact.id)
    }

    @Test
    fun toggleContactSelection_deselectsDisplayNameContact() {
        val displayNameContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
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
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        loadViewModelWithInitialContactsInMultiSelectMode(listOf(multiPhoneContact))

        viewModel.toggleContactSelection(multiPhoneContact)

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.containsKey(multiPhoneContact.id)).isTrue()
        assertThat(multiPhoneContact.isFullySelected(selection[multiPhoneContact.id])).isTrue()
    }

    @Test
    fun toggleContactSelection_deselectsAllEntriesForMultiPhoneContact() {
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        loadViewModelWithInitialContactsInMultiSelectMode(listOf(multiPhoneContact))

        // Select first
        viewModel.toggleContactSelection(multiPhoneContact)
        // Then deselect
        viewModel.toggleContactSelection(multiPhoneContact)

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.isEmpty()).isTrue()
    }

    @Test
    fun toggleContactSelection_inSingleSelectForMultiEntry_selectsOnlyFirstEntry() {
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        loadViewModelWithInitialContacts(listOf(multiPhoneContact))
        assertThat(viewModel.currentSuccessState.isMultiSelectEnabled).isFalse()

        // Toggle contact
        viewModel.toggleContactSelection(multiPhoneContact)

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.count()).isEqualTo(1)
        assertThat(selection.containsKey(multiPhoneContact.id)).isTrue()
        assertThat(selection[multiPhoneContact.id])
            .containsExactly(multiPhoneContact.phones.first().id)
    }

    @Test
    fun toggleContactSelection_singleSelect_replacesPreviousSelection() {
        val displayNameContactList = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST
        loadViewModelWithInitialContacts(displayNameContactList)
        assertThat(viewModel.currentSuccessState.isMultiSelectEnabled).isFalse()

        // Select the first contact
        viewModel.toggleContactSelection(displayNameContactList[0])
        var selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.count()).isEqualTo(1)
        assertThat(selection.containsKey(displayNameContactList.first().id)).isTrue()

        // Select the second contact
        viewModel.toggleContactSelection(displayNameContactList[1])
        selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.count()).isEqualTo(1)
        assertThat(selection.containsKey(displayNameContactList[1].id)).isTrue()
        assertThat(selection.containsKey(displayNameContactList[0].id))
            .isFalse() // Previous is gone
    }

    @Test
    fun toggleEntrySelection_selectsOneEntry() {
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        loadViewModelWithInitialContactsInMultiSelectMode(listOf(multiPhoneContact))

        val entryToSelect = multiPhoneContact.phones.first()
        viewModel.toggleEntrySelection(multiPhoneContact.id, entryToSelect.id)

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.containsKey(multiPhoneContact.id)).isTrue()
        assertThat(selection[multiPhoneContact.id]).containsExactly(entryToSelect.id)
    }

    @Test
    fun toggleEntrySelection_singleSelect_replacesPreviousSelection() {
        val singlePhoneContact = ContactTestDataFactory.GENERIC_PHONE_CONTACT
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        loadViewModelWithInitialContacts(listOf(singlePhoneContact, multiPhoneContact))
        assertThat(viewModel.currentSuccessState.isMultiSelectEnabled).isFalse()

        // Select the first contact
        viewModel.toggleContactSelection(singlePhoneContact)
        var selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.count()).isEqualTo(1)
        assertThat(selection.containsKey(singlePhoneContact.id)).isTrue()

        // Select an entry from the second contact
        val entryToSelect = multiPhoneContact.phones.first()
        viewModel.toggleEntrySelection(multiPhoneContact.id, entryToSelect.id)

        selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.count()).isEqualTo(1)
        assertThat(selection.containsKey(multiPhoneContact.id)).isTrue()
        assertThat(selection.containsKey(singlePhoneContact.id)).isFalse() // Previous is gone
    }

    @Test
    fun toggleEntrySelection_deselectsOneEntry() {
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        loadViewModelWithInitialContactsInMultiSelectMode(listOf(multiPhoneContact))

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
        val singleEmailContact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        loadViewModelWithInitialContactsInMultiSelectMode(listOf(singleEmailContact))

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
        val displayNameContactList = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST
        loadViewModelWithInitialContactsInMultiSelectMode(displayNameContactList)

        viewModel.toggleContactSelection(displayNameContactList[0])
        viewModel.toggleContactSelection(displayNameContactList[1])
        var selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.count()).isEqualTo(2)

        viewModel.clearSelection()

        selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.isEmpty()).isTrue()
    }

    @Test
    fun prepareSelectionResult_withNoSelection_returnsEmptyList() {
        loadViewModelWithInitialContacts(
            listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT)
        )

        val uris = viewModel.prepareSelectionResult()

        assertThat(uris).isEmpty()
    }

    @Test
    fun prepareSelectionResult_withDisplayNameContact_returnsContactLookupUri() {
        val displayNameContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        loadViewModelWithInitialContacts(listOf(displayNameContact))
        viewModel.toggleContactSelection(displayNameContact)

        val uris = viewModel.prepareSelectionResult()

        val expectedUri =
            ContactsContract.Contacts.getLookupUri(
                displayNameContact.id,
                displayNameContact.lookupKey,
            )
        assertThat(uris).containsExactly(expectedUri)
    }

    @Test
    fun prepareSelectionResult_withSingleEmailEntry_returnsDataUri() {
        val singleEmailContact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        loadViewModelWithInitialContacts(listOf(singleEmailContact))
        val entry = singleEmailContact.emails.first()
        viewModel.toggleEntrySelection(singleEmailContact.id, entry.id)

        val uris = viewModel.prepareSelectionResult()

        val expectedUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, entry.id)
        assertThat(uris).containsExactly(expectedUri)
    }

    @Test
    fun prepareSelectionResult_withMultiplePhoneEntries_returnsDataUris() {
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        // Must be in multi-select mode
        loadViewModelWithInitialContacts(listOf(multiPhoneContact), buildIntentExtras(true))
        viewModel.toggleContactSelection(multiPhoneContact) // Selects all entries

        val uris = viewModel.prepareSelectionResult()

        assertThat(uris.size).isEqualTo(multiPhoneContact.phones.size)
        multiPhoneContact.phones.forEach { phoneEntry ->
            val expectedUri =
                ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, phoneEntry.id)
            assertThat(uris).contains(expectedUri)
        }
    }

    @Test
    fun prepareSelectionResult_withMixedSelection_returnsAllUris() {
        val displayNameContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        // Must be in multi-select mode
        loadViewModelWithInitialContacts(
            listOf(displayNameContact, multiPhoneContact),
            buildIntentExtras(true),
        )

        // Select the DisplayNameContact
        viewModel.toggleContactSelection(displayNameContact)
        // Select the first phone entry from the MultiPhoneContact
        val entry = multiPhoneContact.phones.first()
        viewModel.toggleEntrySelection(multiPhoneContact.id, entry.id)

        val uris = viewModel.prepareSelectionResult()

        val expectedDisplayNameUri =
            ContactsContract.Contacts.getLookupUri(
                displayNameContact.id,
                displayNameContact.lookupKey,
            )
        val expectedPhoneUri =
            ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, entry.id)

        assertThat(uris).containsExactly(expectedDisplayNameUri, expectedPhoneUri)
    }

    @Test
    fun prepareSelectionResult_inSingleSelect_withMultipleEntriesSelected_returnsOnlyOneUri() {
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        // This simulates the defensive logic in toggleContactSelection
        loadViewModelWithInitialContacts(listOf(multiPhoneContact), buildIntentExtras(false))
        viewModel.toggleContactSelection(
            multiPhoneContact
        ) // This should only select the first entry

        val uris = viewModel.prepareSelectionResult()
        val firstEntry = multiPhoneContact.phones.first()
        val expectedUri =
            ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, firstEntry.id)

        assertThat(uris).containsExactly(expectedUri)
    }

    @Test
    fun onSearchQueryChanged_debouncesSearch() = runTest {
        val searchQuery = "test"
        val searchResult =
            listOf(ContactTestDataFactory.createDisplayNameContact(10L, "Test Result 1"))

        fakeRepository.setSearchResults(searchQuery, searchResult)
        loadViewModelWithInitialContacts(emptyList())

        val collectedStates = mutableListOf<ContactsUiState>()
        val job = launch { viewModel.uiState.toList(collectedStates) }

        viewModel.onSearchQueryChanged(searchQuery)
        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS - 100)
        // Repository search for searchQuery should not have been called yet
        assertThat(fakeRepository.searchInvocationsCountForQuery(searchQuery)).isEqualTo(0)

        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS)
        // Repository search for searchQuery should have been called now
        assertThat(fakeRepository.searchInvocationsCountForQuery(searchQuery)).isEqualTo(1)
        assertThat(collectedStates.last())
            .isEqualTo(SearchState.Success(searchQuery, searchResult, longObjectMapOf()))
        job.cancel()
    }

    @Test
    fun onSearchQueryChanged_searchError_setsErrorState() = runTest {
        val searchQuery = "error"
        val exception = RuntimeException("An unexpected error occurred during search.")
        fakeRepository.setSearchException(searchQuery, exception)

        loadViewModelWithInitialContacts(emptyList())

        viewModel.onSearchQueryChanged(searchQuery)
        testDispatcher.scheduler.advanceUntilIdle()

        val errorState = viewModel.uiState.value as SearchState.Error
        assertThat(errorState.message).isEqualTo("An unexpected error occurred during search.")
    }

    @Test
    fun onSearchQueryChanged_transitionsFromListSuccessToSearchSuccess() = runTest {
        val searchQuery = "query"
        val searchResults =
            listOf(ContactTestDataFactory.createDisplayNameContact(10L, "Query Result"))
        loadViewModelWithInitialContacts(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST)
        fakeRepository.setSearchResults(searchQuery, searchResults)

        // Collect states in a list
        val collectedStates = mutableListOf<ContactsUiState>()
        val collectJob = launch(testDispatcher) { viewModel.uiState.toList(collectedStates) }

        // Trigger the search
        viewModel.onSearchQueryChanged(searchQuery)
        testDispatcher.scheduler.advanceUntilIdle() // Let the Loading state be set

        // Advance past the debounce
        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle() // Let the search complete

        collectJob.cancel()

        // Assert the states
        assertThat(collectedStates)
            .containsExactly(
                ContactsListState.Success(
                    ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST,
                    longObjectMapOf(),
                    false,
                    callingAppName = null,
                    requestedMimeTypes = listOf(Phone.CONTENT_TYPE),
                ), // Initial state after processIntent
                SearchState.Success(
                    searchQuery,
                    searchResults,
                    longObjectMapOf(),
                ), // State after search completes
            )
            .inOrder()
    }

    @Test
    fun onSearchQueryChanged_emptySearchResults_setsSuccessWithEmptyList() = runTest {
        val searchQuery = "no_match"

        fakeRepository.setSearchResults(searchQuery, emptyList())

        loadViewModelWithInitialContacts(emptyList())

        viewModel.onSearchQueryChanged(searchQuery)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value)
            .isEqualTo(SearchState.Success(searchQuery, emptyList(), longObjectMapOf()))
    }

    @Test
    fun onSearchQueryChanged_blankQuery_transitionsToEmptySearchState() = runTest {
        loadViewModelWithInitialContacts(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST)

        // Start with a non-blank search
        viewModel.onSearchQueryChanged("test")
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(SearchState.Success::class.java)

        // Call with blank query
        viewModel.onSearchQueryChanged("")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(SearchState.Success::class.java)
        val searchState = state as SearchState.Success
        assertThat(searchState.query).isEmpty()
        assertThat(searchState.searchResults).isEmpty()
        assertThat(searchState.selectedContacts.isEmpty()).isTrue()
    }

    @Test
    fun onSearchQueryChanged_nonBlankQuery_transitionsToSearchStateSuccess() = runTest {
        val searchQuery = "query"
        val searchResults =
            listOf(ContactTestDataFactory.createDisplayNameContact(10L, "Query Result"))

        loadViewModelWithInitialContacts(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST)

        fakeRepository.setSearchResults(searchQuery, searchResults)

        viewModel.onSearchQueryChanged(searchQuery)
        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value)
            .isEqualTo(SearchState.Success(searchQuery, searchResults, longObjectMapOf()))
    }

    @Test
    fun onSearchQueryChanged_selectionPreservedAcrossSearches() = runTest {
        val query1 = "test"
        val results1 = listOf(ContactTestDataFactory.createDisplayNameContact(1L, "Test Contact"))
        val query2 = "another"
        val results2 =
            listOf(ContactTestDataFactory.createDisplayNameContact(2L, "Another Contact"))

        fakeRepository.setSearchResults(query1, results1)
        fakeRepository.setSearchResults(query2, results2)

        loadViewModelWithInitialContacts(emptyList())

        // First search and select
        viewModel.onSearchQueryChanged(query1)
        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleContactSelection(results1[0])
        val selection1 = (viewModel.uiState.value as SearchState.Success).selectedContacts
        assertThat(selection1.containsKey(1L)).isTrue()

        // Second search
        viewModel.onSearchQueryChanged(query2)
        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        val state2 = viewModel.uiState.value as SearchState.Success
        assertThat(state2.query).isEqualTo(query2)
        assertThat(state2.searchResults).isEqualTo(results2)
        // Selection should be preserved
        assertThat(state2.selectedContacts).isEqualTo(selection1)
        assertThat(state2.selectedContacts.containsKey(1L)).isTrue()
    }

    @Test
    fun onSearchQueryChanged_preservesSelectionFromContactsListState() = runTest {
        val initialContacts = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST
        val contactToSelect = initialContacts[0]

        loadViewModelWithInitialContacts(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST)

        viewModel.toggleContactSelection(contactToSelect)
        val initialSelection =
            (viewModel.uiState.value as ContactsListState.Success).selectedContacts
        assertThat(initialSelection.containsKey(contactToSelect.id)).isTrue()

        // Perform a search
        val searchQuery = "query"
        val searchResults = listOf(initialContacts[1])
        fakeRepository.setSearchResults(searchQuery, searchResults)
        viewModel.onSearchQueryChanged(searchQuery)
        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify selection is still present in SearchState
        val searchState = viewModel.uiState.value as SearchState.Success
        assertThat(searchState.selectedContacts).isEqualTo(initialSelection)
        assertThat(searchState.selectedContacts.containsKey(contactToSelect.id)).isTrue()
    }

    @Test
    fun exitSearch_revertsToContactsListState_withSelectionPreserved() = runTest {
        val initialContacts = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST
        val searchQuery = "a"
        val searchResults = listOf(initialContacts[0], initialContacts[1])
        fakeRepository.setSearchResults(searchQuery, searchResults)

        loadViewModelWithInitialContacts(initialContacts)

        // Perform a search
        viewModel.onSearchQueryChanged(searchQuery)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(SearchState.Success::class.java)

        // Select an item in search results
        val contactToSelect = searchResults[0]
        viewModel.toggleContactSelection(contactToSelect)
        val selectionAfterSearch = (viewModel.uiState.value as SearchState.Success).selectedContacts
        assertThat(selectionAfterSearch.containsKey(contactToSelect.id)).isTrue()

        // Exit search
        viewModel.exitSearch()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify state reverted to ContactsListState.Success
        assertThat(viewModel.uiState.value).isInstanceOf(ContactsListState.Success::class.java)
        val finalListState = viewModel.uiState.value as ContactsListState.Success

        // Verify contacts list is the initial list
        assertThat(finalListState.availableContacts).isEqualTo(initialContacts)

        // Verify selection is preserved
        assertThat(finalListState.selectedContacts).isEqualTo(selectionAfterSearch)
        assertThat(finalListState.selectedContacts.containsKey(contactToSelect.id)).isTrue()
    }

    /**
     * Helper function to put the ViewModel into a Success state with a predefined list of contacts.
     */
    private fun loadViewModelWithInitialContacts(
        contacts: List<Contact>,
        intentExtras: Bundle? = null,
    ) {
        fakeRepository.setInitialContacts(contacts)
        viewModel.processIntent(
            intentAction = Intent.ACTION_PICK,
            intentType = Phone.CONTENT_TYPE,
            intentExtras = intentExtras,
            callingAppName = null,
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun loadViewModelWithInitialContactsInMultiSelectMode(contacts: List<Contact>) {
        loadViewModelWithInitialContacts(contacts, buildIntentExtras(true))
    }

    private fun buildIntentExtras(isMultiSelectEnabled: Boolean): Bundle =
        Bundle().apply { putBoolean(Intent.EXTRA_ALLOW_MULTIPLE, isMultiSelectEnabled) }

    /**
     * A helper property to safely access the `Success` state for assertions. Fails the test if the
     * current state is not `Success`.
     */
    private val ContactsViewModel.currentSuccessState: ContactsListState.Success
        get() {
            val state = this.uiState.value
            assertThat(state).isInstanceOf(ContactsListState.Success::class.java)
            return state as ContactsListState.Success
        }
}
