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
package com.android.democontactspickerclientapp37

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.democontactspickerclientapp.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Sdk37IntentTypeSelector(selected: Sdk37IntentType, onSelect: (Sdk37IntentType) -> Unit) {
    Column {
        Sdk37IntentType.entries.forEach { type ->
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

@Composable
fun ActionPickContactsConfiguration(
    selectedMimeTypes: Set<MimeType>,
    onMimeTypesChange: (Set<MimeType>) -> Unit,
) {
    SectionTitle("Configure ACTION_PICK_CONTACTS")

    val allMimeTypes = MimeType.entries.toSet()
    val isAllSelected = selectedMimeTypes.containsAll(allMimeTypes)

    // "Select All" checkbox row
    Row(
        Modifier.fillMaxWidth()
            .height(48.dp)
            .toggleable(
                value = isAllSelected,
                onValueChange = {
                    if (isAllSelected) {
                        onMimeTypesChange(emptySet())
                    } else {
                        onMimeTypesChange(allMimeTypes)
                    }
                },
                role = Role.Checkbox,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = isAllSelected, onCheckedChange = null)
        Text(
            text = "SELECT ALL",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
    MimeType.entries.forEach { mimeType ->
        Row(
            Modifier.fillMaxWidth()
                .height(48.dp)
                .toggleable(
                    value = selectedMimeTypes.contains(mimeType),
                    onValueChange = { isSelected ->
                        val newMimeTypes = selectedMimeTypes.toMutableSet()
                        if (isSelected) newMimeTypes.add(mimeType)
                        else newMimeTypes.remove(mimeType)
                        onMimeTypesChange(newMimeTypes)
                    },
                    role = Role.Checkbox,
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selectedMimeTypes.contains(mimeType), onCheckedChange = null)
            Text(
                text = mimeType.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Composable
fun ActionPickContactsResultDisplay(uri: Uri?) {
    if (uri == null) {
        Text(
            text = "No URI received",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title Section: The URI
            Column {
                Text(
                    text = "Session URI:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = uri.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            val context = LocalContext.current
            var sessionContacts by remember { mutableStateOf<List<SessionContact>?>(null) }
            var isLoading by remember { mutableStateOf(false) }

            LaunchedEffect(uri) {
                isLoading = true
                sessionContacts = withContext(Dispatchers.IO) { parseSessionResult(context, uri) }
                isLoading = false
            }

            // Content Display
            if (isLoading) {
                Text(
                    "Loading session data...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val contacts = sessionContacts
                if (contacts.isNullOrEmpty()) {
                    Text(
                        "No contacts found in this session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    // Summary Line
                    val totalRows = contacts.sumOf { it.dataRows.size }
                    Text(
                        text = "Contains $totalRows data rows for ${contacts.size} contacts:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    // Individual Contact Cards
                    contacts.forEach { contact -> SessionContactTile(contact) }
                }
            }
        }
    }
}

@Composable
fun SessionContactTile(contact: SessionContact) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name and ID
            Text(
                text = "Contact: ${contact.displayName ?: "Unknown"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "ID: ${contact.contactId} • ${contact.dataRows.size} data rows",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            // Data Rows
            contact.dataRows.forEach { row ->
                val label =
                    MimeType.entries.find { it.mimeTypeString == row.mimeType }?.label
                        ?: row.mimeType.substringAfterLast("/")
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$label: ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (row.mimeType == MimeType.PHOTO.mimeTypeString && row.value is ByteArray) {
                        val bitmap: ImageBitmap = remember {
                            val bytes = row.value as ByteArray
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                        }
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Contact Photo",
                            modifier = Modifier.size(48.dp),
                        )
                    } else {
                        Text(
                            text = row.value?.toString() ?: "null",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
