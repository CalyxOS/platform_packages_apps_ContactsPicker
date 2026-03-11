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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.contactspicker.R
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

internal const val AVATAR_TEST_TAG = "contact_avatar"
internal const val AVATAR_FALLBACK_PERSON_ICON_TEST_TAG = "avatar_fallback_person_icon"

/**
 * A composable that displays a circular avatar with the first initial of a display name.
 *
 * @param displayName The display name to use for the avatar's initial.
 * @param profilePictureUri The string URI for the contact's profile picture thumbnail.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Avatar(displayName: String, profilePictureUri: String?) {
    Box(
        modifier =
            Modifier.size(40.dp)
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
            // Fallback to initials / person icon
            val initialContentDescription =
                stringResource(R.string.contact_avatar_initial_content_description)
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .semantics { contentDescription = initialContentDescription },
                contentAlignment = Alignment.Center,
            ) {
                val firstChar = displayName.firstOrNull()
                if (firstChar?.isLetter() == true) {
                    Text(
                        text = firstChar.uppercase(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier =
                            Modifier.size(32.dp).testTag(AVATAR_FALLBACK_PERSON_ICON_TEST_TAG),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
