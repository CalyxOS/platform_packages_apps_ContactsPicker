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
import com.android.contactspicker.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ContactDataFieldMapperTest {

    @Test
    fun mapToSortedItems_withUnorderedInput_returnsCorrectlySortedList() {
        val unorderedDataFields =
            listOf(
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            )

        val result = ContactDataFieldMapper.mapToSortedItems(unorderedDataFields)

        val resultHeaderIds = result.map { it.headerTextResId }

        assertThat(resultHeaderIds)
            .containsExactly(
                R.string.privacy_details_data_field_email_header,
                R.string.privacy_details_data_field_phone_header,
                R.string.privacy_details_data_field_nickname_header,
            )
            .inOrder()
    }

    @Test
    fun mapToSortedItems_withUnknownDataField_ignoresUnknownField() {
        val dataFieldsWithUnknown =
            listOf(
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                "com.example.unknown.data.field",
            )

        val result = ContactDataFieldMapper.mapToSortedItems(dataFieldsWithUnknown)

        assertThat(result).hasSize(1)
        assertThat(result.first().headerTextResId)
            .isEqualTo(R.string.privacy_details_data_field_phone_header)
    }

    @Test
    fun mapToSortedItems_withEmptyInput_returnsEmptyList() {
        val emptyDataFields = emptyList<String>()
        val result = ContactDataFieldMapper.mapToSortedItems(emptyDataFields)
        assertThat(result).isEmpty()
    }

    @Test
    fun mapToSortedItems_withAllKnownFieldsReverseSorted_returnsAllFieldsSorted() {
        val allKnownDataFields =
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
                .reversed()

        val result = ContactDataFieldMapper.mapToSortedItems(allKnownDataFields)

        assertThat(result).hasSize(allKnownDataFields.size)

        val resultHeaderIds = result.map { it.headerTextResId }
        assertThat(resultHeaderIds)
            .containsExactly(
                R.string.privacy_details_data_field_name_header,
                R.string.privacy_details_data_field_email_header,
                R.string.privacy_details_data_field_phone_header,
                R.string.privacy_details_data_field_address_header,
                R.string.privacy_details_data_field_organization_header,
                R.string.privacy_details_data_field_related_people_header,
                R.string.privacy_details_data_field_birthday_header,
                R.string.privacy_details_data_field_photo_header,
                R.string.privacy_details_data_field_group_header,
                R.string.privacy_details_data_field_nickname_header,
                R.string.privacy_details_data_field_website_header,
            )
            .inOrder()
    }
}
