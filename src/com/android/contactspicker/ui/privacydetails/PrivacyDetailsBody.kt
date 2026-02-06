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

import androidx.annotation.OpenForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.contactspicker.R
import com.android.contactspicker.data.model.MimeType
import com.android.contactspicker.ui.utils.IconResource

// TODO(b/446118849) : Move constants to xml files
private val PADDING_PRIVACY_DESCRIPTION =
    PaddingValues(start = 8.dp, end = 24.dp, bottom = 12.dp, top = 0.dp)
private val PADDING_CONTACT_DATA_FIELDS_LIST_HEADER =
    PaddingValues(top = 20.dp, end = 16.dp, bottom = 10.dp, start = 8.dp)

@Composable
fun PrivacyDetailsBody(
    modifier: Modifier = Modifier,
    callingAppName: String?,
    requestedDataFields: List<MimeType>,
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 64.dp)) {
        item {
            PrivacyDescription(
                callingAppName ?: stringResource(R.string.default_calling_app_name),
                modifier = Modifier.padding(PADDING_PRIVACY_DESCRIPTION),
            )
        }
        item {
            ContactDataFieldsListHeader(
                modifier = Modifier.padding(PADDING_CONTACT_DATA_FIELDS_LIST_HEADER)
            )
        }
        contactDataFieldList(requestedDataFields)
    }
}

@Composable
fun PrivacyDescription(appName: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(R.string.privacy_details_description, appName),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun ContactDataFieldsListHeader(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(id = R.string.privacy_details_contact_data_fields_list_header),
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

fun LazyListScope.contactDataFieldList(
    requestedDataFields: List<MimeType>,
    modifier: Modifier = Modifier,
) {
    val dataFieldItems = ContactDataFieldProvider.getContactDataFieldItems(requestedDataFields)
    val dataFieldItemsSize = dataFieldItems.size

    itemsIndexed(items = dataFieldItems) { index, item ->
        val cornerShape = getShapeForListPosition(index, dataFieldItemsSize)
        ContactDataFieldRow(
            data = item,
            cornerShape = cornerShape,
            modifier = modifier.padding(top = 2.dp),
        )
    }
}

@Composable
fun ContactDataFieldRow(
    data: ContactDataFieldItem,
    cornerShape: Shape,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = cornerShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier =
                Modifier.defaultMinSize(minHeight = 68.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (val iconData = data.icon) {
                is IconResource.Vector -> {
                    Icon(
                        modifier = Modifier.padding(8.dp).size(24.dp),
                        imageVector = iconData.imageVector,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = null, // disregard for accessibility
                    )
                }
                is IconResource.Painter -> {
                    Icon(
                        modifier = Modifier.padding(8.dp).size(24.dp),
                        painter = painterResource(id = iconData.id),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = null, // disregard for accessibility
                    )
                }
            }
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = stringResource(id = data.headerTextResId),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                data.descriptionTextResId?.let { descriptionResId ->
                    Text(
                        text = stringResource(id = descriptionResId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OpenForTesting
internal fun getShapeForListPosition(index: Int, listSize: Int): Shape {
    return when {
        // If there's only one item, apply larger radius to all corners.
        listSize == 1 -> RoundedCornerShape(20.dp)
        // For the First item in the list, apply a larger radius only to upper corners.
        index == 0 ->
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp,
            )
        // For the last item in the list, apply a larger radius only to bottom corners.
        index == listSize - 1 ->
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp,
            )
        // Middle items have a smaller, uniform corner radius.
        else -> RoundedCornerShape(4.dp)
    }
}
