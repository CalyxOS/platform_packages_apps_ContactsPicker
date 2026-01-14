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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.R
import com.android.contactspicker.SearchState
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.PhoneContact

private val CollapsedSearchBarPaddingValues =
    PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsPickerSearchBar(
    modifier: Modifier,
    expanded: Boolean,
    uiState: State<ContactsUiState>,
    onExpandedChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (Long, Long) -> Unit,
    onExitSearch: () -> Unit,
) {

    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    SearchBar(
        modifier =
            if (expanded) modifier.fillMaxWidth()
            else modifier.padding(CollapsedSearchBarPaddingValues),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = {
                    query = it
                    onQueryChange(it)
                },
                onSearch = { keyboardController?.hide() },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                placeholder = {
                    Text(stringResource(R.string.contacts_picker_top_bar_search_placeholder_hint))
                },
                leadingIcon = {
                    SearchBarLeadingIcon(
                        expanded = expanded,
                        onExitSearch = {
                            query = ""
                            onExitSearch()
                        },
                    )
                },
                trailingIcon = {
                    if (expanded) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription =
                                    stringResource(
                                        id =
                                            R.string
                                                .contacts_picker_top_bar_search_clear_text_content_description
                                    ),
                            )
                        }
                    }
                },
                colors =
                    TextFieldDefaults.colors(
                        unfocusedContainerColor =
                            if (expanded) {
                                MaterialTheme.colorScheme.surfaceContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            )
        },
        colors =
            SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                dividerColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        // SearchBar content when expanded.
        // BackHandler is used to close the search bar when the back button is pressed or back
        // gesture detected.
        BackHandler(enabled = expanded) {
            query = ""
            onExitSearch()
        }

        val uiStateValue = uiState.value
        if (expanded && uiStateValue is SearchState.Success) {
            SearchResultsList(
                searchState = uiStateValue,
                onToggleContactSelection = onToggleContactSelection,
                onToggleEntrySelection = onToggleEntrySelection,
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    searchState: SearchState.Success,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (Long, Long) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(searchState.query) {
        // Whenever the query changes, scroll to the top
        listState.scrollToItem(0)
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().imePadding(), state = listState) {
        val searchResults = searchState.searchResults
        itemsIndexed(
            items = searchResults,
            key = { _, contact ->
                when (contact) {
                    is EmailContact -> "search-email-${contact.emails.firstOrNull()?.id}"
                    is PhoneContact -> "search-phone-${contact.phones.firstOrNull()?.id}"
                    else -> "search-aggregate-${contact.id}"
                }
            },
        ) { index, contact ->
            val position =
                when {
                    searchResults.size == 1 -> ItemPosition.ONLY
                    index == 0 -> ItemPosition.FIRST
                    index == searchResults.size - 1 -> ItemPosition.LAST
                    else -> ItemPosition.MIDDLE
                }
            val bottomPadding =
                if (position == ItemPosition.LAST || position == ItemPosition.ONLY) 8.dp else 1.dp
            val topPadding =
                if (position == ItemPosition.FIRST || position == ItemPosition.ONLY) 24.dp else 0.dp
            Row(
                modifier =
                    Modifier.fillMaxWidth().padding(bottom = bottomPadding, top = topPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ContactItem(
                    contact = contact,
                    position = position,
                    selectedEntries = searchState.selectedContacts[contact.id],
                    isMultiSelectEnabled = false, // Not relevant in search state
                    onToggleContactSelection = onToggleContactSelection,
                    onToggleEntrySelection = onToggleEntrySelection,
                    isSearchMode = true,
                )
            }
        }
    }
}

@Composable
private fun SearchBarLeadingIcon(expanded: Boolean, onExitSearch: () -> Unit) {
    if (expanded) {
        IconButton(onClick = { onExitSearch() }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription =
                    stringResource(
                        id = R.string.contacts_picker_top_bar_search_arrow_back_content_description
                    ),
            )
        }
    } else {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription =
                stringResource(
                    id = R.string.contacts_picker_top_bar_search_icon_content_description
                ),
        )
    }
}
