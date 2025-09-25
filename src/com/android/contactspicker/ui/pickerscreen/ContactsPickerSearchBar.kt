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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.contactspicker.R

private val CollapsedSearchBarPaddingValues =
    PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsPickerSearchBar(
    modifier: Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {

    var query by remember { mutableStateOf("") }

    SearchBar(
        modifier =
            if (expanded) modifier.fillMaxWidth()
            else modifier.padding(CollapsedSearchBarPaddingValues),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = { onExpandedChange(false) },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                placeholder = {
                    Text(stringResource(R.string.contacts_picker_top_bar_search_placeholder_hint))
                },
                leadingIcon = {
                    SearchBarLeadingIcon(
                        expanded = expanded,
                        onExpanded = onExpandedChange,
                        onSearchQueryChanged = { query = it },
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
            onExpandedChange(false)
            query = ""
        }
    }
}

@Composable
private fun SearchBarLeadingIcon(
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
) {
    if (expanded) {
        IconButton(
            onClick = {
                onExpanded(false)
                onSearchQueryChanged("")
            }
        ) {
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
