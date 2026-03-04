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

import android.icu.text.MessageFormat
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.android.contactspicker.R
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.EmailEntry
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.data.model.PhoneEntry
import com.android.contactspicker.ui.components.AVATAR_SIZE
import com.android.contactspicker.ui.components.Avatar
import java.util.Locale

// TODO(b/450842541): move constants to a separate file

private const val AVATAR_ANIMATION_INITIAL_SIZE = 8f
private val CONTACT_ITEM_PADDING = 16.dp
private val EXPANDED_CONTACT_ITEM_START_PADDING = 32.dp
private val EXPANDED_CONTACT_ITEM_END_PADDING = 16.dp
private val EXPANDED_CONTACT_ITEM_VERTICAL_PADDING = 8.dp
private val AVATAR_TEXT_SPACING = 16.dp
private val ICON_TEXT_SPACING = 8.dp
private val CARD_OUTER_CORNER_RADIUS = 32.dp
private val CARD_INNER_CORNER_RADIUS = 4.dp

internal val TOGGLE_ICON_BOX_SIZE = 48.dp
internal val TOGGLE_ICON_BUTTON_HEIGHT = 40.dp
internal val TOGGLE_ICON_BUTTON_WIDTH = 32.dp
internal val TOP_ITEM_SHAPE =
    RoundedCornerShape(
        topStart = CARD_OUTER_CORNER_RADIUS,
        topEnd = CARD_OUTER_CORNER_RADIUS,
        bottomStart = CARD_INNER_CORNER_RADIUS,
        bottomEnd = CARD_INNER_CORNER_RADIUS,
    )

internal val BOTTOM_ITEM_SHAPE =
    RoundedCornerShape(
        topStart = CARD_INNER_CORNER_RADIUS,
        topEnd = CARD_INNER_CORNER_RADIUS,
        bottomStart = CARD_OUTER_CORNER_RADIUS,
        bottomEnd = CARD_OUTER_CORNER_RADIUS,
    )

internal val SINGLE_ITEM_SHAPE = RoundedCornerShape(CARD_OUTER_CORNER_RADIUS)

internal val MIDDLE_ITEM_SHAPE = RoundedCornerShape(CARD_INNER_CORNER_RADIUS)

enum class ItemPosition {
    FIRST,
    MIDDLE,
    LAST,
    ONLY,
}

/**
 * A composable that displays a single contact item.
 *
 * The entries shown (e.g., phone or email) are determined by the type of the [Contact] object
 * provided. If a contact has multiple phone numbers or emails, it will be expandable.
 *
 * @param contact The contact to display.
 * @param selectedEntries The list of currently selected entries for the contact, keyed by IDs.
 * @param isMultiSelectEnabled True if multiple selections are allowed.
 * @param isSearchMode True if the item is being displayed in search results. In this mode,
 *   interactions target the specific entry displayed rather than the entire contact.
 * @param onToggleContactSelection A callback invoked when the avatar is clicked to select/deselect
 *   the whole contact.
 * @param onToggleEntrySelection A callback invoked when a single entry (e.g. an email) is selected
 *   from an expanded list.
 * @param position The item's position in its group, used to determine shape.
 */
