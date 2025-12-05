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

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.android.contactspicker.data.model.MimeType
import com.android.contactspicker.room.dao.PrivacyBannerShownDao
import com.android.contactspicker.room.entity.PrivacyBannerShown
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(MockitoJUnitRunner::class)
class PrivacyBannerRepositoryImplTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Mock private lateinit var mockDao: PrivacyBannerShownDao

    private lateinit var repository: PrivacyBannerRepository

    @Before
    fun setUp() {
        repository = PrivacyBannerRepositoryImpl(mockDao)
    }

    @Test
    fun wasPrivacyBannerShown_callsDaoAndReturnsResult() = runTest {
        val appUid = 12345
        val mimeTypes = listOf(MimeType.PHONE, MimeType.EMAIL)
        whenever(mockDao.wasPrivacyBannerShown(appUid, mimeTypes)).thenReturn(true)

        val result = repository.wasPrivacyBannerShown(appUid, mimeTypes)

        verify(mockDao).wasPrivacyBannerShown(appUid, mimeTypes)
        assertThat(result).isTrue()
    }

    @Test
    fun wasPrivacyBannerShown_unseen_callsDaoInsertWithCorrectEntity() = runTest {
        val appUid = 12345
        val mimeTypes = listOf(MimeType.PHONE, MimeType.EMAIL)
        val expectedEntity = PrivacyBannerShown(appUid = appUid, mimeTypes = mimeTypes)
        whenever(mockDao.wasPrivacyBannerShown(appUid, mimeTypes)).thenReturn(false)

        repository.wasPrivacyBannerShown(appUid, mimeTypes)

        verify(mockDao).insert(expectedEntity)
    }

    @Test
    fun wasPrivacyBannerShown_appUidIsNegativeOne_returnsFallbackAndDoesNotCallDao() = runTest {
        val appUid = -1
        val mimeTypes = listOf(MimeType.PHONE, MimeType.EMAIL)

        val result = repository.wasPrivacyBannerShown(appUid, mimeTypes)

        assertThat(result).isFalse()
        verify(mockDao, never()).wasPrivacyBannerShown(any(), any())
        verify(mockDao, never()).insert(any())
    }

    @Test
    fun wasPrivacyBannerShown_mimeTypesIsEmpty_returnsFallbackAndDoesNotCallDao() = runTest {
        val appUid = 12345
        val mimeTypes = emptyList<MimeType>()

        val result = repository.wasPrivacyBannerShown(appUid, mimeTypes)

        assertThat(result).isFalse()
        verify(mockDao, never()).wasPrivacyBannerShown(any(), any())
        verify(mockDao, never()).insert(any())
    }

    @Test
    fun wasPrivacyBannerShown_daoThrowsException_returnsFallbackAndLogsError() = runTest {
        val appUid = 12345
        val mimeTypes = listOf(MimeType.PHONE, MimeType.EMAIL)
        whenever(mockDao.wasPrivacyBannerShown(appUid, mimeTypes)).thenThrow(RuntimeException())

        val result = repository.wasPrivacyBannerShown(appUid, mimeTypes)

        assertThat(result).isFalse()
        verify(mockDao).wasPrivacyBannerShown(appUid, mimeTypes)
        verify(mockDao, never()).insert(any())
    }
}
