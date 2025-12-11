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
package com.android.contactspicker.data.repository

import android.content.Context
import android.content.pm.ProviderInfo
import android.net.Uri
import android.provider.ContactsPickerSessionContract
import android.test.mock.MockContentResolver
import com.android.contactspicker.fakes.FakeContentProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class ContactsPickerSessionProviderRepositoryImplTest {

    private val mockContext: Context = mock()
    private val fakeContentProvider = FakeContentProvider()
    private val mockContentResolver = MockContentResolver()
    private lateinit var repository: ContactsPickerSessionProviderRepository

    @Before
    fun setUp() {
        val providerInfo =
            ProviderInfo().apply { authority = ContactsPickerSessionContract.AUTHORITY }
        fakeContentProvider.attachInfo(mockContext, providerInfo)

        mockContentResolver.addProvider(
            ContactsPickerSessionContract.AUTHORITY,
            fakeContentProvider,
        )
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
        repository = ContactsPickerSessionProviderRepositoryImpl(mockContext)
    }

    @Test
    fun createSession_insertsCorrectValues() = runTest {
        val callingUid = 12345
        val dataIds = listOf(1L, 2L)
        val expectedSessionUri =
            Uri.parse("content://${ContactsPickerSessionContract.AUTHORITY}/sessions/session_1")
        val expectedInsertContactDataIds = "1,2"
        val expectedInsertCallingUid = callingUid

        fakeContentProvider.insertContactsPickerSessionProviderUri(callingUid, expectedSessionUri)
        val resultUri = repository.createSession(dataIds, callingUid)

        val insertedContentValues =
            fakeContentProvider.getContactsPickerSessionProviderInsertContent(callingUid)
        insertedContentValues?.getAsInteger(
            ContactsPickerSessionContract.Session.SESSION_REQUESTER_UID
        )
        assertThat(
                insertedContentValues?.getAsString(
                    ContactsPickerSessionContract.Session.CONTACT_DATA_IDS
                )
            )
            .isEqualTo(expectedInsertContactDataIds)
        assertThat(
                insertedContentValues?.getAsInteger(
                    ContactsPickerSessionContract.Session.SESSION_REQUESTER_UID
                )
            )
            .isEqualTo(expectedInsertCallingUid)

        assertThat(resultUri).isEqualTo(expectedSessionUri)
    }

    @Test(expected = IllegalArgumentException::class)
    fun createSession_throwsExceptionWhenUriListIsEmpty() = runTest {
        repository.createSession(emptyList(), 12345)
    }
}
