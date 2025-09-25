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

import android.provider.ContactsContract
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import com.android.contactspicker.R
import com.android.contactspicker.ui.utils.IconResource

/** Maps raw contact data field types to a sorted list of UI-ready [ContactDataFieldItem]s. */
object ContactDataFieldMapper {

    // Defines the custom sort order for the data fields.
    private val sortOrder =
        listOf(
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE,
        )

    /**
     * Create a map for efficient O(1) lookup of data fields sort priority. This is initialized only
     * once.
     */
    private val dataFieldToPriority: Map<String, Int> =
        sortOrder.withIndex().associate { (index, dataField) -> dataField to index }

    // TODO(b/446667017): Handle ContactsContract.Contacts.CONTENT_TYPE input and default enrichment
    // of name and contact preferences
    /*
     * Creates a sorted list of [ContactDataFieldItem] from a list of contact data field types.
     *
     * @param Currently dataFields A list of MIME type strings from `ContactsContract.CommonDataKinds`.
     * @return A sorted list of [ContactDataFieldItem].
     */
    fun mapToSortedItems(dataFields: List<String>): List<ContactDataFieldItem> {
        return dataFields
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
    }

    /** Maps a single data field to a [ContactDataFieldItem], returning null for unknown types. */
    private fun mapSingleField(dataField: String): ContactDataFieldItem? {
        return when (dataField) {
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE ->
                ContactDataFieldItem(
                    icon = IconResource.Vector(Icons.Outlined.Person),
                    headerTextResId = R.string.privacy_details_data_field_name_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_name_content_description,
                )

            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE ->
                ContactDataFieldItem(
                    icon = IconResource.Vector(Icons.Outlined.Phone),
                    headerTextResId = R.string.privacy_details_data_field_phone_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_phone_content_description,
                )

            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE ->
                ContactDataFieldItem(
                    icon = IconResource.Vector(Icons.Outlined.Email),
                    headerTextResId = R.string.privacy_details_data_field_email_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_email_content_description,
                )

            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE ->
                ContactDataFieldItem(
                    icon = IconResource.Vector(Icons.Outlined.Map),
                    headerTextResId = R.string.privacy_details_data_field_address_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_address_content_description,
                )

            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE ->
                ContactDataFieldItem(
                    icon = IconResource.Vector(Icons.Outlined.Business),
                    headerTextResId = R.string.privacy_details_data_field_organization_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_organization_content_description,
                )

            ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE ->
                ContactDataFieldItem(
                    icon = IconResource.Painter(R.drawable.related_people),
                    headerTextResId = R.string.privacy_details_data_field_related_people_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_related_people_content_description,
                )

            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE ->
                ContactDataFieldItem(
                    icon = IconResource.Vector(Icons.Outlined.Cake),
                    headerTextResId = R.string.privacy_details_data_field_birthday_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_birthday_content_description,
                )

            ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE ->
                ContactDataFieldItem(
                    icon = IconResource.Vector(Icons.Outlined.Link),
                    headerTextResId = R.string.privacy_details_data_field_website_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_website_content_description,
                )

            ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE ->
                ContactDataFieldItem(
                    icon = IconResource.Vector(Icons.Outlined.Person),
                    headerTextResId = R.string.privacy_details_data_field_nickname_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_nickname_content_description,
                )
            ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE ->
                ContactDataFieldItem(
                    icon = IconResource.Vector(Icons.Outlined.AccountBox),
                    headerTextResId = R.string.privacy_details_data_field_photo_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_photo_content_description,
                )

            ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE ->
                ContactDataFieldItem(
                    icon = IconResource.Vector(Icons.Outlined.Group),
                    headerTextResId = R.string.privacy_details_data_field_group_header,
                    contentDescriptionResId =
                        R.string.privacy_details_data_field_group_content_description,
                )
            else -> null
        }
    }
}
