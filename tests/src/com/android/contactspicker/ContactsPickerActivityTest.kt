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

import android.content.Context
import android.content.Intent
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactsPickerActivityTest {

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
    fun intent_contactPicketFlagDisabled_throws() {
        val intent = Intent(context, ContactsPickerActivity::class.java)

        assertThrows(RuntimeException::class.java) {
            ActivityScenario.launch<ContactsPickerActivity>(intent)
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
    fun intent_contactPicketFlagEnabled_startsActivity() {
        val intent = Intent(context, ContactsPickerActivity::class.java)
        ActivityScenario.launch<ContactsPickerActivity>(intent).use { scenario ->
            assertThat(scenario.state).isAnyOf(Lifecycle.State.STARTED, Lifecycle.State.CREATED)
            scenario.close()
        }
    }
}
