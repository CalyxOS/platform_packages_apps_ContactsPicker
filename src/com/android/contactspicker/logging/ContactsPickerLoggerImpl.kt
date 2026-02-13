/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.android.contactspicker.logging

import androidx.annotation.VisibleForTesting
import com.android.contactspicker.ContactsPickerStatsLog
import com.android.contactspicker.config.ContactsPickerAction
import com.android.contactspicker.data.model.MimeType
import javax.inject.Inject

class ContactsPickerLoggerImpl @Inject constructor() : ContactsPickerLogger {

    override fun logContactsPickerSessionStarted(
        callingAppUid: Int,
        callingAppTargetSdk: Int,
        pickerIntentAction: ContactsPickerAction,
        requestedMimeTypes: List<MimeType>,
        useSystemContactsPicker: Boolean,
        matchAllRequestedMimeTypes: Boolean,
    ) {
        ContactsPickerStatsLog.write(
            ContactsPickerStatsLog.CONTACTS_PICKER_SESSION_STARTED_REPORTED,
            /* calling_app_package_uid */ callingAppUid,
            /* calling_app_target_sdk */ callingAppTargetSdk,
            /* intent_action */ pickerIntentAction.toLoggingEnumValue(),
            /* requested_mimetypes */
            requestedMimeTypes.convertToLoggingEnumList(),
            /* intent_extra_use_system_contacts_picker */ useSystemContactsPicker,
            /* intent_extra_pick_contacts_match_all_data_fields */ matchAllRequestedMimeTypes,
        )
    }
}

@VisibleForTesting
internal fun ContactsPickerAction.toLoggingEnumValue(): Int =
    when (this) {
        ContactsPickerAction.ACTION_PICK_CONTACTS ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__INTENT_ACTION__INTENT_ACTION_TYPE_ACTION_PICK_CONTACTS
        ContactsPickerAction.ACTION_PICK ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__INTENT_ACTION__INTENT_ACTION_TYPE_ACTION_PICK
    }

@VisibleForTesting
internal fun List<MimeType>.convertToLoggingEnumList(): IntArray =
    this.map { mimeType -> mimeType.toLoggingEnumValue() }.toIntArray()

private fun MimeType.toLoggingEnumValue(): Int =
    when (this) {
        MimeType.STRUCTURED_NAME ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_NAME
        MimeType.PHONE ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_PHONE
        MimeType.EMAIL ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_EMAIL
        MimeType.STRUCTURED_POSTAL ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_POSTAL_ADDRESS
        MimeType.ORGANIZATION ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_ORGANISATION
        MimeType.RELATION ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_RELATION
        MimeType.EVENT ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_EVENT
        MimeType.PHOTO ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_PHOTO
        MimeType.GROUP_MEMBERSHIP ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_GROUP_MEMBERSHIP
        MimeType.WEBSITE ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_WEBSITE
        MimeType.NICKNAME ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_NICKNAME
        MimeType.CONTACTS ->
            ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_FULL_CONTACT
    }
