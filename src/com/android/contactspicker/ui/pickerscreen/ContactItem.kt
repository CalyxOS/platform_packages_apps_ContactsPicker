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

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.contactspicker.R
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.EmailEntry
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.data.model.PhoneEntry
import com.android.contactspicker.ui.components.Avatar

private val CONTACT_ITEM_PADDING = 16.dp
private val EXPANDED_CONTACT_ITEM_START_PADDING = 32.dp
private val EXPANDED_CONTACT_ITEM_END_PADDING = 16.dp
private val EXPANDED_CONTACT_ITEM_VERTICAL_PADDING = 8.dp
private val AVATAR_TEXT_SPACING = 16.dp
private val ICON_TEXT_SPACING = 8.dp

/**
 * A composable that displays a single contact item.
 *
 * The entries shown (e.g., phone or email) are determined by the type of the [Contact] object
 * provided. If a contact has multiple phone numbers or emails, it will be expandable.
 *
 * @param contact The contact to display.
 * @param selectedEntries The list of currently selected entries for the contact, keyed by IDs.
 * @param onToggleContactSelection A callback invoked when the avatar is clicked to select/deselect
 *   the whole contact.
 * @param onToggleEntrySelection A callback invoked when a single entry (e.g. an email) is selected
 *   from an expanded list.
 */
@Composable
fun ContactItem(
    contact: Contact,
    selectedEntries: Set<Long>?,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (contactId: Long, entryId: Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val isExpandable =
        (contact is PhoneContact && contact.phones.size > 1) ||
            (contact is EmailContact && contact.emails.size > 1)

    // A contact is considered "fully selected" for the avatar checkmark only when all of its
    // entries are selected.
    val isFullySelected = contact.isFullySelected(selectedEntries)

    // The background highlights if any entry is selected.
    val isAnyEntrySelected = selectedEntries?.isNotEmpty() == true
    Surface(
        color =
            if (isAnyEntrySelected) MaterialTheme.colorScheme.surfaceDim
            else MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            val rowModifier =
                if (isExpandable) {
                    Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp)
                } else {
                    Modifier.fillMaxWidth().padding(CONTACT_ITEM_PADDING)
                }
            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AVATAR_TEXT_SPACING),
            ) {
                SelectableAvatar(
                    contact = contact,
                    isSelected = isFullySelected,
                    onClick = { onToggleContactSelection(contact) },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    // Secondary text changes based on the type and count of contact entries
                    contact.secondaryText()?.let { secondaryText ->
                        Text(
                            text = secondaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isExpandable) {
                    val rotationAngle by
                        animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            label = "expand_collapse_icon_rotation",
                        )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription =
                            stringResource(
                                if (expanded)
                                    R.string.contact_item_collapse_button_content_description
                                else R.string.contact_item_expand_button_content_description
                            ),
                        modifier = Modifier.graphicsLayer { rotationZ = rotationAngle },
                    )
                }
            }

            if (expanded) {
                ExpandedContact(
                    contact = contact,
                    selectedEntries = selectedEntries ?: emptySet(),
                    onToggleEntry = onToggleEntrySelection,
                )
            }
        }
    }
}

/**
 * Determines the secondary text to display below the contact's name.
 *
 * Returns the phone number or email if there is only one. If there are multiple, it returns a
 * formatted count string (e.g., "2 phone numbers"). Returns null if the contact has no entries
 * (i.e., it's a [DisplayNameContact]).
 */
@Composable
private fun Contact.secondaryText(): String? =
    when (this) {
        is DisplayNameContact -> null
        is EmailContact ->
            if (emails.size > 1) stringResource(R.string.contact_item_emails_count, emails.size)
            else emails.first().address
        is PhoneContact ->
            if (phones.size > 1) stringResource(R.string.contact_item_phones_count, phones.size)
            else phones.first().number
    }

@Composable
private fun SelectableAvatar(contact: Contact, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription =
                        stringResource(
                            R.string.contact_item_selected_content_description,
                            contact.displayName,
                        ),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        } else {
            Avatar(displayName = contact.displayName)
        }
    }
}

@Composable
private fun ExpandedContact(
    contact: Contact,
    selectedEntries: Set<Long>,
    onToggleEntry: (contactId: Long, entryId: Long) -> Unit,
) {
    when (contact) {
        is PhoneContact ->
            contact.phones.forEach { phoneEntry ->
                ExpandedPhoneEntry(
                    phoneEntry = phoneEntry,
                    isChecked = selectedEntries.contains(phoneEntry.id),
                    onCheckedChange = { onToggleEntry(contact.id, phoneEntry.id) },
                )
            }
        is EmailContact ->
            contact.emails.forEach { emailEntry ->
                ExpandedEmailEntry(
                    emailEntry = emailEntry,
                    isChecked = selectedEntries.contains(emailEntry.id),
                    onCheckedChange = { onToggleEntry(contact.id, emailEntry.id) },
                )
            }
        is DisplayNameContact -> {
            /* Not expandable, do nothing */
        }
    }
}

@Composable
private fun ExpandedPhoneEntry(
    phoneEntry: PhoneEntry,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
) {
    ExpandedContactEntry(
        text = phoneEntry.number,
        label = phoneEntry.label,
        isChecked = isChecked,
        onCheckedChange = onCheckedChange,
    ) {
        Icon(
            imageVector = Icons.Outlined.Phone,
            contentDescription =
                stringResource(R.string.contact_item_phone_icon_content_description),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExpandedEmailEntry(
    emailEntry: EmailEntry,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
) {
    ExpandedContactEntry(
        text = emailEntry.address,
        label = emailEntry.label,
        isChecked = isChecked,
        onCheckedChange = onCheckedChange,
    ) {
        Icon(
            imageVector = Icons.Outlined.Email,
            contentDescription =
                stringResource(R.string.contact_item_email_icon_content_description),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExpandedContactEntry(
    text: String,
    label: String?,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable { onCheckedChange() }
                .padding(
                    start = EXPANDED_CONTACT_ITEM_START_PADDING,
                    end = EXPANDED_CONTACT_ITEM_END_PADDING,
                    top = EXPANDED_CONTACT_ITEM_VERTICAL_PADDING,
                    bottom = EXPANDED_CONTACT_ITEM_VERTICAL_PADDING,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ICON_TEXT_SPACING),
    ) {
        icon()
        Spacer(modifier = Modifier.padding(ICON_TEXT_SPACING))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
            if (!label.isNullOrBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Checkbox(checked = isChecked, onCheckedChange = { onCheckedChange() })
    }
}
