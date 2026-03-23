/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.contactspicker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.android.contactspicker.R
import com.android.contactspicker.data.model.SectionKey
import com.android.contactspicker.data.model.SectionKey.EmojiSection
import com.android.contactspicker.data.model.SectionKey.FavoriteSection
import com.android.contactspicker.data.model.SectionKey.LetterKey
import com.android.contactspicker.data.model.SectionKey.StringKey

/** Represent the display information of a [SectionKey] in the UI. */
sealed class SectionDisplay {
    /** Only text will be displayed. */
    data class Text(val text: String) : SectionDisplay()

    /** Icon will be displayed, with optional text. */
    data class Icon(
        val icon: ImageVector,
        val iconContentDescription: String,
        val text: String? = null,
    ) : SectionDisplay()
}

/** Returns the [SectionDisplay] for the given [SectionKey]. */
@Composable
fun SectionKey.getSectionDisplay(): SectionDisplay {
    return when (this) {
        is LetterKey -> SectionDisplay.Text(text = letter.toString())
        is StringKey -> SectionDisplay.Text(text = header)
        FavoriteSection ->
            SectionDisplay.Icon(
                text = stringResource(R.string.contacts_picker_favorites_header),
                icon = Icons.Filled.Star,
                iconContentDescription =
                    stringResource(R.string.favorites_header_icon_content_description),
            )
        EmojiSection ->
            SectionDisplay.Icon(
                icon = Icons.Filled.Mood,
                iconContentDescription =
                    stringResource(R.string.emoji_header_icon_content_description),
            )
    }
}
