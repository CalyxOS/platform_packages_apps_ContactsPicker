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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.contactspicker.R
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.ContactsSelection
import com.android.contactspicker.data.model.SectionKey
import com.android.contactspicker.data.model.SectionKey.EmojiSection
import com.android.contactspicker.data.model.SectionKey.FavoriteSection
import com.android.contactspicker.data.model.SectionKey.LetterKey
import com.android.contactspicker.data.model.SectionKey.StringKey
import com.android.contactspicker.data.model.SelectionSource
import com.android.contactspicker.ui.scrubber.AnimatedScrubber
import com.android.contactspicker.ui.scrubber.ScrubberController
import com.android.contactspicker.ui.scrubber.ScrubberLabel
import com.android.contactspicker.ui.scrubber.rememberScrubberController
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.collectLatest

const val CONTACTS_LIST_TEST_TAG = "contacts_list"
const val MIN_CONTACTS_COUNT_FOR_SCRUBBER_ACTIVATION = 50

// SelectionBottomBar height (56dp) + bottom padding (24dp) + clearance (16dp)
private val SELECTION_BAR_HEIGHT_SPACE = 96.dp

/**
 * Displays the main content of the contact picker, including a privacy banner and a vertically
 * scrollable list of contacts.
 *
 * The contacts are grouped into sections (Favorites, Non-Letter/Emoji, and Alphabetical) determined
 * by their [SectionKey]. The sections are displayed in the sort order defined by the [SectionKey]s.
 * Each section has a sticky header.
 *
 * @param availableContactsGroups The contacts to be displayed, grouped by section headers. It also
 *   contains the favorite contacts list, and it is ordered as expected in UI.
 * @param callingAppName The name of the app requesting the contacts, used in the privacy banner.
 * @param showPrivacyBanner Whether to display the privacy banner at the top of the list.
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
    availableContactsGroups: Map<SectionKey, List<Contact>>,
    callingAppName: String?,
    showPrivacyBanner: Boolean,
    onPrivacyBannerMoreDetails: () -> Unit,
    onPrivacyBannerDismissRequest: () -> Unit,
    selectedContacts: ContactsSelection,
    isMultiSelectEnabled: Boolean,
    onToggleContactSelection: (Contact, SelectionSource) -> Unit,
    onToggleEntrySelection: (contactId: Long, entryId: Long, SelectionSource) -> Unit,
) {
    val listState = rememberLazyListState()
    val totalContactsCount =
        remember(availableContactsGroups) {
            availableContactsGroups.filterKeys { it !is FavoriteSection }.values.sumOf { it.size }
        }
    val showScrubber = totalContactsCount >= MIN_CONTACTS_COUNT_FOR_SCRUBBER_ACTIVATION

    val scrubberController = rememberScrubberController(availableContactsGroups, showPrivacyBanner)

    val isListScrollEnabled =
        if (showScrubber) {
            scrubberController.isListScrollEnabledForUser
        } else {
            true
        }

    Box(modifier = Modifier.fillMaxWidth()) {
        ContactsList(
            listState = listState,
            availableContactsGroups = availableContactsGroups,
            userScrollEnabled = isListScrollEnabled,
            showPrivacyBanner = showPrivacyBanner,
            callingAppName = callingAppName,
            onPrivacyBannerMoreDetails = onPrivacyBannerMoreDetails,
            onPrivacyBannerDismissRequest = onPrivacyBannerDismissRequest,
            selectedContacts = selectedContacts,
            isMultiSelectEnabled = isMultiSelectEnabled,
            onToggleContactSelection = onToggleContactSelection,
            onToggleEntrySelection = onToggleEntrySelection,
        )
        if (showScrubber) {
            ScrubberListSynchronizationEffects(scrubberController, listState)

            AnimatedScrubber(
                scrubberController.scrubberState,
                listState,
                Modifier.fillMaxHeight(),
                label = { ScrubberLabel(scrubberController.labelSectionKey) },
            )
        }
    }
}

@Composable
private fun ContactsList(
    listState: LazyListState,
    availableContactsGroups: Map<SectionKey, List<Contact>>,
    userScrollEnabled: Boolean,
    showPrivacyBanner: Boolean,
    callingAppName: String?,
    onPrivacyBannerMoreDetails: () -> Unit,
    onPrivacyBannerDismissRequest: () -> Unit,
    selectedContacts: ContactsSelection,
    isMultiSelectEnabled: Boolean,
    onToggleContactSelection: (Contact, SelectionSource) -> Unit,
    onToggleEntrySelection: (Long, Long, SelectionSource) -> Unit,
) {

    val layoutDirection = LocalLayoutDirection.current
    val navBarsPadding = WindowInsets.navigationBars.asPaddingValues()

    // Calculate the total bottom padding: System Bars + Selection Bar (if visible)
    val extraBottomPadding by
        animateDpAsState(
            targetValue = if (selectedContacts.isNotEmpty()) SELECTION_BAR_HEIGHT_SPACE else 0.dp,
            label = "SelectionBarBottomPadding",
        )
    val totalBottomPadding = navBarsPadding.calculateBottomPadding() + extraBottomPadding

    val listContentPadding =
        PaddingValues(
            start = navBarsPadding.calculateStartPadding(layoutDirection),
            end = navBarsPadding.calculateEndPadding(layoutDirection),
            top = navBarsPadding.calculateTopPadding(),
            bottom = totalBottomPadding,
        )

    LazyColumn(
        state = listState,
        userScrollEnabled = userScrollEnabled,
        modifier = Modifier.fillMaxWidth().testTag(CONTACTS_LIST_TEST_TAG),
        contentPadding = listContentPadding,
    ) {
        item(key = "privacy_banner") {
            PrivacyBanner(
                visible = showPrivacyBanner,
                callingAppName = callingAppName,
                onDismissRequest = onPrivacyBannerDismissRequest,
                onMoreDetails = onPrivacyBannerMoreDetails,
            )
        }
        availableContactsGroups.forEach { (sectionKey, contactsInGroup) ->
            stickyHeader(key = "header_${sectionKey.uniqueId}") { SectionHeaderForKey(sectionKey) }

            val currentSource =
                if (sectionKey is FavoriteSection) {
                    SelectionSource.FAVORITES
                } else {
                    SelectionSource.MAIN_LIST
                }
            val groupSize = contactsInGroup.size
            itemsIndexed(
                items = contactsInGroup,
                // The key must be unique across the entire list.
                // Since a contact that appears in 'Favorites' will appear in another
                // section, prefix the key with the section ID.
                key = { _, contact -> "${sectionKey.uniqueId}_${contact.id}" },
            ) { index, contact ->
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
                        isSearchMode = false,
                        onToggleContactSelection = { c ->
                            onToggleContactSelection(c, currentSource)
                        },
                        onToggleEntrySelection = { cId, eId ->
                            onToggleEntrySelection(cId, eId, currentSource)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrubberListSynchronizationEffects(
    scrubberController: ScrubberController,
    listState: LazyListState,
) {
    // Scrubber -> List: Collects scroll requests upon scrubber position change from
    // [ListScrubberMediator.scrollRequests] and triggers [LazyListState.scrollToItem] to move the
    // list
    LaunchedEffect(scrubberController, listState) {
        scrubberController.scrollRequests.collectLatest { index -> listState.scrollToItem(index) }
    }

    // List -> Scrubber: Tracks index of the first visible item plus the fraction of that item that
    // has been scrolled past the top edge of the viewport and updates the scrubber handle position
    // based on that.
    LaunchedEffect(scrubberController, listState) {
        snapshotFlow { listState.getFractionalFirstVisibleItemIndex() }
            .collectLatest { fractionalFirstVisibleItemIndex ->
                scrubberController.updateVerticalOffsetFraction(
                    preciseListIndex = fractionalFirstVisibleItemIndex
                )
            }
    }
}

/**
 * Calculates the precise scroll position of the list as a fractional index.
 *
 * This value represents the index of the first visible item plus the fraction of that item that has
 * been scrolled past the top edge of the viewport.
 *
 * For example:
 * - If the item at index 2 is aligned with the top edge, the result is `2.0f`.
 * - If the item at index 2 is scrolled halfway off the screen, the result is `2.5f`.
 *
 * This is useful for synchronizing UI elements (like a scrubber or scrollbar) with the list's exact
 * scroll position.
 *
 * @return A [Float] representing the fractional index, or `0f` if the list is empty.
 */
