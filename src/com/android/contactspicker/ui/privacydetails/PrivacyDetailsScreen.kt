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
package com.android.contactspicker.ui.privacydetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.contactspicker.ContactsListState
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.R
import com.android.contactspicker.ui.components.TitleTopBar

// TODO(b/446118849) : Move constants to xml files
private val TOPBAR_PADDING = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
private val PRIVACY_DETAILS_BODY_PADDING = PaddingValues(horizontal = 16.dp, vertical = 14.dp)

internal const val PRIVACY_DETAILS_SCREEN_TOP_BAR_TEST_TAG = "privacy_details_screen_top_bar"

internal const val PRIVACY_DETAILS_SCREEN_BODY_TEST_TAG = "privacy_details_screen_body"

// TODO(b/455591386) : Revisit Error handling in PrivacyDetailsScreen in ContactsPicker
@Composable
fun PrivacyDetailsScreen(onBackPressed: () -> Unit, uiState: State<ContactsUiState>) {
    val callingAppName = (uiState.value as? ContactsListState.Success)?.callingAppName
    val requestedMimeTypes =
        (uiState.value as? ContactsListState.Success)?.requestedMimeTypes ?: emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        TitleTopBar(
            onBackPressed = onBackPressed,
            title = stringResource(id = R.string.privacy_details_top_bar_header),
            backIconDescription =
                stringResource(
                    id = R.string.privacy_details_top_bar_back_button_content_description
                ),
            modifier =
                Modifier.padding(TOPBAR_PADDING).testTag(PRIVACY_DETAILS_SCREEN_TOP_BAR_TEST_TAG),
        )

        PrivacyDetailsBody(
            modifier =
                Modifier.padding(PRIVACY_DETAILS_BODY_PADDING)
                    .testTag(PRIVACY_DETAILS_SCREEN_BODY_TEST_TAG),
            callingAppName = callingAppName,
            requestedDataFields = requestedMimeTypes,
        )
    }
}
