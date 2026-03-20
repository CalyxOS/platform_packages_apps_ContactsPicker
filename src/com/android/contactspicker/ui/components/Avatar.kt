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

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.contactspicker.R
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

const val AVATAR_SIZE = 40
internal const val AVATAR_TEST_TAG = "contact_avatar"
internal const val AVATAR_FALLBACK_PERSON_ICON_TEST_TAG = "avatar_fallback_person_icon"

/**
 * A composable that displays a circular avatar with the first initial of a display name.
 *
 * @param displayName The display name to use for the avatar's initial.
 * @param lookupKey The stable lookupKey from Contacts Provider.
 * @param profilePictureUri The string URI for the contact's profile picture thumbnail.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Avatar(displayName: String, lookupKey: String, profilePictureUri: String?) {
    Box(
        modifier =
            Modifier.size(AVATAR_SIZE.dp)
                .clip(CircleShape) // Clip the whole container
                .testTag(AVATAR_TEST_TAG),
        contentAlignment = Alignment.Center,
    ) {
        if (!profilePictureUri.isNullOrBlank()) {
            GlideImage(
                model = Uri.parse(profilePictureUri),
                contentDescription =
                    stringResource(R.string.contact_avatar_profile_picture_content_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Determine the stable color index based on the lookup key
            val isDark = isSystemInDarkTheme()
            val colorIndex =
                remember(lookupKey) {
                    (lookupKey.hashCode() and 0x7FFFFFFF) % LIGHT_BG_PALETTE.size
                }
            val backgroundColor =
                if (isDark) DARK_BG_PALETTE[colorIndex] else LIGHT_BG_PALETTE[colorIndex]
            val foregroundColor =
                if (isDark) DARK_TEXT_PALETTE[colorIndex] else LIGHT_TEXT_PALETTE[colorIndex]
            // Fallback to initials / person icon
            val initialContentDescription =
                stringResource(R.string.contact_avatar_initial_content_description)
            Box(
                modifier =
                    Modifier.fillMaxSize().background(backgroundColor).semantics {
                        contentDescription = initialContentDescription
                    },
                contentAlignment = Alignment.Center,
            ) {
                val firstChar = displayName.firstOrNull()
                if (firstChar?.isLetter() == true) {
                    Text(
                        text = firstChar.uppercase(),
                        color = foregroundColor,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_person_silhouette),
                        contentDescription = null,
                        modifier =
                            Modifier.fillMaxSize().testTag(AVATAR_FALLBACK_PERSON_ICON_TEST_TAG),
                        tint = foregroundColor,
                    )
                }
            }
        }
    }
}

// GM3 Color Palettes (sourced from Google Monogram library)
private val LIGHT_BG_PALETTE =
    listOf(
        Color(0xFFFFB3AE),
        Color(0xFF80DA88),
        Color(0xFFFCBD00),
        Color(0xFFD9BAFD),
        Color(0xFFFFAEE4),
        Color(0xFF60D5F3),
    )
private val LIGHT_TEXT_PALETTE =
    listOf(
        Color(0xFF60150F),
        Color(0xFF00381F),
        Color(0xFF4D2600),
        Color(0xFF400B84),
        Color(0xFF620438),
        Color(0xFF003641),
    )
private val DARK_BG_PALETTE =
    listOf(
        Color(0xFF8A1A16),
        Color(0xFF00522C),
        Color(0xFF6D3A01),
        Color(0xFF5629A4),
        Color(0xFF8D0053),
        Color(0xFF004E5D),
    )
private val DARK_TEXT_PALETTE =
    listOf(
        Color(0xFFFFF8F8),
        Color(0xFFF2FCEF),
        Color(0xFFFFFADE),
        Color(0xFFFDF8FF),
        Color(0xFFFFF7FC),
        Color(0xFFF0FBFF),
    )
