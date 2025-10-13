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

import androidx.collection.LongObjectMap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.contactspicker.data.model.Contact

const val CONTACTS_LIST_TEST_TAG = "contacts_list"

/**
 * Displays the main content of the contact picker, including a privacy banner and a vertically
 * scrollable list of contacts.
 *
 * The contacts are grouped alphabetically by their display name, with a sticky header for each
 * letter.
 *
 * @param contacts The list of [Contact]s to be displayed.
 * @param onPrivacyBannerMoreDetails The callback to be invoked when the "More details" button on
 *   the privacy banner is clicked.
 * @param onPrivacyBannerDismissRequest The callback to be invoked when the "Dismiss" button on the
 *   privacy banner is clicked.
 * @param selectedContacts The map of currently selected contacts, keyed by contact ID.
 * @param onToggleContactSelection A callback invoked when a contact's avatar is clicked.
 * @param onToggleEntrySelection A callback invoked when a single entry row is clicked.
 */
@Composable
fun ContactsPickerBody(
    contacts: List<Contact>,
    onPrivacyBannerMoreDetails: () -> Unit,
    onPrivacyBannerDismissRequest: () -> Unit,
    selectedContacts: LongObjectMap<Set<Long>>,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (contactId: Long, entryId: Long) -> Unit,
) {
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
    LazyColumn(modifier = Modifier.fillMaxWidth().testTag(CONTACTS_LIST_TEST_TAG)) {
        item(key = "privacy_banner") {
            PrivacyBanner(
                onMoreDetails = onPrivacyBannerMoreDetails,
                onDismissRequest = onPrivacyBannerDismissRequest,
            )
        }
        groupedContacts.forEach { (letter, contactsInGroup) ->
            stickyHeader(key = "header_$letter") { SectionHeader(letter = letter) }
            items(items = contactsInGroup, key = { contact -> contact.id }) { contact ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ContactItem(
                        contact = contact,
                        selectedEntries = selectedContacts[contact.id],
                        onToggleContactSelection = onToggleContactSelection,
                        onToggleEntrySelection = onToggleEntrySelection,
                    )
                }
            }
        }
    }
}
