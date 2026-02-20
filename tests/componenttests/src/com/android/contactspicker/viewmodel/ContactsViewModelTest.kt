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

import android.content.ContentProvider
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.flags.Flags
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsPickerSessionContract
import androidx.test.core.app.ApplicationProvider
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.ContactsPreviewState
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.Flags.FLAG_ENABLE_ACTION_PICK_TAKEOVER_IN_DROIDFOOD
import com.android.contactspicker.R
import com.android.contactspicker.SearchState
import com.android.contactspicker.config.ContactsPickerAction
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.MimeType
import com.android.contactspicker.data.model.PausedProfileInfo
import com.android.contactspicker.data.model.PausedReason
import com.android.contactspicker.data.model.PickerUserState
import com.android.contactspicker.data.model.SwitchableProfileInfo
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.model.UserType
import com.android.contactspicker.data.model.emptyContactsSelection
import com.android.contactspicker.data.repository.UserRepository
import com.android.contactspicker.fakes.FakeContactsPickerSessionProviderRepository
import com.android.contactspicker.fakes.FakeContactsRepository
import com.android.contactspicker.fakes.FakePrivacyBannerRepository
import com.android.contactspicker.logging.ContactsPickerLogger
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.google.common.truth.Truth.assertThat
import dagger.Lazy
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
class ContactsViewModelTest {

    companion object {
        private const val TEST_CONTACT_LOOKUP_KEY = "key"
        private const val TEST_CONTACT_DISPLAY_NAME = "Test Name"
        private const val PAUSED_WORK_APPS_TITLE = "Work apps are paused"
        private const val TEST_APP_NAME = "TestApp"
        private const val TEST_PACKAGE_NAME = "com.test.app"
        private const val TEST_CALLING_UID = 12345
        private const val TEST_APP_ID = 12345
        private const val USER_ID_PERSONAL = 0
        private const val USER_ID_WORK = 10
        private const val USER_ID_SECONDARY = 12

        private val PERSONAL_PROFILE =
            UserProfile(
                userId = USER_ID_PERSONAL,
                userIdToQueryContacts = USER_ID_PERSONAL,
                userType = UserType.PERSONAL,
                switchableInfo = SwitchableProfileInfo("Personal", null),
            )

        private val WORK_PROFILE =
            UserProfile(
                userId = USER_ID_WORK,
                userIdToQueryContacts = USER_ID_WORK,
                userType = UserType.WORK,
                switchableInfo = SwitchableProfileInfo("Work", null),
            )

        private val PAUSED_WORK_PROFILE =
            UserProfile(
                userId = USER_ID_WORK,
                userIdToQueryContacts = USER_ID_WORK,
                userType = UserType.WORK,
                switchableInfo = SwitchableProfileInfo("Work", null),
                pausedInfo = PausedProfileInfo(PausedReason.QUIET_MODE),
            )

        private val BLOCKED_WORK_PROFILE =
            UserProfile(
                userId = USER_ID_WORK,
                userIdToQueryContacts = USER_ID_WORK,
                userType = UserType.WORK,
                switchableInfo = SwitchableProfileInfo("Work", null),
                pausedInfo = PausedProfileInfo(PausedReason.MANAGED_PROFILE_CONTACTS_BLOCKED),
            )
    }

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    private val testDispatcher = StandardTestDispatcher()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var fakeContactsRepository: FakeContactsRepository

    private lateinit var fakeContactsPickerSessionProviderRepository:
        FakeContactsPickerSessionProviderRepository
    private lateinit var fakePrivacyBannerRepository: FakePrivacyBannerRepository
    private lateinit var mockUserRepository: UserRepository

    private lateinit var mockContactsPickerLogger: ContactsPickerLogger
    private lateinit var viewModel: ContactsViewModel
    private val userStateFlow =
        MutableStateFlow(
            PickerUserState.Success(
                userIdToAvailableUsersMap = emptyMap(),
                selectedUserId = USER_ID_PERSONAL,
            )
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeContactsRepository = FakeContactsRepository()
        fakeContactsPickerSessionProviderRepository = FakeContactsPickerSessionProviderRepository()
        fakePrivacyBannerRepository = FakePrivacyBannerRepository()
        mockUserRepository = mock()
        mockContactsPickerLogger = mock()
        userStateFlow.value =
            PickerUserState.Success(
                userIdToAvailableUsersMap = emptyMap(),
                selectedUserId = USER_ID_PERSONAL,
            )
        runBlocking {
            whenever(mockUserRepository.getUserState(anyOrNull(), anyInt()))
                .thenReturn(userStateFlow)
        }

        val fakeFactory =
            ContactsSelectionHandler.Factory { isMultiSelect, limit, listener ->
                ContactsSelectionHandler(isMultiSelect, limit, listener)
            }
        viewModel =
            ContactsViewModel(
                ApplicationProvider.getApplicationContext(),
                fakeContactsRepository,
                fakeContactsPickerSessionProviderRepository,
                fakePrivacyBannerRepository,
                Lazy { mockUserRepository },
                fakeFactory,
                mockContactsPickerLogger,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @RequiresFlagsDisabled(FLAG_ENABLE_ACTION_PICK_TAKEOVER_IN_DROIDFOOD)
    fun handleIntent_lowSdk_returnsFalse() = runTest {
        val result =
            viewModel.handleIntent(
                intentAction = Intent.ACTION_PICK,
                intentType = Phone.CONTENT_TYPE,
                intentExtras = null,
                callingAppName = TEST_APP_NAME,
                callingPackageName = TEST_PACKAGE_NAME,
                callingAppUid = TEST_CALLING_UID,
                callingAppTargetSdk = ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD - 1,
            )
        assertThat(result).isFalse()
    }

    @Test
    @RequiresFlagsEnabled(FLAG_ENABLE_ACTION_PICK_TAKEOVER_IN_DROIDFOOD)
    fun handleIntent_lowSdkWithTrunkfoodFlag_returnsTrue() = runTest {
        val result =
            viewModel.handleIntent(
                intentAction = Intent.ACTION_PICK,
                intentType = Phone.CONTENT_TYPE,
                intentExtras = null,
                callingAppName = TEST_APP_NAME,
                callingPackageName = TEST_PACKAGE_NAME,
                callingAppUid = TEST_CALLING_UID,
                callingAppTargetSdk = ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD - 1,
            )
        assertThat(result).isTrue()
    }

    @Test
    @RequiresFlagsDisabled(FLAG_ENABLE_ACTION_PICK_TAKEOVER_IN_DROIDFOOD)
    fun handleIntent_lowSdkWithExtraAndTrunkfoodFlagDisabled_returnsTrue() = runTest {
        val extras = Bundle().apply { putBoolean(Intent.EXTRA_USE_SYSTEM_CONTACTS_PICKER, true) }

        val result =
            viewModel.handleIntent(
                intentAction = Intent.ACTION_PICK,
                intentType = Phone.CONTENT_TYPE,
                intentExtras = extras, // With Extra
                callingAppName = "TestApp",
                callingPackageName = "com.test",
                callingAppUid = 123,
                callingAppTargetSdk = 36,
            )
        assertThat(result).isTrue()
    }

    @Test
    fun handleIntent_setsLoadingThenSuccessState() = runTest {
        val testContacts = listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT)
        fakeContactsRepository.setInitialContacts(testContacts)
        val collectedStates = mutableListOf<ContactsUiState>()
        val job = launch { viewModel.uiState.toList(collectedStates) }

        val result =
            viewModel.handleIntent(
                intentAction = Intent.ACTION_PICK,
                intentType = Phone.CONTENT_TYPE,
                intentExtras = null,
                callingAppName = TEST_APP_NAME,
                callingPackageName = TEST_PACKAGE_NAME,
                callingAppUid = TEST_CALLING_UID,
                callingAppTargetSdk = ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD,
            )
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(result).isTrue()
        assertThat(collectedStates).hasSize(2)
        assertThat(collectedStates)
            .containsExactly(
                ContactsListState.Loading,
                ContactsListState.Success(
                    testContacts,
                    emptyContactsSelection(),
                    false,
                    callingAppName = TEST_APP_NAME,
                    requestedMimeTypes = listOf(MimeType.PHONE),
                    showPrivacyBanner = true,
                ),
            )
            .inOrder()

        job.cancel()
    }

    @Test
    fun handleIntent_withNullCallingPackage_doesNotCrash() = runTest {
        val testContacts = listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT)
        fakeContactsRepository.setInitialContacts(testContacts)
        val collectedStates = mutableListOf<ContactsUiState>()
        val job = launch { viewModel.uiState.toList(collectedStates) }

        val result =
            viewModel.handleIntent(
                intentAction = Intent.ACTION_PICK,
                intentType = Phone.CONTENT_TYPE,
                intentExtras = null,
                callingAppName = null,
                callingPackageName = null,
                callingAppUid = TEST_CALLING_UID,
                callingAppTargetSdk = ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD,
            )
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(result).isTrue()
        assertThat(collectedStates).hasSize(2)
        assertThat(collectedStates)
            .containsExactly(
                ContactsListState.Loading,
                ContactsListState.Success(
                    testContacts,
                    emptyContactsSelection(),
                    false,
                    callingAppName = null,
                    requestedMimeTypes = listOf(MimeType.PHONE),
                    showPrivacyBanner = true,
                ),
            )
            .inOrder()

        job.cancel()
    }

