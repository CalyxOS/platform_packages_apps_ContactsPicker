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
package com.android.contactspicker.ui.scrubber

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.contactspicker.R
import com.android.contactspicker.data.model.SectionKey
import com.android.contactspicker.data.model.SectionKey.EmojiSection
import com.android.contactspicker.data.model.SectionKey.FavoriteSection
import com.android.contactspicker.data.model.SectionKey.LetterKey
import com.android.contactspicker.data.model.SectionKey.StringKey

// TODO(b/468919056): Add FadeIn/FadeOut animation for ScrubberLabel
/**
 * Displays a label that shows the current section key during fast scrolling.
 *
 * This composable will be used with the scrubber composable. The content of the label is determined
 * by the provided [sectionKey].
 *
 * @param sectionKey The current [SectionKey] to display. When not null, the label is visible and
 *   shows content corresponding to the key type (e.g., a letter or an icon). When null, the label
 *   is hidden.
 */
@Composable
fun ScrubberLabel(sectionKey: SectionKey?) {
    if (sectionKey != null) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.testTag(SCRUBBER_LABEL_TEST_TAG),
            shadowElevation = 4.dp,
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 40.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                ScrubberContent(targetKey = sectionKey)
            }
        }
    }
}

@Composable
private fun ScrubberContent(targetKey: SectionKey) {
    when (targetKey) {
        is LetterKey ->
            Text(
                text = targetKey.letter.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(SCRUBBER_LABEL_TEXT_TEST_TAG),
            )
        is StringKey ->
            Text(
                text = targetKey.header,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(SCRUBBER_LABEL_TEXT_TEST_TAG),
            )
        is FavoriteSection ->
            Image(
                imageVector = Icons.Filled.Star,
                contentDescription =
                    stringResource(R.string.favorites_header_icon_content_description),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.size(56.dp).testTag(SCRUBBER_LABEL_FAVORITE_ICON_TEST_TAG),
            )
        is EmojiSection ->
            Image(
                imageVector = Icons.Filled.Mood,
                contentDescription = stringResource(R.string.emoji_header_icon_content_description),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.size(56.dp).testTag(SCRUBBER_LABEL_EMOJI_ICON_TEST_TAG),
            )
    }
}

internal const val SCRUBBER_LABEL_TEST_TAG = "scrubber_label"
internal const val SCRUBBER_LABEL_TEXT_TEST_TAG = "scrubber_label_text"
internal const val SCRUBBER_LABEL_FAVORITE_ICON_TEST_TAG = "scrubber_label_favorite_icon"
internal const val SCRUBBER_LABEL_EMOJI_ICON_TEST_TAG = "scrubber_label_emoji_icon"
