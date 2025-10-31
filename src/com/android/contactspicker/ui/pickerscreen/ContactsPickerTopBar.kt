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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.R
import com.android.contactspicker.SearchState
import com.android.contactspicker.data.model.Contact

const val CONTACTS_PICKER_TOP_BAR_PRIVACY_ICON_TEST_TAG = "contacts_picker_top_bar_privacy_icon"

@Composable
fun ContactsPickerTopBar(
    uiState: State<ContactsUiState>,
    onSearchBarToggled: (isExpanded: Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (Long, Long) -> Unit,
    onExitSearch: () -> Unit,
) {
    val isSearchExpanded by remember { derivedStateOf { uiState.value is SearchState } }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            ProfileSwitcher()
            Icon(
                painter = painterResource(id = R.drawable.android_security_privacy),
                contentDescription = stringResource(R.string.privacy_info_content_description),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(CONTACTS_PICKER_TOP_BAR_PRIVACY_ICON_TEST_TAG),
            )
        }
    }
}
