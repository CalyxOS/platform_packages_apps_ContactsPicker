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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.contactspicker.data.model.SectionKey
import com.android.contactspicker.ui.SectionDisplay
import com.android.contactspicker.ui.getSectionDisplay

internal const val CONTACTS_LIST_SECTION_HEADER_TEST_TAG = "contacts_list_section_header"
private val SECTION_HEADER_PADDING_VALUES = PaddingValues(horizontal = 24.dp, vertical = 8.dp)

/**
 * A composable that displays a section header for the given [sectionKey].
 *
 * @param sectionKey The [SectionKey] to display.
 * @param modifier The modifier to apply to the header.
 */
@Composable
fun SectionHeader(sectionKey: SectionKey, modifier: Modifier = Modifier) {
    when (val display = sectionKey.getSectionDisplay()) {
        is SectionDisplay.Text -> {
            SectionHeader(title = display.text)
        }
        is SectionDisplay.Icon -> {
            SectionHeader(
                imageVector = display.icon,
                iconContentDescription = display.iconContentDescription,
                text = display.text,
                modifier =
                    if (sectionKey is SectionKey.FavoriteSection) {
                        modifier.semantics { hideFromAccessibility() }
                    } else {
                        modifier
                    },
            )
        }
    }
}

/**
 * A composable that displays a section header with a string.
 *
 * @param title The string to display as the header.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier =
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(SECTION_HEADER_PADDING_VALUES)
                .testTag(CONTACTS_LIST_SECTION_HEADER_TEST_TAG),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * A composable that displays a section header with an icon or an icon followed by a text.
 *
 * @param imageVector ImageVector to display as an icon in the header.
 * @param iconContentDescription Content description for the icon.
 * @param text Optional text. If present it will be displayed in the same line after the icon.
 */
@Composable
private fun SectionHeader(
    imageVector: ImageVector,
    iconContentDescription: String,
    text: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(SECTION_HEADER_PADDING_VALUES)
                .testTag(CONTACTS_LIST_SECTION_HEADER_TEST_TAG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconSize =
            with(LocalDensity.current) { MaterialTheme.typography.labelLarge.fontSize.toDp() }
        Icon(
            imageVector = imageVector,
            contentDescription = iconContentDescription,
            modifier = modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))

        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
