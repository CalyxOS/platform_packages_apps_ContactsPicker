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
package com.android.contactspicker

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SetActivityResultTest {

    private lateinit var context: Context
    private val testUri1: Uri = Uri.parse("content://com.android.contacts/contacts/1")
    private val testUri2: Uri = Uri.parse("content://com.android.contacts/contacts/2")
    private val callingUid = 1000

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun createSingleSelectionResult_withPickAction_returnsIntentWithData() {
        val intent = Intent(Intent.ACTION_PICK)
        val resultIntent = createSingleSelectionResult(context, intent, testUri1, callingUid)

        assertThat(resultIntent.data).isEqualTo(testUri1)
        assertThat(resultIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotEqualTo(0)
    }

    @Test
    fun createSingleSelectionResult_withWrongAction_throwsException() {
        val intent = Intent(Intent.ACTION_VIEW)
        assertThrows(IllegalArgumentException::class.java) {
            createSingleSelectionResult(context, intent, testUri1, callingUid)
        }
    }

    @Test
    fun createMultiSelectionResult_withPickAction_returnsIntentWithClipData() {
        val mockContentResolver = mock(ContentResolver::class.java)
        val mockContext = mock(Context::class.java)
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)

        val intent = Intent(Intent.ACTION_PICK)
        val uris = listOf(testUri1, testUri2)
        val resultIntent = createMultiSelectionResult(mockContext, intent, uris, callingUid)

        assertThat(resultIntent.clipData!!.itemCount).isEqualTo(2)
        assertThat(resultIntent.clipData!!.getItemAt(0).uri).isEqualTo(testUri1)
        assertThat(resultIntent.clipData!!.getItemAt(1).uri).isEqualTo(testUri2)
        assertThat(resultIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotEqualTo(0)
    }

    @Test
    fun createMultiSelectionResult_withWrongAction_throwsException() {
        val intent = Intent(Intent.ACTION_VIEW)
        val uris = listOf(testUri1, testUri2)
        assertThrows(IllegalArgumentException::class.java) {
            createMultiSelectionResult(context, intent, uris, callingUid)
        }
    }

    @Test
    fun createMultiSelectionResult_withEmptyUris_throwsException() {
        val intent = Intent(Intent.ACTION_VIEW)
        val uris = listOf<Uri>()
        assertThrows(IllegalArgumentException::class.java) {
            createMultiSelectionResult(context, intent, uris, callingUid)
        }
    }
}
