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

package com.android.contactspicker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val TOP_SPACER_WEIGHT = 1f
private const val BOTTOM_SPACER_WEIGHT = 3f
private val ICON_SIZE = 56.dp
private val ICON_PADDING = 12.dp
private val TITLE_TOP_PADDING = 24.dp
private val TEXT_WIDTH = 312.dp
private val DESCRIPTION_TOP_PADDING = 8.dp

/** Composable function to display the empty state screen. */
@Composable
fun EmptyContactsScreen(title: String, description: String, icon: ImageVector) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.weight(TOP_SPACER_WEIGHT))
        Icon(
            imageVector = icon,
            contentDescription = null, // Decorative icon
            modifier =
                Modifier.size(ICON_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(ICON_PADDING),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = TITLE_TOP_PADDING).width(TEXT_WIDTH),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = DESCRIPTION_TOP_PADDING).width(TEXT_WIDTH),
        )
        Spacer(modifier = Modifier.weight(BOTTOM_SPACER_WEIGHT))
    }
}
