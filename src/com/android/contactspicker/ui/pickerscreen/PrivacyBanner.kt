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

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.contactspicker.R
import com.android.contactspicker.ui.components.PrivacyShieldIcon

internal const val PRIVACY_BANNER_TEST_TAG = "PrivacyBanner"

/**
 * A banner that provides a privacy notice about sharing contact data with the requesting
 * application
 *
 * @param visible Whether the banner should be visible.
 * @param callingAppName The name of the calling application.
 * @param onDismissRequest Callback to be invoked when the "Dismiss" button is clicked.
 * @param onMoreDetails Callback to be invoked when the "More details" button is clicked.
 */
@Composable
fun PrivacyBanner(
    visible: Boolean,
    callingAppName: String?,
    onDismissRequest: () -> Unit,
    onMoreDetails: () -> Unit,
) {
    val spatialExpressiveSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = spatialExpressiveSpec),
        exit = shrinkVertically(animationSpec = spatialExpressiveSpec),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(28.dp),
                    )
                    .testTag(PRIVACY_BANNER_TEST_TAG)
        ) {
            PrivacyBannerDescription(
                callingAppName ?: stringResource(R.string.default_calling_app_name),
                modifier = Modifier.padding(16.dp),
            )
            PrivacyBannerActions(
                onDismissRequest,
                onMoreDetails,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun PrivacyBannerDescription(callingAppName: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PrivacyShieldIcon(modifier = Modifier.padding(4.dp).size(24.dp))
        Text(
            text = stringResource(R.string.privacy_banner_description, callingAppName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrivacyBannerActions(
    onDismissRequest: () -> Unit,
    onMoreDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        ActionButton(textResId = R.string.privacy_banner_dismiss, onClick = onDismissRequest)
        ActionButton(textResId = R.string.privacy_banner_more_details, onClick = onMoreDetails)
    }
}

@Composable
private fun ActionButton(@StringRes textResId: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = stringResource(textResId),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}
