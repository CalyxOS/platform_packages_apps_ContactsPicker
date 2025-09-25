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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.ui.pickerscreen.ContactsList
import com.android.contactspicker.ui.pickerscreen.TopBar
import kotlinx.coroutines.launch

internal const val BOTTOM_SHEET_TEST_TAG = "bottom_sheet"
internal const val BOTTOM_SHEET_LOADING_INDICATOR_TEST_TAG = "bottom_sheet_loading_indicator"
internal const val BOTTOM_SHEET_PEEK_HEIGHT_RATIO = 0.75f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsPickerBottomSheet(onDismissRequest: () -> Unit, uiState: ContactsUiState) {
    val peekHeight = LocalConfiguration.current.screenHeightDp.dp * BOTTOM_SHEET_PEEK_HEIGHT_RATIO
    val bottomSheetState =
        rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false,
        )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    val scope = rememberCoroutineScope()
    LaunchedEffect(bottomSheetState.currentValue) {
        if (bottomSheetState.currentValue == SheetValue.Hidden) {
            onDismissRequest()
        }
    }

    BottomSheetScaffold(
        modifier =
            Modifier.windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetShape = MaterialTheme.shapes.extraLarge,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetShadowElevation = 8.dp,
        containerColor = Color.Transparent,
        sheetContent = {
            Column(
                modifier =
                    Modifier.fillMaxSize().padding(vertical = 8.dp).testTag(BOTTOM_SHEET_TEST_TAG),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (val state = uiState) {
                    is ContactsUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.testTag(BOTTOM_SHEET_LOADING_INDICATOR_TEST_TAG)
                            )
                        }
                    }

                    is ContactsUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    is ContactsUiState.Success -> {
                        TopBar(
                            onSearchBarToggled = { isExpanded ->
                                if (isExpanded) {
                                    scope.launch { bottomSheetState.expand() }
                                }
                            }
                        )
                        ContactsList(contacts = state.contacts, displayMode = state.displayMode)
                    }
                }
            }
        },
    ) { /* Empty content of the screen that appears behind the bottom sheet. */
    }
}