@Composable
fun ContactItem(
    contact: Contact,
    position: ItemPosition,
    selectedEntries: Set<Long>?,
    isMultiSelectEnabled: Boolean,
    isSearchMode: Boolean,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (Long, Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val isExpandable =
        (contact is PhoneContact && contact.phones.size > 1) ||
            (contact is EmailContact && contact.emails.size > 1)

    val searchTargetEntryId =
        remember(contact) {
            when (contact) {
                is PhoneContact -> contact.phones.firstOrNull()?.id
                is EmailContact -> contact.emails.firstOrNull()?.id
                is DisplayNameContact -> contact.id
            }
        }

    // The avatar is changed to a checkmark when any of the contact's entries are selected.
    // In search mode, each entry is shown as a separate ContactItem, so need to know which entryId
    // corresponds to the current ContactItem.
    val isSelected =
        if (isSearchMode) {
            searchTargetEntryId != null && selectedEntries?.contains(searchTargetEntryId) == true
        } else {
            contact.hasAnySelection(selectedEntries ?: emptySet())
        }

    val onRowClick: () -> Unit = {
        if (isSearchMode) {
            searchTargetEntryId?.let { entryId -> onToggleEntrySelection(contact.id, entryId) }
        } else if (isExpandable) {
            expanded = !expanded
        } else {
            onToggleContactSelection(contact)
        }
    }

    val hasSeparateAvatarAction =
        !isSearchMode && isExpandable && (isSelected || isMultiSelectEnabled)

    val onAvatarClick: (() -> Unit)? =
        if (hasSeparateAvatarAction) {
            { onToggleContactSelection(contact) }
        } else {
            // click on the avatar will fall through to the parent row
            null
        }

    Surface(
        color =
            if (isSelected) MaterialTheme.colorScheme.surfaceDim
            else MaterialTheme.colorScheme.surfaceBright,
        shape = calculateShape(position, isSelected),
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            val isAvatarActionDeselect =
                if (isMultiSelectEnabled) contact.isFullySelected(selectedEntries) else isSelected
            val avatarActionLabel =
                if (isAvatarActionDeselect) stringResource(R.string.a11y_deselect_all)
                else stringResource(R.string.a11y_select_all)

            val rowClickLabel =
                if (isExpandable && !isSearchMode) {
                    stringResource(
                        if (expanded) R.string.contact_item_collapse_button_content_description
                        else R.string.contact_item_expand_button_content_description
                    )
                } else null

            val rowModifier =
                Modifier.fillMaxWidth()
                    .clickable(onClickLabel = rowClickLabel, onClick = onRowClick)
                    .semantics {
                        // This ensures TalkBack announces "Selected" or "Not Selected"
                        selected = isSelected
                        role = Role.Checkbox
                        // Only add the TalkBack custom action if the avatar has a separate action
                        if (hasSeparateAvatarAction) {
                            customActions =
                                listOf(
                                    CustomAccessibilityAction(label = avatarActionLabel) {
                                        onAvatarClick?.invoke()
                                        true
                                    }
                                )
                        }
                    }
                    .padding(CONTACT_ITEM_PADDING)

            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AVATAR_TEXT_SPACING),
            ) {
                SelectableAvatar(
                    modifier = Modifier.clearAndSetSemantics {},
                    contact = contact,
                    isSelected = isSelected,
                    onClick = onAvatarClick,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.semantics { contentDescription = contact.displayName },
                    )
                    // Secondary text changes based on the type, total count of contact entries, and
                    // count of selected entries.
                    ContactSecondaryText(
                        contact = contact,
                        selectedEntries = selectedEntries ?: emptySet(),
                    )
                }
                if (isExpandable && !isSearchMode) {
                    val rotationAngle by
                        animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            label = "expand_collapse_icon_rotation",
                        )

                    Box(
                        modifier = Modifier.size(TOGGLE_ICON_BOX_SIZE).clearAndSetSemantics {},
                        contentAlignment = Alignment.Center,
                    ) {
                        FilledIconToggleButton(
                            checked = expanded,
                            onCheckedChange = { expanded = it },
                            modifier =
                                Modifier.size(
                                    width = TOGGLE_ICON_BUTTON_WIDTH,
                                    height = TOGGLE_ICON_BUTTON_HEIGHT,
                                ),
                            shape = CircleShape,
                            colors =
                                IconButtonDefaults.filledIconToggleButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkedContainerColor =
                                        MaterialTheme.colorScheme.surfaceContainer,
                                    checkedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer { rotationZ = rotationAngle },
                            )
                        }
                    }
                }
            }

            if (expanded) {
                ExpandedContact(
                    contact = contact,
                    selectedEntries = selectedEntries ?: emptySet(),
                    isMultiSelectEnabled = isMultiSelectEnabled,
                    onToggleEntry = onToggleEntrySelection,
                )
            }
        }
    }
}

