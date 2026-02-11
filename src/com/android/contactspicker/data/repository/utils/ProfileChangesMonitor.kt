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
import androidx.annotation.OpenForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Monitors changes in user profile availability (e.g. Work Profile turned on/off) by listening to
 * system broadcasts.
 */
@OpenForTesting
open class ProfileChangesMonitor
@Inject
constructor(@param:ApplicationContext private val context: Context) {

    open fun getProfileChangeFlow(): Flow<Unit> = callbackFlow {
        val profileChangeReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    trySend(Unit)
                }
            }

        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PROFILE_ADDED)
                addAction(Intent.ACTION_PROFILE_REMOVED)
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            }

        // Register receiver across all users to ensure we get updates even if the app is running
        // in a secondary user (e.g. Work Profile) but needs to hear about changes in others (e.g.
        // Personal).
        context.registerReceiverAsUser(
            profileChangeReceiver,
            UserHandle.ALL,
            filter,
            null,
            null,
            Context.RECEIVER_NOT_EXPORTED,
        )

        awaitClose { context.unregisterReceiver(profileChangeReceiver) }
    }
}
