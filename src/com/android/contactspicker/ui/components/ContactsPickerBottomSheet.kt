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
package com.android.contactspicker.ui.components

import android.widget.Toast
import androidx.collection.LongObjectMap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.navigation.ContactsPickerNavHost
import com.android.contactspicker.navigation.ContactsPickerRoute
import com.android.contactspicker.ui.pickerscreen.SelectionBottomBar
import kotlinx.coroutines.launch

internal const val BOTTOM_SHEET_TEST_TAG = "bottom_sheet"
internal const val SCRIM_TEST_TAG = "scrim"

internal const val BOTTOM_SHEET_PEEK_HEIGHT_RATIO = 0.75f
private const val SCRIM_ALPHA = 0.32f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsPickerBottomSheet(
    onDismissRequest: () -> Unit,
    uiState: State<ContactsUiState>,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (Long, Long) -> Unit,
    onClearSelection: () -> Unit,
    bottomSheetState: SheetState =
        rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false,
        ),
) {
    val peekHeight = LocalConfiguration.current.screenHeightDp.dp * BOTTOM_SHEET_PEEK_HEIGHT_RATIO
    val navController = rememberNavController()
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val uiStateValue = uiState.value

    LaunchedEffect(bottomSheetState.currentValue) {
        if (bottomSheetState.currentValue == SheetValue.Hidden) {
            onDismissRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Draw a scrim behind the scaffold when the sheet is not hidden
        if (bottomSheetState.currentValue != SheetValue.Hidden) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
                        .testTag(SCRIM_TEST_TAG)
            )
        }
        BottomSheetScaffold(
            modifier =
                Modifier.windowInsetsPadding(
                    WindowInsets.statusBars.union(WindowInsets.displayCutout)
                ),
            scaffoldState = scaffoldState,
            sheetPeekHeight = peekHeight,
            sheetContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            sheetContentColor = MaterialTheme.colorScheme.onSurface,
            sheetShadowElevation = 8.dp,
            containerColor = Color.Transparent,
            sheetContent = {
                ContactsPickerNavHost(
                    navController = navController,
                    uiState = uiState,
                    onToggleContactSelection = onToggleContactSelection,
                    onToggleEntrySelection = onToggleEntrySelection,
                    onExpandRequest = { scope.launch { bottomSheetState.expand() } },
                    modifier = Modifier.testTag(BOTTOM_SHEET_TEST_TAG),
                )
            },
        ) { /* Empty content of the screen that appears behind the bottom sheet. */
        }

        if (uiStateValue is ContactsUiState.Success) {
            AnimatedSelectionBottomBar(
                visible =
                    uiStateValue.selectedContacts.isNotEmpty() &&
                        currentRoute == ContactsPickerRoute.route,
                selectedContactsCount = uiStateValue.selectedContacts.totalElementsSize(),
                onClearSelection = onClearSelection,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun AnimatedSelectionBottomBar(
    visible: Boolean,
    selectedContactsCount: Int,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier.padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        SelectionBottomBar(
            selectedCount = selectedContactsCount,
            onPreviewClick = {
                // TODO(b/441480198): Navigate to the preview screen
                Toast.makeText(context, "Preview clicked", Toast.LENGTH_SHORT).show()
            },
            onDoneClick = {
                // TODO(b/452020365): Pass the selected contacts to the caller
                Toast.makeText(context, "Done clicked", Toast.LENGTH_SHORT).show()
            },
            onClearSelection = onClearSelection,
        )
    }
}

/** Calculates the total number of items across all value sets in a LongObjectMap. */
private fun LongObjectMap<Set<Long>>.totalElementsSize(): Int {
    var count = 0
    this.forEachValue { value -> count += value.size }
    return count
}