private fun LazyListState.getFractionalFirstVisibleItemIndex(): Float {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return 0f

    val firstItem = visibleItems.find { it.index == firstVisibleItemIndex } ?: return 0f

    val firstItemSize = firstItem.size
    val offsetFraction =
        if (firstItemSize > 0) {
            firstItem.offset.absoluteValue.toFloat() / firstItemSize
        } else {
            0f
        }

    return firstItem.index + offsetFraction
}

// TODO(b/489972870): Validate and remove SectionHeader/ScrubberLabel duplication in UI when
// handling SectionKey
@Composable
private fun SectionHeaderForKey(sectionKey: SectionKey) {
    when (sectionKey) {
        is LetterKey -> {
            SectionHeader(sectionKey.letter.toString())
        }
        is StringKey -> {
            SectionHeader(sectionKey.header)
        }
        is FavoriteSection -> {
            SectionHeader(
                imageVector = Icons.Filled.Star,
                iconContentDescription =
                    stringResource(R.string.favorites_header_icon_content_description),
                text = stringResource(R.string.contacts_picker_favorites_header),
                modifier = Modifier.semantics { hideFromAccessibility() },
            )
        }
        is EmojiSection -> {
            SectionHeader(
                imageVector = Icons.Filled.Mood,
                iconContentDescription =
                    stringResource(R.string.emoji_header_icon_content_description),
            )
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
