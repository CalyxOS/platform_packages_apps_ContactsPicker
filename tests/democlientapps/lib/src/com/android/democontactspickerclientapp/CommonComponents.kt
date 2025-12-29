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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.democontactspickerclientapp.lib.R

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

@Composable
fun ContactTile(result: ContactResult) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Contact name: ${result.contactName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${result.detailLabel}: ${result.detail}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "URI: ${result.uri}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun ResultDisplay(pickerResult: PickerResult) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Result Status",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = pickerResult.statusText,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            style = MaterialTheme.typography.titleMedium,
        )

        if (pickerResult.contacts.isNotEmpty()) {

            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                pickerResult.contacts.forEach { result -> ContactTile(result) }
            }
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
