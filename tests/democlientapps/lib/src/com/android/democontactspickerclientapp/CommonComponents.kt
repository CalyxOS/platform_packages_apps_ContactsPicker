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
package com.android.democontactspickerclientapp

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.democontactspickerclientapp.lib.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ScreenTitle(targetSdk: Int) {
    val context = LocalContext.current
    val buildInfo = remember {
        try {
            context.resources.openRawResource(R.raw.build_info).bufferedReader().use {
                it.readText()
            }
        } catch (e: Exception) {
            "Build info not available"
        }
    }
    Text("Contacts Picker Demo Client", style = MaterialTheme.typography.headlineMedium)
    Text("Target SDK: $targetSdk", style = MaterialTheme.typography.headlineSmall)
    Text(buildInfo.trim(), style = MaterialTheme.typography.labelMedium)
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}

@Composable
fun SwitchOption(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun CommonOptionsWithSystemPickerDisabled(
    allowMultiple: Boolean,
    onAllowMultipleChange: (Boolean) -> Unit,
) {
    SwitchOption(
        title = "Allow Multiple Selection",
        checked = allowMultiple,
        onCheckedChange = onAllowMultipleChange,
    )
}

@Composable
fun CommonOptions(
    allowMultiple: Boolean,
    onAllowMultipleChange: (Boolean) -> Unit,
    overrideSelectionLimit: Boolean,
    onOverrideSelectionLimitChange: (Boolean) -> Unit,
    selectionLimit: Int,
    onSelectionLimitChange: (Int) -> Unit,
) {
    CommonOptionsWithSystemPickerDisabled(allowMultiple, onAllowMultipleChange)
    SwitchOption(
        title = "Override selection limit",
        checked = overrideSelectionLimit,
        onCheckedChange = onOverrideSelectionLimitChange,
    )
    if (overrideSelectionLimit) {
        Spacer(modifier = Modifier.height(8.dp))
        NumberInputRow(
            value = selectionLimit,
            onValueChange = onSelectionLimitChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

fun LazyListScope.actionPickResultDisplay(pickerResult: PickerResult) {
    item {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column {
                    Text(
                        text = "Result Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = pickerResult.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    if (pickerResult.rows.isNotEmpty()) {
        items(pickerResult.rows) { flatRow -> RowTile(flatRow) }
    }
}

@Composable
fun RowTile(flatRow: FlatRow) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Row from: ${flatRow.contactName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = flatRow.row.metadata,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            DataRowContent(flatRow.row)
        }
    }
}

@Composable
fun DataRowContent(row: DataRow) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${row.label}: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        if (row.photoBytes != null) {
            var bitmap by
                remember(row.photoBytes) {
                    mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
                }
            var hasError by remember(row.photoBytes) { mutableStateOf(false) }

            LaunchedEffect(row.photoBytes) {
                withContext(Dispatchers.IO) {
                    val decoded =
                        BitmapFactory.decodeByteArray(row.photoBytes, 0, row.photoBytes.size)
                    if (decoded != null) {
                        bitmap = decoded.asImageBitmap()
                    } else {
                        hasError = true
                    }
                }
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = "Contact Photo",
                    modifier = Modifier.size(48.dp),
                )
            } else if (hasError) {
                Text(
                    text = "Invalid photo data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(
                    text = "Loading image...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(text = row.valueText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun LaunchPickerButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("Launch Contacts Picker") }
}

@Composable
fun NumberInputRow(value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = { onValueChange(value - 1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease selection limit")
            }
            TextField(
                value = value.toString(),
                onValueChange = { newValue ->
                    if (newValue.isEmpty()) {
                        onValueChange(0)
                    } else {
                        // Make sure we only accept numbers
                        newValue.toIntOrNull()?.let { onValueChange(it) }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            )
            IconButton(onClick = { onValueChange(value + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase selection limit")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Button(onClick = { onValueChange(5) }) { Text("5") }
            Button(onClick = { onValueChange(50) }) { Text("50") }
            Button(onClick = { onValueChange(100) }) { Text("100") }
        }
    }
}
