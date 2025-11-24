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
class ContactDataFieldProviderTest {

    @Test
    fun getContactDataFieldItems_includesNameAndPreferencesItems() {
        val dataFields = listOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        val result = ContactDataFieldProvider.getContactDataFieldItems(dataFields)

        assertThat(result.size).isEqualTo(3)

        assertThat(result.first().headerTextResId)
            .isEqualTo(R.string.privacy_details_data_field_name_header)
        assertThat(result.last().headerTextResId)
            .isEqualTo(R.string.privacy_details_data_field_preferences_header)
    }

    @Test
    fun getContactDataFieldItems_ignoresUnsupportedDataField() {
        val dataFieldsWithUnsupported =
            listOf(
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                "com.example.unknown.data.field",
            )

        val result = ContactDataFieldProvider.getContactDataFieldItems(dataFieldsWithUnsupported)

        assertThat(result).hasSize(3)
        assertThat(result[1].headerTextResId)
            .isEqualTo(R.string.privacy_details_data_field_phone_header)
    }

    @Test
    fun getContactDataFieldItems_withEmptyList_returnsOnlyNameAndPreferences() {
        // Scenario: Test the behavior when the input list of data fields is empty.
        // The function should still return the default 'Name' and 'Preferences' items.
        val dataFields = emptyList<String>()

        // Act: Call the function with an empty list.
        val result = ContactDataFieldProvider.getContactDataFieldItems(dataFields)

        // Assert: The result should contain only the 'Name' and 'Preferences' items, in that order.
        assertThat(result).hasSize(2)
        assertThat(result.first().headerTextResId)
            .isEqualTo(R.string.privacy_details_data_field_name_header)
        assertThat(result.last().headerTextResId)
            .isEqualTo(R.string.privacy_details_data_field_preferences_header)
    }

    @Test
    fun getContactDataFieldItems_withCommonDataKindsItemTypes_returnsSortedList() {
        val supportedCommonDataKindsItemTypes =
            listOf(
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

        val result =
            ContactDataFieldProvider.getContactDataFieldItems(supportedCommonDataKindsItemTypes)

        assertThat(result).hasSize(12)

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
                R.string.privacy_details_data_field_preferences_header,
            )
            .inOrder()
    }

    @Test
    fun getContactDataFieldItems_withCommonDataKindsEmailType_returnsCorrectList() {
        val input = listOf(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE)
        // Act
        val result = ContactDataFieldProvider.getContactDataFieldItems(input)
        // Assert
        val resultHeaderIds = result.map { it.headerTextResId }
        assertThat(resultHeaderIds)
            .containsExactly(
                R.string.privacy_details_data_field_name_header,
                R.string.privacy_details_data_field_email_header,
                R.string.privacy_details_data_field_preferences_header,
            )
            .inOrder()
    }

    @Test
    fun getContactDataFieldItems_withCommonDataKindsPhoneType_returnsCorrectList() {
        val input = listOf(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)
        // Act
        val result = ContactDataFieldProvider.getContactDataFieldItems(input)
        // Assert
        val resultHeaderIds = result.map { it.headerTextResId }
        assertThat(resultHeaderIds)
            .containsExactly(
                R.string.privacy_details_data_field_name_header,
                R.string.privacy_details_data_field_phone_header,
                R.string.privacy_details_data_field_preferences_header,
            )
            .inOrder()
    }

    @Test
    fun getContactDataFieldItems_withCommonDataKindsContactType_returnsOnlyNameAndPreferences() {
        val input = listOf(ContactsContract.Contacts.CONTENT_TYPE)
        // Act
        val result = ContactDataFieldProvider.getContactDataFieldItems(input)
        // Assert
        val resultHeaderIds = result.map { it.headerTextResId }
        assertThat(resultHeaderIds)
            .containsExactly(
                R.string.privacy_details_data_field_name_header,
                R.string.privacy_details_data_field_preferences_header,
            )
            .inOrder()
    }
}
