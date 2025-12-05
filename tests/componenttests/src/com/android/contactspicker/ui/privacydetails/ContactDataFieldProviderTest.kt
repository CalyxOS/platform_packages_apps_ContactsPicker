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

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.android.contactspicker.R
import com.android.contactspicker.data.model.MimeType
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(JUnit4::class)
class ContactDataFieldProviderTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun getContactDataFieldItems_includesNameAndPreferencesItems() {
        val dataFields = listOf(MimeType.PHONE)
        val result = ContactDataFieldProvider.getContactDataFieldItems(dataFields)

        assertThat(result.size).isEqualTo(3)

        assertThat(result.first().headerTextResId)
            .isEqualTo(R.string.privacy_details_data_field_name_header)
        assertThat(result.last().headerTextResId)
            .isEqualTo(R.string.privacy_details_data_field_preferences_header)
    }

    @Test
    fun getContactDataFieldItems_ignoresUnsupportedMimeTypes() {
        val dataFieldsWithUnsupported =
            listOf(MimeType.PHONE, MimeType.CONTACTS, MimeType.STRUCTURED_NAME)

        val result = ContactDataFieldProvider.getContactDataFieldItems(dataFieldsWithUnsupported)

        assertThat(result).hasSize(3)
        assertThat(result[1].headerTextResId)
            .isEqualTo(R.string.privacy_details_data_field_phone_header)
    }

    @Test
    fun getContactDataFieldItems_withEmptyList_returnsOnlyNameAndPreferences() {
        // Scenario: Test the behavior when the input list of data fields is empty.
        // The function should still return the default 'Name' and 'Preferences' items.
        val dataFields = emptyList<MimeType>()

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
        val supportedCommonDataKindsItemTypes = MimeType.entries.reversed()

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
        val input = listOf(MimeType.EMAIL)
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
        val input = listOf(MimeType.PHONE)
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
    fun getContactDataFieldItems_withContactsType_returnsOnlyNameAndPreferences() {
        val input = listOf(MimeType.CONTACTS)
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
