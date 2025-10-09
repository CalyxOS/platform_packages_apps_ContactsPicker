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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.android.contactspicker.ui.components.ContactsPickerBottomSheet
import com.android.contactspicker.ui.theme.ContactsPickerAppTheme
import com.android.contactspicker.viewmodel.ContactsViewModel
import dagger.hilt.android.AndroidEntryPoint

/** The main activity for the Contacts Picker system app. */
@AndroidEntryPoint(ComponentActivity::class)
class ContactsPickerActivity : Hilt_ContactsPickerActivity() {

    private val contactsViewModel: ContactsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            contactsViewModel.processIntent(intent.action, intent.type)
        }

        setContent {
            val uiState by contactsViewModel.uiState.collectAsState()
            ContactsPickerAppTheme {
                ContactsPickerBottomSheet(onDismissRequest = { finish() }, uiState = uiState)
            }
        }
    }
}
