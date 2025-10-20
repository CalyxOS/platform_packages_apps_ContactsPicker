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

import android.content.Context
import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.ContactsContract
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.R
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class PrivacyDetailsBodyTest {
    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val SAMPLE_APP_NAME = "Sample_App"

    @Test
    fun privacyDetailsBody_displaysAllElements() {
        val dataFields =
            listOf(
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            )

        composeTestRule.setContent {
            PrivacyDetailsBody(appName = SAMPLE_APP_NAME, requestedDataFields = dataFields)
        }

        val expectedDescription =
            context.getString(R.string.privacy_details_description, SAMPLE_APP_NAME)
        composeTestRule.onNodeWithText(expectedDescription).assertIsDisplayed()

        val expectedHeader =
            context.getString(R.string.privacy_details_contact_data_fields_list_header)
        composeTestRule.onNodeWithText(expectedHeader).assertIsDisplayed()

        val expectedPhoneText = context.getString(R.string.privacy_details_data_field_phone_header)
        val expectedEmailText = context.getString(R.string.privacy_details_data_field_email_header)
        composeTestRule.onNodeWithText(expectedPhoneText).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedEmailText).assertIsDisplayed()
    }

    @Test
    fun getShapeForListPosition_singleItem_returnsFullyRoundedShape() {
        val shape = getShapeForListPosition(index = 0, listSize = 1)
        assertThat(shape).isEqualTo(RoundedCornerShape(20.dp))
    }

    @Test
    fun getShapeForListPosition_firstOfMany_returnsTopRoundedShape() {
        val shape = getShapeForListPosition(index = 0, listSize = 3)
        val expectedShape =
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp,
            )
        assertThat(shape).isEqualTo(expectedShape)
    }

    @Test
    fun getShapeForListPosition_lastOfMany_returnsBottomRoundedShape() {
        val shape = getShapeForListPosition(index = 2, listSize = 3)
        val expectedShape =
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp,
            )
        assertThat(shape).isEqualTo(expectedShape)
    }

    @Test
    fun getShapeForListPosition_middleItem_returnsSlightlyRoundedShape() {
        val shape = getShapeForListPosition(index = 1, listSize = 3)
        assertThat(shape).isEqualTo(RoundedCornerShape(4.dp))
    }
}