/**
 * Determines and displays the secondary text below the contact's name. Handles early return for
 * contacts without entries and ensures correct [TextDirection] for raw data vs. localized strings.
 *
 * Displays:
 * - nothing if the contact has no entries (i.e., it's a [DisplayNameContact]).
 * - the phone number or email for single entry contacts.
 * - for multi-entry contacts a formatted count string (e.g., "2 phone numbers") if none are
 *   selected, or a count of selected entries (e.g., "1 of 2 selected").
 */
@Composable
private fun ContactSecondaryText(
    contact: Contact,
    selectedEntries: Set<Long>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isVerbatim = contact is PhoneContact && contact.phones.size == 1

    val (text, direction) =
        when (contact) {
            is EmailContact -> {
                val totalCount = contact.emails.size
                if (totalCount > 1) {
                    val selectedCount = contact.emails.count { selectedEntries.contains(it.id) }
                    formatMultiEntrySecondaryText(
                        selectedCount,
                        totalCount,
                        context.getString(R.string.contact_item_emails_selected_count),
                        context.getString(R.string.contact_item_emails_count),
                    ) to TextDirection.Content
                } else {
                    contact.emails.first().address to TextDirection.Ltr
                }
            }
            is PhoneContact -> {
                val totalCount = contact.phones.size
                if (totalCount > 1) {
                    val selectedCount = contact.phones.count { selectedEntries.contains(it.id) }

                    formatMultiEntrySecondaryText(
                        selectedCount,
                        totalCount,
                        context.getString(R.string.contact_item_phones_selected_count),
                        context.getString(R.string.contact_item_phones_count),
                    ) to TextDirection.Content
                } else {
                    contact.phones.first().number to TextDirection.Ltr
                }
            }
            else -> return
        }

    val annotatedString = rememberAnnotatedContactData(text, direction, isVerbatim)

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * Constructs and remembers an [AnnotatedString] for contact data, applying the [ParagraphStyle] for
 * RTL directionality and [VerbatimTtsAnnotation] for TalkBack integrity.
 */
@Composable
private fun rememberAnnotatedContactData(
    text: String,
    direction: TextDirection,
    isVerbatim: Boolean,
): AnnotatedString =
    remember(text, direction, isVerbatim) {
        buildAnnotatedString {
            withStyle(ParagraphStyle(textDirection = direction)) {
                if (isVerbatim) {
                    withAnnotation(VerbatimTtsAnnotation(text)) { append(text) }
                } else {
                    append(text)
                }
            }
        }
    }

/** Returns a plain formatted string for multi-entry counts. */
fun formatMultiEntrySecondaryText(
    selectedCount: Int,
    totalCount: Int,
    itemsSelectedCountMessage: String,
    noItemsSelectedCountMessage: String,
): String {
    val (msgFormat, args) =
        if (selectedCount > 0)
            MessageFormat(itemsSelectedCountMessage, Locale.getDefault()) to
                mapOf("selected_count" to selectedCount, "total_count" to totalCount)
        else
            MessageFormat(noItemsSelectedCountMessage, Locale.getDefault()) to
                mapOf("count" to totalCount)

    return msgFormat.format(args)
}

@Composable
private fun SelectableAvatar(
    modifier: Modifier,
    contact: Contact,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
) {
    val spatialFastSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

    Box(
        modifier =
            modifier
                .size(AVATAR_SIZE.dp)
                .clip(CircleShape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (!isSelected) {
            Avatar(displayName = contact.displayName, profilePictureUri = contact.profilePictureUri)
        }
        AnimatedVisibility(
            visible = isSelected,
            enter =
                fadeIn(animationSpec = spatialFastSpec) +
                    scaleIn(
                        initialScale = AVATAR_ANIMATION_INITIAL_SIZE / AVATAR_SIZE,
                        animationSpec = spatialFastSpec,
                        transformOrigin = TransformOrigin.Center,
                    ),
            exit =
                fadeOut(animationSpec = spatialFastSpec) +
                    scaleOut(
                        targetScale = AVATAR_ANIMATION_INITIAL_SIZE / AVATAR_SIZE,
                        animationSpec = spatialFastSpec,
                        transformOrigin = TransformOrigin.Center,
                    ),
        ) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
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
        }
    }
}

@Composable
private fun ExpandedContact(
    contact: Contact,
    selectedEntries: Set<Long>,
    isMultiSelectEnabled: Boolean,
    onToggleEntry: (contactId: Long, entryId: Long) -> Unit,
) {
    when (contact) {
        is PhoneContact ->
            contact.phones.forEach { phoneEntry ->
                ExpandedPhoneEntry(
                    phoneEntry = phoneEntry,
                    isChecked = selectedEntries.contains(phoneEntry.id),
                    isMultiSelectEnabled = isMultiSelectEnabled,
                    onCheckedChange = { onToggleEntry(contact.id, phoneEntry.id) },
                )
            }
        is EmailContact ->
            contact.emails.forEach { emailEntry ->
                ExpandedEmailEntry(
                    emailEntry = emailEntry,
                    isChecked = selectedEntries.contains(emailEntry.id),
                    isMultiSelectEnabled = isMultiSelectEnabled,
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
    isMultiSelectEnabled: Boolean,
    onCheckedChange: () -> Unit,
) {
    val annotatedText =
        rememberAnnotatedContactData(
            text = phoneEntry.number,
            direction = TextDirection.Ltr,
            isVerbatim = true,
        )
    ExpandedContactEntry(
        text = annotatedText,
        label = phoneEntry.label,
        isChecked = isChecked,
        isMultiSelectEnabled = isMultiSelectEnabled,
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
    isMultiSelectEnabled: Boolean,
    onCheckedChange: () -> Unit,
) {
    val annotatedText =
        rememberAnnotatedContactData(
            text = emailEntry.address,
            direction = TextDirection.Content,
            isVerbatim = false,
        )
    ExpandedContactEntry(
        text = annotatedText,
        label = emailEntry.label,
        isChecked = isChecked,
        isMultiSelectEnabled = isMultiSelectEnabled,
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
    text: AnnotatedString,
    label: String?,
    isChecked: Boolean,
    isMultiSelectEnabled: Boolean,
    onCheckedChange: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable { onCheckedChange() }
                .semantics {
                    selected = isChecked
                    role = if (isMultiSelectEnabled) Role.Checkbox else Role.RadioButton
                }
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
        SelectionControl(
            // The whole row is clickable so hide the selection control from Talkback.
            modifier = Modifier.semantics { hideFromAccessibility() },
            selected = isChecked,
            isMultiSelect = isMultiSelectEnabled,
        )
    }
}

@Composable
private fun SelectionControl(selected: Boolean, isMultiSelect: Boolean, modifier: Modifier) {
    Box(modifier = modifier.size(TOGGLE_ICON_BOX_SIZE), contentAlignment = Alignment.Center) {
        if (isMultiSelect) {
            Checkbox(checked = selected, onCheckedChange = null)
        } else {
            RadioButton(selected = selected, onClick = null)
        }
    }
}

/**
 * Checks if any entry within the contact is currently selected. Used to determine whether to show
 * the avatar with a checkmark.
 */
private fun Contact.hasAnySelection(selectedEntries: Set<Long>): Boolean {
    return when (this) {
        is PhoneContact -> phones.any { selectedEntries.contains(it.id) }
        is EmailContact -> emails.any { selectedEntries.contains(it.id) }
        is DisplayNameContact -> selectedEntries.contains(id)
    }
}

// Calculates the shape of the ContactItem based on its position and selection state:
// - any selected or partially selected item has all corners rounded
// - only the first item has rounded top corners
// - only last item has rounded
@VisibleForTesting
internal fun calculateShape(position: ItemPosition, isAnyEntrySelected: Boolean): Shape =
    if (isAnyEntrySelected) {
        SINGLE_ITEM_SHAPE
    } else {
        when (position) {
            ItemPosition.ONLY -> SINGLE_ITEM_SHAPE
            ItemPosition.FIRST -> TOP_ITEM_SHAPE

            ItemPosition.LAST -> BOTTOM_ITEM_SHAPE

            ItemPosition.MIDDLE -> MIDDLE_ITEM_SHAPE
        }
    }