    @Test
    fun handleIntent_whenRepositorySucceeds_setsSuccessState() = runTest {
        val displayNameContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        initializeViewModelForLegacyActionPick(listOf(displayNameContact))

        val successState = viewModel.uiState.value as ContactsListState.Success
        assertThat(successState.availableContacts).containsExactly(displayNameContact)
        assertThat(successState.selectedContacts.isEmpty()).isTrue()
    }

    @Test
    fun handleIntent_noContactsForFullContacts_setsNoContactsStateWithCorrectMessage() = runTest {
        initializeViewModelForLegacyActionPick(
            emptyList(),
            intentType = ContactsContract.Contacts.CONTENT_TYPE,
        )

        val noContactsState = viewModel.uiState.value as ContactsListState.NoResults
        assertThat(noContactsState.titleText)
            .isEqualTo(context.getString(R.string.no_contacts_title))
        assertThat(noContactsState.descriptionText)
            .isEqualTo(context.getString(R.string.no_contacts_description))
    }

    @Test
    fun handleIntent_noContactsForEmails_setsNoContactsStateWithCorrectMessage() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))

        val noContactsState = viewModel.uiState.value as ContactsListState.NoResults
        assertThat(noContactsState.titleText)
            .isEqualTo(context.getString(R.string.no_email_contacts_title))
        assertThat(noContactsState.descriptionText).isNull()
    }

    @Test
    fun handleIntent_noContactsForPhones_setsNoContactsStateWithCorrectMessage() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Phone.CONTENT_ITEM_TYPE))

        val noContactsState = viewModel.uiState.value as ContactsListState.NoResults
        assertThat(noContactsState.titleText)
            .isEqualTo(context.getString(R.string.no_phone_contacts_title))
        assertThat(noContactsState.descriptionText).isNull()
    }

    @Test
    fun handleIntent_noContactsForCustomTypes_setsNoContactsStateWithCorrectMessage() = runTest {
        initializeViewModelForActionPickContacts(
            emptyList(),
            listOf(Phone.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE),
        )

        val noContactsState = viewModel.uiState.value as ContactsListState.NoResults
        assertThat(noContactsState.titleText)
            .isEqualTo(context.getString(R.string.no_custom_details_contacts_title))
        assertThat(noContactsState.descriptionText).isNull()
    }

    @Test
    fun handleIntent_customMimeTypesAnNoContactsOnDevice_setsNoContactsStateWithCorrectMessage() =
        runTest {
            fakeContactsRepository.setHasAnyContacts(false)
            initializeViewModelForActionPickContacts(
                emptyList(),
                listOf(Phone.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE),
            )

            val noContactsState = viewModel.uiState.value as ContactsListState.NoResults
            assertThat(noContactsState.titleText)
                .isEqualTo(context.getString(R.string.no_contacts_title))
            assertThat(noContactsState.descriptionText)
                .isEqualTo(context.getString(R.string.no_contacts_description))
        }

    @Test
    fun handleIntent_repositoryThrows_setsErrorState() = runTest {
        val testException = IllegalArgumentException("Unsupported action")
        fakeContactsRepository.setException(testException)

        val result =
            viewModel.handleIntent(
                intentAction = Intent.ACTION_PICK,
                intentType = Phone.CONTENT_TYPE,
                intentExtras = null,
                callingAppName = TEST_APP_NAME,
                callingPackageName = TEST_PACKAGE_NAME,
                callingAppUid = TEST_CALLING_UID,
                callingAppTargetSdk = ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD,
            )
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(result).isTrue()
        val errorState = viewModel.uiState.value as ContactsListState.Error
        assertThat(errorState.message).isEqualTo("Unsupported action")
    }

    @Test(expected = IllegalArgumentException::class)
    fun handleIntent_withInvalidAction_throwsException() {
        viewModel.handleIntent(
            intentAction = "INVALID_ACTION",
            intentType = null,
            intentExtras = null,
            callingAppName = TEST_APP_NAME,
            callingPackageName = TEST_PACKAGE_NAME,
            callingAppUid = TEST_CALLING_UID,
            callingAppTargetSdk = ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD,
        )
    }

    @Test
    fun handleIntent_withoutMultiSelectExtra_setsSingleSelectModeInState() = runTest {
        initializeViewModelForLegacyActionPick(
            listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT),
            intentExtras = null,
        )
        assertThat(viewModel.currentSuccessState.isMultiSelectEnabled).isFalse()
    }

    @Test
    fun handleIntent_withMultiSelectExtraFalse_setsSingleSelectModeInState() = runTest {
        initializeViewModelForLegacyActionPick(
            listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT),
            intentExtras = buildIntentExtrasWithMultiSelect(isMultiSelectEnabled = false),
        )
        assertThat(viewModel.currentSuccessState.isMultiSelectEnabled).isFalse()
    }

    @Test
    fun handleIntent_withMultiSelectExtraTrue_setsMultiSelectModeInState() = runTest {
        initializeViewModelForLegacyActionPick(
            listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT),
            intentExtras = buildIntentExtrasWithMultiSelect(isMultiSelectEnabled = true),
        )
        assertThat(viewModel.currentSuccessState.isMultiSelectEnabled).isTrue()
    }

    @Test
    fun handleIntent_logsSessionStarted() = runTest {
        val intentAction = Intent.ACTION_PICK
        val callingAppTargetSdk = ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD
        viewModel.handleIntent(
            intentAction = intentAction,
            intentType = Phone.CONTENT_TYPE,
            intentExtras = null,
            callingAppName = TEST_APP_NAME,
            callingPackageName = TEST_PACKAGE_NAME,
            callingAppUid = TEST_CALLING_UID,
            callingAppTargetSdk = callingAppTargetSdk,
        )

        verify(mockContactsPickerLogger)
            .logContactsPickerSessionStarted(
                TEST_CALLING_UID,
                callingAppTargetSdk,
                ContactsPickerAction.ACTION_PICK,
                listOf(MimeType.PHONE),
                useSystemContactsPicker = false,
                matchAllRequestedMimeTypes = false,
            )
    }

    @Test
    fun toggleContactSelection_updatesUiState() = runTest {
        val displayNameContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        initializeViewModelForLegacyActionPick(listOf(displayNameContact))

        viewModel.toggleContactSelection(displayNameContact)
        testDispatcher.scheduler.advanceUntilIdle()

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.containsKey(displayNameContact.id)).isTrue()
        assertThat(selection[displayNameContact.id]).containsExactly(displayNameContact.id)
    }

    @Test
    fun toggleEntrySelection_updatesUiState() = runTest {
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        initializeViewModelForLegacyActionPickInMultiSelectMode(listOf(multiPhoneContact))

        val entryToSelect = multiPhoneContact.phones.first()
        viewModel.toggleEntrySelection(multiPhoneContact.id, entryToSelect.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.containsKey(multiPhoneContact.id)).isTrue()
        assertThat(selection[multiPhoneContact.id]).containsExactly(entryToSelect.id)
    }

    @Test
    fun clearSelection_updatesUiState() = runTest {
        val displayNameContactList = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST
        initializeViewModelForLegacyActionPickInMultiSelectMode(displayNameContactList)

        viewModel.toggleContactSelection(displayNameContactList[0])
        testDispatcher.scheduler.advanceUntilIdle()

        var selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.isNotEmpty()).isTrue()

        viewModel.clearSelection()
        testDispatcher.scheduler.advanceUntilIdle()

        selection = viewModel.currentSuccessState.selectedContacts
        assertThat(selection.isEmpty()).isTrue()
    }

    @Test
    fun selectionLimitExceeded_eventsPropagateToViewModel() = runTest {
        val selectionLimit = 1
        val contacts = ContactTestDataFactory.createContactList(2)
        initializeViewModelForLegacyActionPickWithInitialContactsAndSelectionLimit(
            contacts,
            selectionLimit,
        )

        val emittedEvents = mutableListOf<SnackbarEvent>()
        val job = launch { viewModel.snackbarEvents.collect { emittedEvents.add(it) } }

        viewModel.toggleContactSelection(contacts[0])
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleContactSelection(contacts[1])
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(emittedEvents).hasSize(1)
        assertThat(emittedEvents.first())
            .isInstanceOf(SnackbarEvent.ShowSelectionLimitReached::class.java)

        job.cancel()
    }

    @Test
    fun handleIntent_privacyBannerShownBefore_shouldNotShowBannerAgain() = runTest {
        val testContacts = listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT)
        fakePrivacyBannerRepository.markPrivacyBannerAsShown(
            TEST_CALLING_UID,
            listOf(MimeType.PHONE),
        )
        initializeViewModelForLegacyActionPick(
            contacts = testContacts,
            callingAppUid = TEST_CALLING_UID,
        )

        val successState = viewModel.uiState.value as ContactsListState.Success
        assertThat(successState.showPrivacyBanner).isFalse()
    }

    @Test
    fun handleIntent_privacyBannerShownFirstTime_shouldShowBanner() = runTest {
        val testContacts = listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT)
        initializeViewModelForLegacyActionPick(testContacts, callingAppUid = TEST_CALLING_UID)

        val successState = viewModel.uiState.value as ContactsListState.Success
        assertThat(successState.showPrivacyBanner).isTrue()
    }

    @Test
    fun onDoneClicked_inLoadingState_throwsException() = runTest {
        val events = mutableListOf<PickerResultEvent>()
        val job = launch { viewModel.pickerResultEvents.toList(events) }

        assertFailsWith<IllegalStateException> { viewModel.onDoneClicked() }
        assertThat(events).isEmpty()
        job.cancel()
    }

    @Test
    fun onDoneClicked_inErrorState_throwsException() = runTest {
        val events = mutableListOf<PickerResultEvent>()
        val job = launch { viewModel.pickerResultEvents.toList(events) }
        fakeContactsRepository.setException(IllegalArgumentException("Unsupported action"))

        val result =
            viewModel.handleIntent(
                intentAction = Intent.ACTION_PICK,
                intentType = Phone.CONTENT_TYPE,
                intentExtras = null,
                callingAppName = TEST_APP_NAME,
                callingPackageName = TEST_PACKAGE_NAME,
                callingAppUid = TEST_CALLING_UID,
                callingAppTargetSdk = ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD,
            )
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(result).isTrue()
        assertFailsWith<IllegalStateException> { viewModel.onDoneClicked() }
        assertThat(events).isEmpty()
        job.cancel()
    }

    @Test
    fun onDoneClicked_withNoSelection_sendsCancelEvent() = runTest {
        initializeViewModelForLegacyActionPick(
            listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT)
        )

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        assertThat(events.first()).isInstanceOf(PickerResultEvent.CancelAndFinish::class.java)
    }

    @Test
    fun onDoneClicked_withDisplayNameContact_returnsContactLookupUri() = runTest {
        val displayNameContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        initializeViewModelForLegacyActionPick(listOf(displayNameContact))
        viewModel.toggleContactSelection(displayNameContact)
        val baseUri =
            ContactsContract.Contacts.getLookupUri(
                displayNameContact.id,
                displayNameContact.lookupKey,
            )
        val expectedUri = ContentProvider.maybeAddUserId(baseUri, USER_ID_PERSONAL)

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        assertIntentData(events.first(), expectedUri)
    }

    @Test
    fun onDoneClicked_withSingleEmailEntry_returnsDataUri() = runTest {
        val singleEmailContact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        initializeViewModelForLegacyActionPick(listOf(singleEmailContact))
        val entry = singleEmailContact.emails.first()
        viewModel.toggleEntrySelection(singleEmailContact.id, entry.id)
        val baseUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, entry.id)
        val expectedUri = ContentProvider.maybeAddUserId(baseUri, USER_ID_PERSONAL)

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        assertIntentData(events.first(), expectedUri)
    }

    @Test
    fun onDoneClicked_withMultiplePhoneEntries_returnsDataUris() = runTest {
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        // Must be in multi-select mode
        initializeViewModelForLegacyActionPick(
            listOf(multiPhoneContact),
            buildIntentExtrasWithMultiSelect(true),
        )
        viewModel.toggleContactSelection(multiPhoneContact) // Selects all entries

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        assertThat(getUrisFromClipData(events.first()))
            .containsExactlyElementsIn(getExpectedDataUris(multiPhoneContact.phones.map { it.id }))
    }

    @Test
    fun onDoneClicked_withMixedSelection_returnsAllUris() = runTest {
        val displayNameContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        // Must be in multi-select mode
        initializeViewModelForLegacyActionPick(
            listOf(displayNameContact, multiPhoneContact),
            buildIntentExtrasWithMultiSelect(true),
        )
        // Select the DisplayNameContact
        viewModel.toggleContactSelection(displayNameContact)
        // Select the first phone entry from the MultiPhoneContact
        val entry = multiPhoneContact.phones.first()
        viewModel.toggleEntrySelection(multiPhoneContact.id, entry.id)

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        val expectedDisplayNameUri =
            ContentProvider.maybeAddUserId(
                ContactsContract.Contacts.getLookupUri(
                    displayNameContact.id,
                    displayNameContact.lookupKey,
                ),
                USER_ID_PERSONAL,
            )
        val expectedPhoneUri =
            ContentProvider.maybeAddUserId(
                ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, entry.id),
                USER_ID_PERSONAL,
            )
        assertThat(getUrisFromClipData(events.first()))
            .containsExactly(expectedDisplayNameUri, expectedPhoneUri)
    }

    @Test
    fun onDoneClicked_inSingleSelect_withMultipleEntriesSelected_returnsOnlyOneUri() = runTest {
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        // This simulates the defensive logic in toggleContactSelection
        initializeViewModelForLegacyActionPick(
            listOf(multiPhoneContact),
            buildIntentExtrasWithMultiSelect(false),
        )
        viewModel.toggleContactSelection(
            multiPhoneContact
        ) // This should only select the first entry

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        val firstEntry = multiPhoneContact.phones.first()
        val baseUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, firstEntry.id)
        val expectedUri = ContentProvider.maybeAddUserId(baseUri, USER_ID_PERSONAL)

        assertIntentData(events.first(), expectedUri)
    }

    @Test
    fun onDoneClicked_inSearchStateWithError_throwsException() = runTest {
        val events = mutableListOf<PickerResultEvent>()
        val job = launch { viewModel.pickerResultEvents.toList(events) }
        val query = "query"
        fakeContactsRepository.setSearchException(query, IllegalArgumentException())
        initializeViewModelForLegacyActionPick(
            listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT)
        )
        viewModel.onSearchQueryChanged(query)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value is SearchState.Error).isTrue()

        assertFailsWith<IllegalStateException> { viewModel.onDoneClicked() }
        assertThat(events).isEmpty()
        job.cancel()
    }

    @Test
    fun onDoneClicked_withSingleEmailEntryInSearchState_returnsDataUri() = runTest {
        val singleEmailContact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        initializeViewModelForLegacyActionPick(listOf(singleEmailContact))
        viewModel.onSearchQueryChanged("query")
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value is SearchState.Success).isTrue()
        val entry = singleEmailContact.emails.first()
        viewModel.toggleEntrySelection(singleEmailContact.id, entry.id)

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        val baseUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, entry.id)
        val expectedUri = ContentProvider.maybeAddUserId(baseUri, USER_ID_PERSONAL)
        assertIntentData(events.first(), expectedUri)
    }

    @Test
    fun onDoneClicked_withMultiplePhoneEntriesInSearchState_returnsDataUris() = runTest {
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        // Must be in multi-select mode
        initializeViewModelForLegacyActionPick(
            listOf(multiPhoneContact),
            buildIntentExtrasWithMultiSelect(true),
        )
        viewModel.onSearchQueryChanged("query")
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value is SearchState.Success).isTrue()
        viewModel.toggleContactSelection(multiPhoneContact) // Selects all entries

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        assertThat(getUrisFromClipData(events.first()))
            .containsExactlyElementsIn(getExpectedDataUris(multiPhoneContact.phones.map { it.id }))
    }

    @Test
    fun onDoneClicked_withSingleEmailEntryInPreviewState_returnsDataUri() = runTest {
        val singleEmailContact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        initializeViewModelForLegacyActionPick(listOf(singleEmailContact))
        val entry = singleEmailContact.emails.first()
        viewModel.toggleEntrySelection(singleEmailContact.id, entry.id)
        viewModel.onPreviewClicked()
        assertThat(viewModel.uiState.value is ContactsPreviewState).isTrue()

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        val baseUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, entry.id)
        val expectedUri = ContentProvider.maybeAddUserId(baseUri, USER_ID_PERSONAL)
        assertIntentData(events.first(), expectedUri)
    }

    @Test
    fun onDoneClicked_withMultiplePhoneEntriesInPreviewState_returnsDataUris() = runTest {
        val multiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        // Must be in multi-select mode
        initializeViewModelForLegacyActionPick(
            listOf(multiPhoneContact),
            buildIntentExtrasWithMultiSelect(true),
        )
        viewModel.toggleContactSelection(multiPhoneContact) // Selects all entries
        viewModel.onPreviewClicked()
        assertThat(viewModel.uiState.value is ContactsPreviewState).isTrue()

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        assertThat(getUrisFromClipData(events.first()))
            .containsExactlyElementsIn(getExpectedDataUris(multiPhoneContact.phones.map { it.id }))
    }

    @Test
    fun onDoneClicked_actionPickContacts_emailOnly() = runTest {
        val contact1 = ContactTestDataFactory.createEmailContact(1L, "A")
        val expectedSessionUri = Uri.parse("content://session/emails_mode")

        fakeContactsPickerSessionProviderRepository.setSessionResult(
            dataIds = listOf(contact1.emails.first().id),
            callingUid = TEST_CALLING_UID,
            resultUri = expectedSessionUri,
        )
        initializeViewModelForActionPickContacts(
            initialContacts = listOf(contact1),
            requestedMimeTypes = listOf(Email.CONTENT_ITEM_TYPE),
            callingUid = TEST_CALLING_UID,
        )

        viewModel.toggleEntrySelection(contact1.id, contact1.emails.first().id)

        val events = callOnDoneAndCaptureEvents()

        assertSessionResult(events, expectedSessionUri)
    }

    @Test
    fun onDoneClicked_actionPickContacts_customMode() = runTest {
        val contact1 = ContactTestDataFactory.createDisplayNameContact(1L, "A")
        val contact2 = ContactTestDataFactory.createDisplayNameContact(2L, "B")
        val expectedSessionUri = Uri.parse("content://session/custom_mode")
        val expectedDataIds = listOf(101L, 102L, 123L, 111L)
        val mimeTypes = ArrayList(listOf(Email.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE))
        fakeContactsRepository.setDataRowIdsResult(
            contactIds = listOf(contact1.id, contact2.id),
            mimeTypes = mimeTypes.map { MimeType.fromString(it) },
            dataIds = expectedDataIds,
        )
        fakeContactsPickerSessionProviderRepository.setSessionResult(
            dataIds = expectedDataIds,
            callingUid = TEST_CALLING_UID,
            resultUri = expectedSessionUri,
        )

        initializeViewModelForActionPickContacts(
            initialContacts = listOf(contact1, contact2),
            requestedMimeTypes = mimeTypes,
            isMultiSelect = true,
            callingUid = TEST_CALLING_UID,
        )

        viewModel.toggleContactSelection(contact1)
        viewModel.toggleContactSelection(contact2)

        val events = callOnDoneAndCaptureEvents()

        assertSessionResult(events, expectedSessionUri)
    }

    @Test
    fun onDoneClicked_actionPickContacts_noSelection_cancelsAction() = runTest {
        initializeViewModelForActionPickContacts(
            initialContacts = listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT),
            requestedMimeTypes = listOf(Email.CONTENT_ITEM_TYPE),
        )

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        assertThat(events.first()).isInstanceOf(PickerResultEvent.CancelAndFinish::class.java)
    }

    @Test
    fun onSearchQueryChanged_debouncesSearch() = runTest {
        val searchQuery = "test"
        val searchResult =
            listOf(ContactTestDataFactory.createDisplayNameContact(10L, "Test Result 1"))

        fakeContactsRepository.setSearchResults(searchQuery, searchResult)
        initializeViewModelForLegacyActionPick(emptyList())

        val collectedStates = mutableListOf<ContactsUiState>()
        val job = launch { viewModel.uiState.toList(collectedStates) }

        viewModel.onSearchQueryChanged(searchQuery)
        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS - 100)
        // Repository search for searchQuery should not have been called yet
        assertThat(fakeContactsRepository.searchInvocationsCountForQuery(searchQuery)).isEqualTo(0)

        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS)
        // Repository search for searchQuery should have been called now
        assertThat(fakeContactsRepository.searchInvocationsCountForQuery(searchQuery)).isEqualTo(1)
        assertThat(collectedStates.last())
            .isEqualTo(SearchState.Success(searchQuery, searchResult, emptyContactsSelection()))
        job.cancel()
    }

    @Test
    fun onSearchQueryChanged_searchError_setsErrorState() = runTest {
        val searchQuery = "error"
        val exception = RuntimeException("An unexpected error occurred during search.")
        fakeContactsRepository.setSearchException(searchQuery, exception)

        initializeViewModelForLegacyActionPick(emptyList())

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
        initializeViewModelForLegacyActionPick(
            ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST
        )
        fakeContactsRepository.setSearchResults(searchQuery, searchResults)

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
                    emptyContactsSelection(),
                    false,
                    callingAppName = "TestApp",
                    requestedMimeTypes = listOf(MimeType.PHONE),
                    showPrivacyBanner = true,
                ), // Initial state after processIntent
                SearchState.Success(
                    searchQuery,
                    searchResults,
                    emptyContactsSelection(),
                ), // State after search completes
            )
            .inOrder()
    }

    @Test
    fun onSearchQueryChanged_emptySearchResults_setsSuccessWithEmptyList() = runTest {
        val searchQuery = "no_match"

        fakeContactsRepository.setSearchResults(searchQuery, emptyList())

        initializeViewModelForLegacyActionPick(emptyList())

        viewModel.onSearchQueryChanged(searchQuery)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value)
            .isEqualTo(SearchState.Success(searchQuery, emptyList(), emptyContactsSelection()))
    }

    @Test
    fun onSearchQueryChanged_blankQuery_transitionsToEmptySearchState() = runTest {
        initializeViewModelForLegacyActionPick(
            ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST
        )

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

        initializeViewModelForLegacyActionPick(
            ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST
        )

        fakeContactsRepository.setSearchResults(searchQuery, searchResults)

        viewModel.onSearchQueryChanged(searchQuery)
        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value)
            .isEqualTo(SearchState.Success(searchQuery, searchResults, emptyContactsSelection()))
    }

    @Test
    fun onSearchQueryChanged_selectionPreservedAcrossSearches() = runTest {
        val query1 = "test"
        val results1 = listOf(ContactTestDataFactory.createDisplayNameContact(1L, "Test Contact"))
        val query2 = "another"
        val results2 =
            listOf(ContactTestDataFactory.createDisplayNameContact(2L, "Another Contact"))

        fakeContactsRepository.setSearchResults(query1, results1)
        fakeContactsRepository.setSearchResults(query2, results2)

        initializeViewModelForLegacyActionPick(emptyList())

        // First search and select
        viewModel.onSearchQueryChanged(query1)
        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleContactSelection(results1[0])
        testDispatcher.scheduler.advanceUntilIdle()
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

        initializeViewModelForLegacyActionPick(
            ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST
        )

        viewModel.toggleContactSelection(contactToSelect)
        testDispatcher.scheduler.advanceUntilIdle()
        val initialSelection =
            (viewModel.uiState.value as ContactsListState.Success).selectedContacts
        assertThat(initialSelection.containsKey(contactToSelect.id)).isTrue()

        // Perform a search
        val searchQuery = "query"
        val searchResults = listOf(initialContacts[1])
        fakeContactsRepository.setSearchResults(searchQuery, searchResults)
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
        fakeContactsRepository.setSearchResults(searchQuery, searchResults)

        initializeViewModelForLegacyActionPick(initialContacts)

        // Perform a search
        viewModel.onSearchQueryChanged(searchQuery)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(SearchState.Success::class.java)

        // Select an item in search results
        val contactToSelect = searchResults[0]
        viewModel.toggleContactSelection(contactToSelect)
        testDispatcher.scheduler.advanceUntilIdle()
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

    @Test
    fun handleIntent_whenSelectMultipleNotEnabled_ignoresSelectionLimitExtra() = runTest {
        initializeViewModelForLegacyActionPick(
            ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST,
            buildIntentExtrasWithSelectionLimit(
                isMultiSelectEnabled = false,
                selectionLimit = MAX_ALLOWED_SELECTION_LIMIT * 2,
            ),
        )

        // UI state is Success
        val state = viewModel.uiState.value
        assertThat(state is ContactsListState.Success).isTrue()
    }

    @Test(expected = IllegalArgumentException::class)
    fun handleIntent_whenSelectMultipleEnabledAndLimitExceedsMax_throwsException() = runTest {
        initializeViewModelForLegacyActionPick(
            ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST,
            buildIntentExtrasWithSelectionLimit(true, MAX_ALLOWED_SELECTION_LIMIT + 1),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun handleIntent_whenSelectMultipleEnabledAndLimitZero_throwsException() = runTest {
        initializeViewModelForLegacyActionPick(
            ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST,
            buildIntentExtrasWithSelectionLimit(true, 0),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun handleIntent_whenSelectMultipleEnabledAndLimitNegative_throwsException() = runTest {
        initializeViewModelForLegacyActionPick(
            ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT_LIST,
            buildIntentExtrasWithSelectionLimit(true, -1),
        )
    }

    @Test
    fun handleIntent_noSelectLimitExtra_usesDefaultLimit() = runTest {
        val contacts = ContactTestDataFactory.createContactList(DEFAULT_SELECTION_LIMIT + 1)
        initializeViewModelForLegacyActionPickInMultiSelectMode(contacts)
        val emittedEvents = mutableListOf<SnackbarEvent>()
        val job = launch { viewModel.snackbarEvents.collect { emittedEvents.add(it) } }

        for (it in 0..<DEFAULT_SELECTION_LIMIT) {
            viewModel.toggleContactSelection(contacts[it])
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Try to add one more
        viewModel.toggleContactSelection(contacts[DEFAULT_SELECTION_LIMIT])
        testDispatcher.scheduler.advanceUntilIdle()

        // Check that event was fired
        assertThat(emittedEvents.size).isEqualTo(1)
        val event = emittedEvents.first()
        assertThat(event is SnackbarEvent.ShowSelectionLimitReached).isTrue()
        assertThat((event as SnackbarEvent.ShowSelectionLimitReached).limit)
            .isEqualTo(DEFAULT_SELECTION_LIMIT)

        job.cancel()
    }

    @Test
    fun onPreviewClicked_updatesStateToPreview() = runTest {
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        initializeViewModelForLegacyActionPick(listOf(contact))
        viewModel.toggleContactSelection(contact)
        testDispatcher.scheduler.advanceUntilIdle()

        val currentSelection = viewModel.currentSuccessState.selectedContacts

        viewModel.onPreviewClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ContactsPreviewState::class.java)
        val previewState = state as ContactsPreviewState
        assertThat(previewState.contactsToDisplay).containsExactly(contact)
        assertThat(previewState.selectedContacts).isEqualTo(currentSelection)
        assertThat(previewState.isMultiSelectEnabled).isFalse()
    }

    @Test
    fun onBackFromPreview_updatesStateToList() = runTest {
        val contact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        initializeViewModelForLegacyActionPick(listOf(contact))
        viewModel.toggleContactSelection(contact)
        testDispatcher.scheduler.advanceUntilIdle()

        val selection = viewModel.currentSuccessState.selectedContacts

        viewModel.onPreviewClicked()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(ContactsPreviewState::class.java)

        viewModel.onBackFromPreview()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ContactsListState.Success::class.java)
        val listState = state as ContactsListState.Success
        assertThat(listState.availableContacts).containsExactly(contact)
        assertThat(listState.selectedContacts).isEqualTo(selection)
    }

    /** Helper to assert that the operation succeeded and returned the expected Session URI. */
    private fun assertSessionResult(events: List<PickerResultEvent>, expectedUri: Uri) {
        assertThat(events).hasSize(1)
        val event = events.first()
        assertThat(event).isInstanceOf(PickerResultEvent.SetResultAndFinish::class.java)

        val intent = (event as PickerResultEvent.SetResultAndFinish).intent
        assertThat(intent).isNotNull()
        assertThat(intent.data).isEqualTo(expectedUri)
        assertThat(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .isEqualTo(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun assertIntentData(event: PickerResultEvent?, expectedUri: Uri) {
        assertThat(event).isInstanceOf(PickerResultEvent.SetResultAndFinish::class.java)

        val intent = (event as PickerResultEvent.SetResultAndFinish).intent

        assertThat(intent).isNotNull()
        assertThat(intent.data).isEqualTo(expectedUri)
        assertThat(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .isEqualTo(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun getUrisFromClipData(event: PickerResultEvent?): List<Uri> {
        assertThat(event).isInstanceOf(PickerResultEvent.SetResultAndFinish::class.java)
        val intent = (event as PickerResultEvent.SetResultAndFinish).intent

        assertThat(intent).isNotNull()
        assertThat(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .isEqualTo(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val clipData = intent.clipData
        assertThat(clipData).isNotNull()
        return (0 until clipData!!.itemCount).map { index -> clipData.getItemAt(index).uri }
    }

    private fun getExpectedDataUris(
        dataIds: List<Long>,
        userId: Int = USER_ID_PERSONAL,
    ): List<Uri> {
        return dataIds.map {
            val baseUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, it)
            ContentProvider.maybeAddUserId(baseUri, userId)
        }
    }

    /**
     * Helper to initialize the ViewModel for ACTION_PICK_CONTACTS. Handles creating the intent
     * extras and processing the intent.
     */
    private fun initializeViewModelForActionPickContacts(
        initialContacts: List<Contact>,
        requestedMimeTypes: List<String>,
        isMultiSelect: Boolean = false,
        callingUid: Int = TEST_CALLING_UID,
    ) {
        fakeContactsRepository.setInitialContacts(initialContacts)

        val extras =
            Bundle().apply {
                putStringArrayList(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    ArrayList(requestedMimeTypes),
                )
                if (isMultiSelect) {
                    putBoolean(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }

        val result =
            viewModel.handleIntent(
                intentAction = ContactsPickerSessionContract.ACTION_PICK_CONTACTS,
                intentType = null,
                intentExtras = extras,
                callingAppName = TEST_APP_NAME,
                callingPackageName = TEST_PACKAGE_NAME,
                callingAppUid = callingUid,
                callingAppTargetSdk = ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD,
            )
        assertThat(result).isTrue()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    /**
     * Helper function to put the ViewModel into a Success state with a predefined list of contacts.
     */
    private fun initializeViewModelForLegacyActionPick(
        contacts: List<Contact>,
        intentExtras: Bundle? = null,
        intentType: String = Phone.CONTENT_TYPE,
        callingAppUid: Int = TEST_CALLING_UID,
    ) {
        fakeContactsRepository.setInitialContacts(contacts)
        val result =
            viewModel.handleIntent(
                intentAction = Intent.ACTION_PICK,
                intentType = intentType,
                intentExtras = intentExtras,
                callingAppName = TEST_APP_NAME,
                callingPackageName = TEST_PACKAGE_NAME,
                callingAppUid = callingAppUid,
                callingAppTargetSdk = ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD,
            )
        assertThat(result).isTrue()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun initializeViewModelForLegacyActionPickInMultiSelectMode(contacts: List<Contact>) {
        initializeViewModelForLegacyActionPick(
            contacts,
            buildIntentExtrasWithMultiSelect(isMultiSelectEnabled = true),
        )
    }

    private fun initializeViewModelForLegacyActionPickWithInitialContactsAndSelectionLimit(
        contacts: List<Contact>,
        selectionLimit: Int,
    ) {
        initializeViewModelForLegacyActionPick(
            contacts,
            buildIntentExtrasWithSelectionLimit(
                isMultiSelectEnabled = true,
                selectionLimit = selectionLimit,
            ),
        )
    }

    private fun buildIntentExtrasWithMultiSelect(isMultiSelectEnabled: Boolean): Bundle =
        Bundle().apply { putBoolean(Intent.EXTRA_ALLOW_MULTIPLE, isMultiSelectEnabled) }

    private fun buildIntentExtrasWithSelectionLimit(
        isMultiSelectEnabled: Boolean,
        selectionLimit: Int,
    ): Bundle =
        Bundle().apply {
            putBoolean(Intent.EXTRA_ALLOW_MULTIPLE, isMultiSelectEnabled)
            putInt(
                ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT,
                selectionLimit,
            )
        }

    /**
     * A helper property to safely access the Success state for assertions. Fails the test if the
     * current state is not Success.
     */
    private val ContactsViewModel.currentSuccessState: ContactsListState.Success
        get() {
            val state = this.uiState.value
            assertThat(state).isInstanceOf(ContactsListState.Success::class.java)
            return state as ContactsListState.Success
        }

    private val ContactsViewModel.currentSuccessUserState: PickerUserState.Success
        get() {
            val state = this.userState.value
            assertThat(state).isInstanceOf(PickerUserState.Success::class.java)
            return state as PickerUserState.Success
        }

    @Test
    fun onPreviewState_deselectingLastItemSwitchToPreviousState() = runTest {
        val contact = ContactTestDataFactory.GENERIC_PHONE_CONTACT
        initializeViewModelForLegacyActionPickInMultiSelectMode(listOf(contact))
        viewModel.toggleContactSelection(contact)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onPreviewClicked()

        val entryToDeselect = contact.phones.first()
        viewModel.toggleEntrySelection(contact.id, entryToDeselect.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ContactsListState.Success::class.java)
    }

    @Test
    fun onBackFromPreview_propagatesSelectionChangesToListState() = runTest {
        val contacts = ContactTestDataFactory.createContactList(2)
        initializeViewModelForLegacyActionPickInMultiSelectMode(contacts)
        contacts.forEach { contact -> viewModel.toggleContactSelection(contact) }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onPreviewClicked()

        viewModel.toggleContactSelection(contacts[0])
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onBackFromPreview()

        val state = viewModel.uiState.value as ContactsListState.Success
        assertThat(state.selectedContacts.containsKey(contacts[0].id)).isFalse()
        assertThat(state.selectedContacts.containsKey(contacts[1].id)).isTrue()
    }

    @Test
    fun onBackFromPreview_propagatesSelectionChangesToSearchState() = runTest {
        val contact1 = ContactTestDataFactory.GENERIC_PHONE_CONTACT
        val contact2 = ContactTestDataFactory.GENERIC_PHONE_CONTACT
        val query = "Test"
        fakeContactsRepository.setSearchResults(query, listOf(contact1))
        initializeViewModelForLegacyActionPickInMultiSelectMode(listOf(contact1, contact2))

        // Search and select contact1
        viewModel.onSearchQueryChanged(query)
        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleContactSelection(contact1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Go to Preview
        viewModel.onPreviewClicked()

        // Deselect contact1 in Preview
        viewModel.toggleContactSelection(contact1)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SearchState.Success
        assertThat(state.selectedContacts.isEmpty()).isTrue()
        assertThat(state.query).isEqualTo(query)
    }

    @Test
    fun handleIntent_actionPick_doesNotClearSelectedUser() = runTest {
        initializeViewModelForLegacyActionPick(emptyList())

        verify(mockUserRepository, never()).clearSelectedUser()
    }

    @Test
    fun userState_updatesReflectInViewModel() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))
        val newUserState = PickerUserState.Success(emptyMap(), USER_ID_WORK)

        userStateFlow.emit(newUserState)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.userState.value).isEqualTo(newUserState)
    }

    @Test
    fun userStateChange_reloadsContactsWithCorrectUserId() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))
        val newUserId = USER_ID_WORK
        val newUserState = PickerUserState.Success(emptyMap(), newUserId)

        userStateFlow.emit(newUserState)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(fakeContactsRepository.getContactsInvocationsCount()).isEqualTo(2)
    }

    @Test
    fun handleIntent_actionPickContacts_clearsSelectedUser() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))

        verify(mockUserRepository).clearSelectedUser()
    }

    @Test
    fun onProfileSelected_callsRepositorySetSelectedUser() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))
        val newUserId = USER_ID_WORK

        viewModel.onProfileSelected(newUserId)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockUserRepository).setSelectedUser(newUserId)
    }

    @Test
    fun onProfileSelected_sameUser_doesNotCallRepository() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))
        val currentUserId = USER_ID_PERSONAL

        viewModel.onProfileSelected(currentUserId)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockUserRepository, never()).setSelectedUser(anyInt())
    }

    @Test
    fun onProfileClicked_switchableProfile_switchesUser() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))

        viewModel.onProfileClicked(WORK_PROFILE)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockUserRepository).setSelectedUser(USER_ID_WORK)
    }

    @Test
    fun onProfileClicked_pausedProfile_showsBlockedDialog() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))

        viewModel.onProfileClicked(PAUSED_WORK_PROFILE)
        testDispatcher.scheduler.advanceUntilIdle()

        val successState = viewModel.currentSuccessUserState
        assertThat(successState.profileBlockedDialogData).isNotNull()
    }

    @Test
    fun dismissProfileBlockedDialog_hidesDialog() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))

        viewModel.onProfileClicked(PAUSED_WORK_PROFILE)
        testDispatcher.scheduler.advanceUntilIdle()

        val stateWithDialog = viewModel.currentSuccessUserState
        assertThat(stateWithDialog.profileBlockedDialogData).isNotNull()

        viewModel.dismissProfileBlockedDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        val stateWithoutDialog = viewModel.currentSuccessUserState
        assertThat(stateWithoutDialog.profileBlockedDialogData).isNull()
    }

    @Test
    fun performSearch_usesSelectedUserId() = runTest {
        val selectedUserId = USER_ID_WORK
        userStateFlow.value = PickerUserState.Success(emptyMap(), selectedUserId)
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))

        val query = "test"
        fakeContactsRepository.setSearchResults(query, emptyList())

        viewModel.onSearchQueryChanged(query)
        testDispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(fakeContactsRepository.lastSearchContactsUserId).isEqualTo(selectedUserId)
    }

    @Test
    fun onProfileClicked_pausedProfile_adminBlocked_showsAdminDialog() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))

        viewModel.onProfileClicked(BLOCKED_WORK_PROFILE)
        testDispatcher.scheduler.advanceUntilIdle()

        val successState = viewModel.currentSuccessUserState
        assertThat(successState.profileBlockedDialogData).isNotNull()
    }

    @Test
    fun onDoneClicked_usesSelectedUserIdForSession() = runTest {
        val selectedUserId = USER_ID_SECONDARY
        userStateFlow.value = PickerUserState.Success(emptyMap(), selectedUserId)

        val contact = ContactTestDataFactory.createEmailContact(1L, "A")

        fakeContactsPickerSessionProviderRepository.setSessionResult(
            dataIds = listOf(contact.emails.first().id),
            callingUid = TEST_CALLING_UID,
            resultUri = Uri.parse("content://session"),
        )

        initializeViewModelForActionPickContacts(
            initialContacts = listOf(contact),
            requestedMimeTypes = listOf(Email.CONTENT_ITEM_TYPE),
            callingUid = TEST_CALLING_UID,
        )

        viewModel.toggleEntrySelection(contact.id, contact.emails.first().id)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDoneClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(fakeContactsPickerSessionProviderRepository.lastSourceUserId)
            .isEqualTo(selectedUserId)
    }

    @Test
    fun userStateChange_repoThrows_setsErrorState() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))
        val newUserId = USER_ID_WORK
        val newUserState = PickerUserState.Success(emptyMap(), newUserId)

        fakeContactsRepository.setException(RuntimeException("Failed to load"))
        userStateFlow.emit(newUserState)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ContactsListState.Error::class.java)
        assertThat((state as ContactsListState.Error).message).isEqualTo("Failed to load")
    }

    @Test
    fun userStateChange_emptyContacts_setsNoContactsListState() = runTest {
        initializeViewModelForActionPickContacts(
            listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT),
            listOf(Email.CONTENT_ITEM_TYPE),
        )
        val newUserId = USER_ID_WORK
        val newUserState = PickerUserState.Success(emptyMap(), newUserId)

        // Set next call to return empty list
        fakeContactsRepository.setInitialContacts(emptyList())
        userStateFlow.emit(newUserState)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ContactsListState.NoResults::class.java)
    }

    @Test
    fun onDoneClicked_customQueryMode_passesUserIdToGetContacts() = runTest {
        val selectedUserId = USER_ID_WORK
        userStateFlow.value = PickerUserState.Success(emptyMap(), selectedUserId)
        val customMimeTypes = listOf(Email.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE)
        val contact =
            ContactTestDataFactory.createDisplayNameContact(
                id = 1L,
                lookupKey = TEST_CONTACT_LOOKUP_KEY,
                displayName = TEST_CONTACT_DISPLAY_NAME,
            )

        initializeViewModelForActionPickContacts(
            initialContacts = listOf(contact),
            requestedMimeTypes = customMimeTypes,
            callingUid = TEST_CALLING_UID,
        )

        viewModel.toggleContactSelection(contact)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDoneClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(fakeContactsRepository.lastGetDataRowIdsUserId).isEqualTo(selectedUserId)
    }

    @Test
    fun onProfileClicked_pausedProfile_undefinedReason_showsGenericDialog() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))

        val undefinedPausedProfile =
            UserProfile(
                userId = 11,
                userIdToQueryContacts = 11,
                userType = UserType.WORK,
                switchableInfo = SwitchableProfileInfo("Work", null),
                pausedInfo = PausedProfileInfo(PausedReason.UNDEFINED),
            )

        viewModel.onProfileClicked(undefinedPausedProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        val successState = viewModel.currentSuccessUserState
        val dialogData = successState.profileBlockedDialogData
        assertThat(dialogData).isNotNull()

        assertThat(dialogData?.title).isEqualTo(PAUSED_WORK_APPS_TITLE)
    }

    @Test
    fun onProfileClicked_neitherPausedNorSwitchable_doesNothing() = runTest {
        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))

        val inertProfile =
            UserProfile(
                userId = 10,
                userIdToQueryContacts = 10,
                userType = UserType.PERSONAL,
                switchableInfo = null,
                pausedInfo = null,
            )

        viewModel.onProfileClicked(inertProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockUserRepository, never()).setSelectedUser(anyInt())
        val successState = viewModel.currentSuccessUserState
        assertThat(successState.profileBlockedDialogData).isNull()
    }

    /** Helper to trigger [onDoneClicked] and capture the emitted result event. */
    private fun TestScope.callOnDoneAndCaptureEvents(): List<PickerResultEvent> {
        val events = mutableListOf<PickerResultEvent>()
        val job = launch { viewModel.pickerResultEvents.toList(events) }

        try {
            viewModel.onDoneClicked()
            testDispatcher.scheduler.advanceUntilIdle()
            return events
        } finally {
            job.cancel()
        }
    }

    @Test
    fun userStateChange_irrelevantChange_doesNotReloadContacts() = runTest {
        val initialUserState =
            PickerUserState.Success(
                userIdToAvailableUsersMap =
                    mapOf(USER_ID_PERSONAL to PERSONAL_PROFILE, USER_ID_WORK to WORK_PROFILE),
                selectedUserId = USER_ID_PERSONAL,
            )
        userStateFlow.value = initialUserState

        initializeViewModelForActionPickContacts(emptyList(), listOf(Email.CONTENT_ITEM_TYPE))
        val initialLoadCount = fakeContactsRepository.getContactsInvocationsCount()

        val newUserState =
            initialUserState.copy(
                userIdToAvailableUsersMap =
                    mapOf(USER_ID_PERSONAL to PERSONAL_PROFILE, USER_ID_WORK to PAUSED_WORK_PROFILE)
            )

        userStateFlow.emit(newUserState)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(fakeContactsRepository.getContactsInvocationsCount()).isEqualTo(initialLoadCount)
    }

    @Test
    fun userStateChange_switchingProfile_clearsSelection() = runTest {
        val initialUserState =
            PickerUserState.Success(
                userIdToAvailableUsersMap =
                    mapOf(USER_ID_PERSONAL to PERSONAL_PROFILE, USER_ID_WORK to WORK_PROFILE),
                selectedUserId = USER_ID_PERSONAL,
            )
        userStateFlow.value = initialUserState

        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        initializeViewModelForActionPickContacts(
            initialContacts = listOf(contact),
            requestedMimeTypes = listOf(Email.CONTENT_ITEM_TYPE),
        )

        viewModel.toggleContactSelection(contact)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.currentSuccessState.selectedContacts.isEmpty()).isFalse()

        val newUserState = initialUserState.copy(selectedUserId = USER_ID_WORK)
        userStateFlow.emit(newUserState)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.currentSuccessState.selectedContacts.isEmpty()).isTrue()
    }

    @Test
    fun onDoneClicked_actionPick_appendsUserIdToResultUri() = runTest {
        val workAppUid = UserHandle.getUid(USER_ID_WORK, TEST_APP_ID)

        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        initializeViewModelForLegacyActionPick(listOf(contact), callingAppUid = workAppUid)
        viewModel.toggleContactSelection(contact)
        testDispatcher.scheduler.advanceUntilIdle()

        val baseUri = ContactsContract.Contacts.getLookupUri(contact.id, contact.lookupKey)
        val expectedUri = ContentProvider.maybeAddUserId(baseUri, USER_ID_WORK)

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        val resultIntent = (events.first() as PickerResultEvent.SetResultAndFinish).intent
        assertThat(resultIntent.data).isEqualTo(expectedUri)
        assertThat(ContentProvider.getUserIdFromUri(resultIntent.data)).isEqualTo(USER_ID_WORK)
    }

    @Test
    fun onDoneClicked_actionPick_multiSelect_appendsUserIdToResultUris() = runTest {
        val workAppUid = UserHandle.getUid(USER_ID_WORK, TEST_APP_ID)

        val contact1 = ContactTestDataFactory.createDisplayNameContact(1L, "A")
        val contact2 = ContactTestDataFactory.createDisplayNameContact(2L, "B")

        initializeViewModelForLegacyActionPick(
            listOf(contact1, contact2),
            buildIntentExtrasWithMultiSelect(true),
            callingAppUid = workAppUid,
        )
        viewModel.toggleContactSelection(contact1)
        viewModel.toggleContactSelection(contact2)
        testDispatcher.scheduler.advanceUntilIdle()

        val uri1 =
            ContentProvider.maybeAddUserId(
                ContactsContract.Contacts.getLookupUri(contact1.id, contact1.lookupKey),
                USER_ID_WORK,
            )
        val uri2 =
            ContentProvider.maybeAddUserId(
                ContactsContract.Contacts.getLookupUri(contact2.id, contact2.lookupKey),
                USER_ID_WORK,
            )

        val events = callOnDoneAndCaptureEvents()

        assertThat(events).hasSize(1)
        assertThat(getUrisFromClipData(events.first())).containsExactly(uri1, uri2)
    }
}
