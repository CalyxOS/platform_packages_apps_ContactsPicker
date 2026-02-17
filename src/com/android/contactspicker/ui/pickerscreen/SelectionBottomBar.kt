/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.contactspicker.R

/**
 * A floating bar that appears at the bottom of the screen to show the number of selected contacts
 * and provide actions.
 *
 * @param selectedCount The number of currently selected items.
 * @param isPreviewMode Whether the picker is currently in preview mode.
 * @param onPreviewClicked A callback for when the preview button is clicked.
 * @param onDoneClick A callback for when the done button is clicked.
 * @param onClearSelection A callback for when the clear selection button is clicked.
 * @param onBackFromPreview A callback for when the back button is clicked while in preview mode.
 */
@Composable
fun SelectionBottomBar(
    selectedCount: Int,
    isPreviewMode: Boolean,
    onPreviewClicked: () -> Unit,
    onDoneClick: () -> Unit,
    onClearSelection: () -> Unit,
    onBackFromPreview: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(
                    Icons.Default.Close,
                    contentDescription =
                        stringResource(
                            R.string.selection_bottom_bar_clear_button_content_description
                        ),
                )
            }
            Text(text = "$selectedCount", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.weight(1f))
            if (isPreviewMode) {
                TextButton(onClick = onBackFromPreview) {
                    Text(stringResource(R.string.selection_bottom_bar_back_button_label))
                }
            } else {
                val previewContentDescription =
                    stringResource(R.string.selection_bottom_bar_preview_button_content_description)
                TextButton(onClick = onPreviewClicked) {
                    Text(
                        text = stringResource(R.string.selection_bottom_bar_preview_button_label),
                        modifier =
                            Modifier.semantics { contentDescription = previewContentDescription },
                    )
                }
            }
            val doneButtonLabel = stringResource(R.string.selection_bottom_bar_done_button_label)
            Button(
                onClick = onDoneClick,
                modifier = Modifier.semantics { contentDescription = doneButtonLabel },
            ) {
                Text(doneButtonLabel)
            }
        }
    }
}
