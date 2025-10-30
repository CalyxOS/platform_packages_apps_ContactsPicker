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

import androidx.annotation.VisibleForTesting
import androidx.collection.LongObjectMap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.contactspicker.R
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
 * @param isMultiSelectEnabled Whether selecting multiple contacts is enabled.
 * @param onToggleContactSelection A callback invoked when a contact's avatar is clicked.
 * @param onToggleEntrySelection A callback invoked when a single entry row is clicked.
 */
@Composable
fun ContactsPickerBody(
    contacts: List<Contact>,
    callingAppName: String?,
    onPrivacyBannerMoreDetails: () -> Unit,
    onPrivacyBannerDismissRequest: () -> Unit,
    selectedContacts: LongObjectMap<Set<Long>>,
    isMultiSelectEnabled: Boolean,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (contactId: Long, entryId: Long) -> Unit,
) {
    val emojiHeaderContentDesc = stringResource(R.string.emoji_header_icon_content_description)
    val groupedContacts =
        remember(contacts) {
            // TODO(b/436818961): consider moving the grouping logic to the view models
            contacts.groupBy {
                val firstChar = it.displayName.firstOrNull()
                if (firstChar?.isLetter() == true) {
                    SectionKey.LetterKey(firstChar.uppercaseChar())
                } else {
                    SectionKey.IconKey(Icons.Default.Mood, emojiHeaderContentDesc)
                }
            }
        }

    val favoriteContacts = remember(contacts) { contacts.filter { it.isFavorite } }

    LazyColumn(modifier = Modifier.fillMaxWidth().testTag(CONTACTS_LIST_TEST_TAG)) {
        item(key = "privacy_banner") {
            PrivacyBanner(
                callingAppName = callingAppName,
                onMoreDetails = onPrivacyBannerMoreDetails,
                onDismissRequest = onPrivacyBannerDismissRequest,
            )
        }

        favoritesSection(
            favoriteContacts = favoriteContacts,
            selectedContacts = selectedContacts,
            isMultiSelectEnabled = isMultiSelectEnabled,
            onToggleContactSelection = onToggleContactSelection,
            onToggleEntrySelection = onToggleEntrySelection,
        )

        groupedContacts.forEach { (sectionKey, contactsInGroup) ->
            stickyHeader(key = "header_$sectionKey") {
                when (sectionKey) {
                    is SectionKey.LetterKey -> SectionHeader(sectionKey.letter)
                    is SectionKey.IconKey ->
                        SectionHeader(sectionKey.icon, sectionKey.contentDescription)
                }
            }
            val groupSize = contactsInGroup.size
            itemsIndexed(items = contactsInGroup, key = { _, contact -> contact.id }) {
                index,
                contact ->
                val position = itemPosition(index, groupSize)

                val bottomPadding =
                    if (position == ItemPosition.LAST || position == ItemPosition.ONLY) 8.dp
                    else 1.dp

                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = bottomPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ContactItem(
                        contact = contact,
                        position = position,
                        selectedEntries = selectedContacts[contact.id],
                        isMultiSelectEnabled = isMultiSelectEnabled,
                        onToggleContactSelection = onToggleContactSelection,
                        onToggleEntrySelection = onToggleEntrySelection,
                    )
                }
            }
        }
    }
}

private fun itemPosition(index: Int, groupSize: Int): ItemPosition {
    return when {
        groupSize == 1 -> ItemPosition.ONLY
        index == 0 -> ItemPosition.FIRST
        index == groupSize - 1 -> ItemPosition.LAST
        else -> ItemPosition.MIDDLE
    }
}

/** A helper function to display the "Favorites" section in the LazyColumn. */
private fun LazyListScope.favoritesSection(
    favoriteContacts: List<Contact>,
    selectedContacts: LongObjectMap<Set<Long>>,
    isMultiSelectEnabled: Boolean,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (contactId: Long, entryId: Long) -> Unit,
) {
    if (favoriteContacts.isNotEmpty()) {
        stickyHeader(key = "header_favorites") {
            SectionHeader(
                imageVector = Icons.Filled.Star,
                iconContentDescription =
                    stringResource(R.string.favorites_header_icon_content_description),
                text = stringResource(R.string.contacts_picker_favorites_header),
            )
        }
        itemsIndexed(items = favoriteContacts, key = { _, contact -> "fav-${contact.id}" }) {
            index,
            contact ->
            val position = itemPosition(index, favoriteContacts.size)

            val bottomPadding =
                if (position == ItemPosition.LAST || position == ItemPosition.ONLY) 8.dp else 1.dp
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = bottomPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ContactItem(
                    contact = contact,
                    position = position,
                    selectedEntries = selectedContacts[contact.id],
                    isMultiSelectEnabled = isMultiSelectEnabled,
                    onToggleContactSelection = onToggleContactSelection,
                    onToggleEntrySelection = onToggleEntrySelection,
                )
            }
        }
    }
}

// A sealed class to represent the key for each section.
// It implements Comparable to define a custom sorting order.
@VisibleForTesting
internal sealed class SectionKey : Comparable<SectionKey> {
    data class IconKey(val icon: ImageVector, val contentDescription: String) : SectionKey() {
        // Icon section should always come first.
        override fun compareTo(other: SectionKey): Int {
            // note: this is assuming only one ImageVector in the list so the order is undefined
            return if (other is IconKey) 0 else -1
        }
    }

    data class LetterKey(val letter: Char) : SectionKey() {
        override fun compareTo(other: SectionKey): Int {
            return when (other) {
                is IconKey -> 1 // Letter sections come after icon sections.
                is LetterKey -> letter.compareTo(other.letter)
            }
        }
    }
}
