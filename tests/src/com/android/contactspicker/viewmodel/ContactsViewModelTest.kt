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

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.android.contactspicker.contact.Contact
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
class ContactsViewModelTest {

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun contacts_areLoaded() = runTest {
        val viewModel = ContactsViewModel()
        val contacts = viewModel.contacts.first()

        val expectedContacts =
            listOf(
                Contact(1, "Alice Wonderland"),
                Contact(2, "Bob The Builder"),
                Contact(3, "Charlie Chaplin"),
                Contact(4, "David Copperfield"),
                Contact(5, "Emily Dickinson"),
                Contact(6, "Frank Sinatra"),
                Contact(7, "Grace Hopper"),
                Contact(8, "Henry Ford"),
                Contact(9, "Ivy Lee"),
                Contact(10, "Jack London"),
            )

        assertThat(contacts).isEqualTo(expectedContacts)
    }
}
