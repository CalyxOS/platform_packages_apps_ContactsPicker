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
package com.android.contactspicker.room.dao

import android.content.Context
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import com.android.contactspicker.data.model.MimeType
import com.android.contactspicker.room.converter.MimeTypeConverter
import com.android.contactspicker.room.database.PrivacyBannerDatabase
import com.android.contactspicker.room.entity.PrivacyBannerShown
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(MockitoJUnitRunner::class)
class PrivacyBannerShownDaoTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private lateinit var db: PrivacyBannerDatabase
    private lateinit var dao: PrivacyBannerShownDao
    private val mimeTypeConverter = MimeTypeConverter()

    private val mimeType1 = MimeType.PHONE
    private val mimeType2 = MimeType.EMAIL

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PrivacyBannerDatabase::class.java).build()
        dao = db.privacyBannerShownDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun wasPrivacyBannerShown_noRecord_returnsFalse() = runBlocking {
        val result = dao.wasPrivacyBannerShown(12345, listOf(mimeType1))
        assertThat(result).isFalse()
    }

    @Test
    fun wasPrivacyBannerShown_recordExists_returnsTrue() = runBlocking {
        val entity = PrivacyBannerShown(appUid = 12345, mimeTypes = listOf(mimeType1))
        dao.insert(entity)
        val result = dao.wasPrivacyBannerShown(12345, listOf(mimeType1))
        assertThat(result).isTrue()
    }

    @Test
    fun wasPrivacyBannerShown_recordExistsWithDifferentAppUid_returnsFalse() = runBlocking {
        val entity = PrivacyBannerShown(appUid = 12345, mimeTypes = listOf(mimeType1))
        dao.insert(entity)
        // Query for a different app UID but the same mime types.
        val result = dao.wasPrivacyBannerShown(54321, listOf(mimeType1))
        assertThat(result).isFalse()
    }

    @Test
    fun wasPrivacyBannerShown_recordExistsWithDifferentMimeTypes_returnsFalse() = runBlocking {
        val entity = PrivacyBannerShown(appUid = 12345, mimeTypes = listOf(mimeType1))
        dao.insert(entity)

        // Query for the same app UID but a different set of mime types.
        val result = dao.wasPrivacyBannerShown(12345, listOf(mimeType2))
        assertThat(result).isFalse()
    }

    @Test
    fun wasPrivacyBannerShown_recordExistsWithSubsetOfMimeTypes_returnsFalse() = runBlocking {
        val entity = PrivacyBannerShown(appUid = 12345, mimeTypes = listOf(mimeType1, mimeType2))
        dao.insert(entity)

        // Query for a subset of the inserted mime types.
        val result = dao.wasPrivacyBannerShown(12345, listOf(mimeType1))
        assertThat(result).isFalse()
    }

    @Test
    fun insert_insertsRecord() = runBlocking {
        val entity = PrivacyBannerShown(appUid = 12345, mimeTypes = listOf(mimeType1))
        assertThat(dao.wasPrivacyBannerShown(12345, listOf(mimeType1))).isFalse()
        dao.insert(entity)
        assertThat(dao.wasPrivacyBannerShown(12345, listOf(mimeType1))).isTrue()
    }

    @Test
    fun insert_duplicate_isIgnored() = runBlocking {
        val appUid = 12345
        val mimeTypes = listOf(mimeType1)
        val entity = PrivacyBannerShown(appUid = appUid, mimeTypes = mimeTypes)

        dao.insert(entity)
        dao.insert(entity)
        assertThat(dao.wasPrivacyBannerShown(12345, listOf(mimeType1))).isTrue()

        val sql = "SELECT COUNT(*) FROM privacy_banner_shown WHERE app_uid = ? AND mime_types = ?"
        val args = arrayOf<Any>(appUid, mimeTypeConverter.fromMimeTypeList(mimeTypes))
        val count =
            db.query(SimpleSQLiteQuery(sql, args)).use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }

        assertThat(count).isEqualTo(1)
    }
}
