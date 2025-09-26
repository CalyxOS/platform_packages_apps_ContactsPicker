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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.ui.components.Avatar

/**
 * A composable that displays a single contact item.
 *
 * The details shown (e.g., phone or email) are determined by the type of the [Contact] object
 * provided.
 *
 * @param contact The contact to display, which must be one of the [Contact] sealed subtypes.
 */
@Composable
fun ContactItem(contact: Contact) {
    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Avatar(displayName = contact.displayName)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                // Secondary text changes based on the type of the contact
                val secondaryText =
                    when (contact) {
                        is DisplayNameContact -> null
                        is EmailContact -> contact.email
                        is PhoneContact -> contact.phone
                    }
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
