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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** A data class holding only the state relevant to the legacy ACTION_PICK. */
data class LegacyDemoConfigState(
    val legacyPickerType: LegacyPickerType = LegacyPickerType.EMAIL,
    val resultText: String = "Picker result will be shown here",
)

enum class LegacyPickerType(val label: String) {
    EMAIL("Emails"),
    PHONE("Phone numbers"),
    CONTACT("Full contacts"),
}

@Composable
fun LegacyActionPickConfiguration(
    config: LegacyDemoConfigState,
    onConfigChange: (LegacyDemoConfigState) -> Unit,
) {
    SectionTitle("Configure ACTION_PICK")
    LegacyPickerTypeSelector(config.legacyPickerType) { newType ->
        onConfigChange(config.copy(legacyPickerType = newType))
    }
}

@Composable
private fun LegacyPickerTypeSelector(
    selected: LegacyPickerType,
    onSelect: (LegacyPickerType) -> Unit,
) {
    Column {
        LegacyPickerType.entries.forEach { type ->
            Row(
                Modifier.fillMaxWidth()
                    .height(48.dp)
                    .toggleable(
                        value = (type == selected),
                        onValueChange = { onSelect(type) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = (type == selected), onClick = null)
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}
