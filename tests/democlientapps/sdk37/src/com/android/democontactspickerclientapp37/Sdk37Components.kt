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

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.democontactspickerclientapp.DataRow
import com.android.democontactspickerclientapp.DataRowContent
import com.android.democontactspickerclientapp.FlatRow
import com.android.democontactspickerclientapp.RowTile
import com.android.democontactspickerclientapp.SectionTitle

data class Contact(val id: String, val displayName: String, val rows: List<DataRow>)

enum class ResultTab {
    CONTACTS,
    ROWS,
}

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

    val (supported, unsupported) = MimeType.entries.partition { it.isSupported }
    val isAllSelected = selectedMimeTypes.containsAll(supported)

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
                        onMimeTypesChange(supported.toSet())
                    }
                },
                role = Role.Checkbox,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = isAllSelected, onCheckedChange = null)
        Text(
            text = "SELECT ALL (Supported)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
    supported.forEach { mimeType -> MimeTypeRow(mimeType, selectedMimeTypes, onMimeTypesChange) }

    if (unsupported.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        unsupported.forEach { mimeType ->
            MimeTypeRow(mimeType, selectedMimeTypes, onMimeTypesChange)
        }
    }
}

@Composable
private fun MimeTypeRow(
    mimeType: MimeType,
    selectedMimeTypes: Set<MimeType>,
    onMimeTypesChange: (Set<MimeType>) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .height(48.dp)
            .toggleable(
                value = selectedMimeTypes.contains(mimeType),
                onValueChange = { isSelected ->
                    val newMimeTypes = selectedMimeTypes.toMutableSet()
                    if (isSelected) newMimeTypes.add(mimeType) else newMimeTypes.remove(mimeType)
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
            // Dim the unsupported items slightly to further distinguish them
            color =
                if (mimeType.isSupported) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

fun SessionDataRow.toDataRow(contactId: Long): DataRow {
    val label =
        MimeType.entries.find { it.mimeTypeString == mimeType }?.label
            ?: mimeType.substringAfterLast("/")
    return DataRow(
        label = label,
        valueText = value?.toString() ?: "null",
        photoBytes = if (mimeType == MimeType.PHOTO.mimeTypeString) value as? ByteArray else null,
        metadata = "Contact ID: $contactId • Row ID: $id",
    )
}

fun LazyListScope.actionPickContactsResultDisplay(
    uri: Uri?,
    isLoading: Boolean,
    parseResult: SessionParseResult?,
    selectedTab: ResultTab,
    onTabSelected: (ResultTab) -> Unit,
) {
    item {
        if (uri == null) {
            Text(
                text = "No URI received",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            return@item
        }

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
                // Title Section: The URI
                Column {
                    Text(
                        text = "Result Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Session URI: $uri",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (isLoading) {
                    Text(
                        "Loading session data...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (parseResult == null || parseResult.orderedRows.isEmpty()) {
                    Text(
                        text = "No contacts found in this session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text =
                            "Contains ${parseResult.orderedRows.size} data rows for ${parseResult.aggregatedContacts.size} contacts",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                        Tab(
                            selected = selectedTab == ResultTab.CONTACTS,
                            onClick = { onTabSelected(ResultTab.CONTACTS) },
                            text = { Text("Contacts") },
                        )
                        Tab(
                            selected = selectedTab == ResultTab.ROWS,
                            onClick = { onTabSelected(ResultTab.ROWS) },
                            text = { Text("Rows") },
                        )
                    }
                }
            }
        }
    }

    if (!isLoading && parseResult != null && parseResult.orderedRows.isNotEmpty()) {
        when (selectedTab) {
            ResultTab.ROWS -> {
                items(parseResult.orderedRows) { sessionRow ->
                    val flatRow =
                        FlatRow(
                            contactName = sessionRow.displayName ?: "Unknown",
                            row = sessionRow.row.toDataRow(sessionRow.contactId),
                        )
                    RowTile(flatRow)
                }
            }
            ResultTab.CONTACTS -> {
                items(parseResult.aggregatedContacts) { sessionContact ->
                    val contact =
                        Contact(
                            id = sessionContact.contactId.toString(),
                            displayName = sessionContact.displayName ?: "Unknown",
                            rows =
                                sessionContact.dataRows.map {
                                    it.toDataRow(sessionContact.contactId)
                                },
                        )
                    ContactTile(contact)
                }
            }
        }
    }
}

@Composable
fun ContactTile(contact: Contact) {
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
            // Header: Name and ID
            Text(
                text = "Contact: ${contact.displayName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "ID: ${contact.id} • ${contact.rows.size} data rows",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            // Data Rows
            contact.rows.forEach { DataRowContent(it) }
        }
    }
}
