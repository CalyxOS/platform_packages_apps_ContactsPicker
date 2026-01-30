/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.contactspicker.data.repository.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ProfileChangesMonitorTest {

    private val mockContext: Context = mock()
    private lateinit var monitor: ProfileChangesMonitor

    @Before
    fun setUp() {
        monitor = ProfileChangesMonitor(mockContext)
    }

    @Test
    fun getProfileChangeFlow_registersReceiverWithCorrectActions() = runTest {
        backgroundScope.launch { monitor.getProfileChangeFlow().collect {} }
        runCurrent()

        val filter = verifyAndCaptureFilter()
        val actions = filter.actionsIterator().asSequence().toSet()
        assertThat(actions)
            .containsExactly(
                Intent.ACTION_PROFILE_ADDED,
                Intent.ACTION_PROFILE_REMOVED,
                Intent.ACTION_PROFILE_AVAILABLE,
                Intent.ACTION_PROFILE_UNAVAILABLE,
            )
    }

    @Test
    fun getProfileChangeFlow_emitsOnBroadcast() = runTest {
        var emitted = false

        backgroundScope.launch { monitor.getProfileChangeFlow().collect { emitted = true } }
        runCurrent()

        val receiver = verifyAndCaptureReceiver()
        receiver.onReceive(mockContext, Intent(Intent.ACTION_MANAGED_PROFILE_ADDED))
        runCurrent()

        assertThat(emitted).isTrue()
    }

    @Test
    fun getProfileChangeFlow_unregistersReceiverOnClose() = runTest {
        val job = launch { monitor.getProfileChangeFlow().collect {} }
        runCurrent()

        val receiver = verifyAndCaptureReceiver()

        job.cancel()
        runCurrent()

        verify(mockContext).unregisterReceiver(receiver)
    }

    private fun verifyAndCaptureFilter(): IntentFilter {
        val filterCaptor = argumentCaptor<IntentFilter>()
        verify(mockContext)
            .registerReceiverAsUser(
                any(),
                eq(UserHandle.ALL),
                filterCaptor.capture(),
                eq(null),
                eq(null),
                eq(Context.RECEIVER_NOT_EXPORTED),
            )
        assertThat(filterCaptor.allValues).hasSize(1)
        return filterCaptor.firstValue
    }

    private fun verifyAndCaptureReceiver(): BroadcastReceiver {
        val receiverCaptor = argumentCaptor<BroadcastReceiver>()
        verify(mockContext)
            .registerReceiverAsUser(
                receiverCaptor.capture(),
                eq(UserHandle.ALL),
                any(),
                eq(null),
                eq(null),
                eq(Context.RECEIVER_NOT_EXPORTED),
            )
        assertThat(receiverCaptor.allValues).hasSize(1)
        return receiverCaptor.firstValue
    }
}
