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
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.EmailEntry
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
        val testContacts =
            listOf(
                EmailContact(
                    1L,
                    "Test",
                    listOf(EmailEntry(id = 11L, address = "alice@wonderland.org", label = "Home")),
                )
            )
        fakeRepository.setInitialContacts(testContacts)

        viewModel.processIntent(Intent.ACTION_PICK, Email.CONTENT_TYPE)
        testDispatcher.scheduler.advanceUntilIdle()

        val successState = viewModel.uiState.value as ContactsUiState.Success
        assertThat(successState.contacts).isEqualTo(testContacts)
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
}
