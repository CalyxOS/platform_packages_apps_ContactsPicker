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
package com.android.contactspicker.ui.pickerscreen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.contactspicker.data.model.Contact

/**
 * A composable that displays a list of contacts, grouped by the first letter of their display name.
 *
 * @param contactsViewModel The view model that provides the list of contacts.
 */
@Composable
fun ContactsList(contacts: List<Contact>) {
    val groupedContacts =
        remember(contacts) {
            // TODO(b/436818961): consider moving the grouping logic to the view models
            contacts.groupBy {
                val firstChar = it.displayName.firstOrNull()
                if (firstChar?.isLetter() == true) {
                    firstChar.uppercaseChar()
                } else {
                    // TODO(b/442808599): make fallback for the grouping an emoji as per mocks
                    '#'
                }
            }
        }
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        groupedContacts.forEach { (letter, contactsInGroup) ->
            stickyHeader(key = "header_$letter") { SectionHeader(letter = letter) }
            items(items = contactsInGroup, key = { contact -> contact.id }) { contact ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ContactItem(contact = contact)
                }
            }
        }
    }
}
