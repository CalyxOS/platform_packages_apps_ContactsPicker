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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.R
import com.android.contactspicker.SearchState
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.PickerUserState
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.ui.components.PrivacyShieldIcon

const val CONTACTS_PICKER_TOP_BAR_MORE_VERTICAL_ICON_TEST_TAG =
    "contacts_picker_top_bar_more_vertical_icon"

@Composable
fun ContactsPickerTopBar(
    uiState: State<ContactsUiState>,
    userState: PickerUserState,
    onSearchBarToggled: (isExpanded: Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (Long, Long) -> Unit,
    onExitSearch: () -> Unit,
    onShowPrivacyDetailsClick: () -> Unit,
    onProfileClicked: (UserProfile) -> Unit,
) {
    val isSearchExpanded by remember { derivedStateOf { uiState.value is SearchState } }
    Row(
        modifier =
            Modifier.fillMaxWidth().padding(horizontal = if (isSearchExpanded) 4.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            if (isSearchExpanded) Arrangement.spacedBy(0.dp) else Arrangement.spacedBy(8.dp),
    ) {
        ContactsPickerSearchBar(
            modifier = Modifier.weight(1.0f),
            expanded = isSearchExpanded,
            uiState = uiState,
            onExpandedChange = onSearchBarToggled,
            onQueryChange = onQueryChange,
            onToggleContactSelection = onToggleContactSelection,
            onToggleEntrySelection = onToggleEntrySelection,
            onExitSearch = onExitSearch,
        )
        if (!isSearchExpanded) {
            ProfileSwitcher(userState = userState, onProfileClicked = onProfileClicked)
            OverflowMenu(onClickPrivacyDetailsMenuItem = onShowPrivacyDetailsClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverflowMenu(onClickPrivacyDetailsMenuItem: () -> Unit) {
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }

    Box {
        TooltipBox(
            positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above
                ),
            tooltip = {
                Text(
                    text =
                        stringResource(
                            R.string.contacts_picker_top_bar_more_options_content_description
                        )
                )
            },
            state = rememberTooltipState(),
        ) {
            IconButton(
                onClick = { showOverflowMenu = true },
                modifier = Modifier.testTag(CONTACTS_PICKER_TOP_BAR_MORE_VERTICAL_ICON_TEST_TAG),
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription =
                        stringResource(
                            R.string.contacts_picker_top_bar_more_options_content_description
                        ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
            DropdownMenuItem(
                onClick = {
                    showOverflowMenu = false
                    onClickPrivacyDetailsMenuItem()
                },
                text = { Text(stringResource(R.string.privacy_details_menu_label)) },
                leadingIcon = { PrivacyShieldIcon() },
            )
        }
    }
}
