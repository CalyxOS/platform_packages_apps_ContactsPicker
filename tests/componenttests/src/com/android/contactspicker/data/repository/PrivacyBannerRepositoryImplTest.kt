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
    fun hasPrivacyBannerBeenShown_callsDaoAndReturnsResult() = runTest {
        val appUid = "com.example.app"
        val mimeTypes = listOf("type1", "type2")
        whenever(mockDao.hasPrivacyBannerBeenShown(appUid, mimeTypes)).thenReturn(true)

        val result = repository.hasPrivacyBannerBeenShown(appUid, mimeTypes)

        verify(mockDao).hasPrivacyBannerBeenShown(appUid, mimeTypes)
        assertThat(result).isTrue()
    }

    @Test
    fun markPrivacyBannerShown_callsDaoInsertWithCorrectEntity() = runTest {
        val appUid = "com.example.app"
        val mimeTypes = listOf("type1", "type2")
        val expectedEntity = PrivacyBannerShown(appUid = appUid, mimeTypes = mimeTypes)

        repository.markPrivacyBannerShown(appUid, mimeTypes)

        verify(mockDao).insert(expectedEntity)
    }
}
