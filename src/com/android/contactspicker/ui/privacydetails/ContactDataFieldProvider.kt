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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import com.android.contactspicker.R
import com.android.contactspicker.data.model.MimeType
import com.android.contactspicker.ui.utils.IconResource.Painter
import com.android.contactspicker.ui.utils.IconResource.Vector

/**
 * Takes raw contact data field types and provides sorted list of UI-ready [ContactDataFieldItem]s.
 */
object ContactDataFieldProvider {

    private val NAME_DATA_FIELD_ITEM =
        ContactDataFieldItem(
            icon = Vector(Icons.Outlined.Person),
            headerTextResId = R.string.privacy_details_data_field_name_header,
            contentDescriptionResId = R.string.privacy_details_data_field_name_content_description,
        )

    private val PREFERENCES_DATA_FIELD_ITEM =
        ContactDataFieldItem(
            icon = Vector(Icons.Outlined.ManageAccounts),
            headerTextResId = R.string.privacy_details_data_field_preferences_header,
            descriptionTextResId = R.string.privacy_details_data_field_preferences_description,
            contentDescriptionResId =
                R.string.privacy_details_data_field_preferences_content_description,
        )

    // Defines the custom sort order for the data fields.
    private val sortOrder =
        listOf(
            MimeType.EMAIL,
            MimeType.PHONE,
            MimeType.STRUCTURED_POSTAL,
            MimeType.ORGANIZATION,
            MimeType.RELATION,
            MimeType.EVENT,
            MimeType.PHOTO,
            MimeType.GROUP_MEMBERSHIP,
            MimeType.NICKNAME,
            MimeType.WEBSITE,
        )

    /**
     * Create a map for efficient O(1) lookup of data fields sort priority. This is initialized only
     * once.
     */
    private val dataFieldToPriority: Map<MimeType, Int> =
        sortOrder.withIndex().associate { (index, dataField) -> dataField to index }

    /**
     * Creates a sorted list of [ContactDataFieldItem] from a list of contact data field types, with
     * preferences and name items included. Name is always added as first item, and preferences is
     * always added as last item.
     *
     * @param dataFields A list of MIME type strings from `ContactsContract.CommonDataKinds`.
     * @return A sorted list of [ContactDataFieldItem].
     */
    fun getContactDataFieldItems(dataFields: List<MimeType>): List<ContactDataFieldItem> {
        val mappedItems =
            dataFields
                // 1. Map each dataField string to a Pair of its UI model and priority.
                .mapNotNull { dataField ->
                    val item = mapSingleField(dataField)
                    val priority = dataFieldToPriority[dataField]

                    if (item != null && priority != null) {
                        item to priority
                    } else {
                        null
                    }
                }
                .sortedBy { it.second }
                .map { it.first }

        return buildList {
            add(NAME_DATA_FIELD_ITEM)
            addAll(mappedItems)
            add(PREFERENCES_DATA_FIELD_ITEM)
        }
    }

    /**
     * Maps a single data field to a [ContactDataFieldItem], returning null for unsupported types.
     */
    private fun mapSingleField(dataField: MimeType): ContactDataFieldItem? {
        return when (dataField) {
            MimeType.PHONE ->
                ContactDataFieldItem(
                    icon = Vector(Icons.Outlined.Phone),
                    headerTextResId = R.string.privacy_details_data_field_phone_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_phone_content_description,
                )

            MimeType.EMAIL ->
                ContactDataFieldItem(
                    icon = Vector(Icons.Outlined.Email),
                    headerTextResId = R.string.privacy_details_data_field_email_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_email_content_description,
                )

            MimeType.STRUCTURED_POSTAL ->
                ContactDataFieldItem(
                    icon = Vector(Icons.Outlined.Map),
                    headerTextResId = R.string.privacy_details_data_field_address_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_address_content_description,
                )

            MimeType.ORGANIZATION ->
                ContactDataFieldItem(
                    icon = Vector(Icons.Outlined.Business),
                    headerTextResId = R.string.privacy_details_data_field_organization_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_organization_content_description,
                )

            MimeType.RELATION ->
                ContactDataFieldItem(
                    icon = Painter(R.drawable.related_people),
                    headerTextResId = R.string.privacy_details_data_field_related_people_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_related_people_content_description,
                )

            MimeType.EVENT ->
                ContactDataFieldItem(
                    icon = Vector(Icons.Outlined.Cake),
                    headerTextResId = R.string.privacy_details_data_field_birthday_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_birthday_content_description,
                )

            MimeType.WEBSITE ->
                ContactDataFieldItem(
                    icon = Vector(Icons.Outlined.Link),
                    headerTextResId = R.string.privacy_details_data_field_website_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_website_content_description,
                )

            MimeType.NICKNAME ->
                ContactDataFieldItem(
                    icon = Vector(Icons.Outlined.Person),
                    headerTextResId = R.string.privacy_details_data_field_nickname_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_nickname_content_description,
                )
            MimeType.PHOTO ->
                ContactDataFieldItem(
                    icon = Vector(Icons.Outlined.AccountBox),
                    headerTextResId = R.string.privacy_details_data_field_photo_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_photo_content_description,
                )

            MimeType.GROUP_MEMBERSHIP ->
                ContactDataFieldItem(
                    icon = Vector(Icons.Outlined.Group),
                    headerTextResId = R.string.privacy_details_data_field_group_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_group_content_description,
                )

            MimeType.STRUCTURED_NAME,
            MimeType.CONTACTS -> null
        }
    }
}
