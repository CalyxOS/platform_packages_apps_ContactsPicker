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

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.android.contactspicker.ContactsPickerStatsLog
import com.android.contactspicker.config.ContactsPickerAction
import com.android.contactspicker.data.model.MimeType
import javax.inject.Inject

internal const val LOADING_TIME_UNSET = -1L

class ContactsPickerLoggerImpl @Inject constructor() : ContactsPickerLogger {

    private var loggingData: LoggingSessionData? = null

    private class LoggingSessionData(
        val callingAppUid: Int,
        val callingAppTargetSdk: Int,
        val pickerIntentAction: Int,
        val requestedMimeTypes: IntArray,
        val useSystemContactsPicker: Boolean,
        val matchAllRequestedMimeTypes: Boolean,
        val sessionStartTimeMs: Long = SystemClock.elapsedRealtime(),
        var loadingContactsStartTimeMs: Long = LOADING_TIME_UNSET,
        var previewOpened: Boolean = false,
        var searchUsed: Boolean = false,
        var privacyBannerDismissed: Boolean = false,
        var loadingTimeMs: Long = LOADING_TIME_UNSET,
    )

    override fun logContactsPickerSessionStarted(
        callingAppUid: Int,
        callingAppTargetSdk: Int,
        pickerIntentAction: ContactsPickerAction,
        requestedMimeTypes: List<MimeType>,
        useSystemContactsPicker: Boolean,
        matchAllRequestedMimeTypes: Boolean,
    ) {
        val currentLoggingData =
            LoggingSessionData(
                    callingAppUid = callingAppUid,
                    callingAppTargetSdk = callingAppTargetSdk,
                    pickerIntentAction = pickerIntentAction.toLoggingEnumValue(),
                    requestedMimeTypes = requestedMimeTypes.convertToLoggingEnumList(),
                    useSystemContactsPicker = useSystemContactsPicker,
                    matchAllRequestedMimeTypes = matchAllRequestedMimeTypes,
                )
                .also { loggingData = it }

        ContactsPickerStatsLog.write(
            ContactsPickerStatsLog.CONTACTS_PICKER_SESSION_STARTED_REPORTED,
            /* calling_app_package_uid */ callingAppUid,
            /* calling_app_target_sdk */ callingAppTargetSdk,
            /* intent_action */ currentLoggingData.pickerIntentAction,
            /* requested_mimetypes */ currentLoggingData.requestedMimeTypes,
            /* intent_extra_use_system_contacts_picker */ useSystemContactsPicker,
            /* intent_extra_pick_contacts_match_all_data_fields */ matchAllRequestedMimeTypes,
        )
    }

    override fun logContactsPickerSessionFinishedSuccessfully(
        numContactsSelected: Int,
        contactsSelectedFromFavorites: Boolean,
        contactsSelectedFromSearch: Boolean,
    ) {
        val currentLoggingData = loggingData ?: return
        val sessionDurationMs =
            SystemClock.elapsedRealtime() - currentLoggingData.sessionStartTimeMs

        ContactsPickerStatsLog.write(
            ContactsPickerStatsLog.CONTACTS_PICKER_SESSION_FINISHED_REPORTED,
            /* calling_app_package_uid */ currentLoggingData.callingAppUid,
            /* calling_app_target_sdk */ currentLoggingData.callingAppTargetSdk,
            /* intent_action */ currentLoggingData.pickerIntentAction,
            /* requested_mimetypes */ currentLoggingData.requestedMimeTypes,
            /* intent_extra_use_system_contacts_picker */ currentLoggingData
                .useSystemContactsPicker,
            /* intent_extra_pick_contacts_match_all_data_fields */ currentLoggingData
                .matchAllRequestedMimeTypes,
            /* session_result */ ContactsPickerStatsLog
                .CONTACTS_PICKER_SESSION_FINISHED_REPORTED__SESSION_RESULT__SESSION_RESULT_SUCCESS,
            /* error_type */ 0, // No error
            /* session_duration_ms */ sessionDurationMs,
            /* startup_loading_time_ms */ currentLoggingData.loadingTimeMs,
            /* num_contacts_selected */ numContactsSelected,
            /* contacts_selected_from_favorites */ contactsSelectedFromFavorites,
            /* contacts_selected_from_search_results */ contactsSelectedFromSearch,
            /* preview_opened */ currentLoggingData.previewOpened,
            /* search_used */ currentLoggingData.searchUsed,
            /* count_search_load_time_above_tolerance */ 0, // TODO(b/441483549): Log long searches
            /* privacy_banner_more_details_opened_by_user */ false, // TODO(b/441483549): Log priv
            // banner opened
            /* privacy_banner_dismissed_by_user */ currentLoggingData.privacyBannerDismissed,
            /* privacy_banner_opened_from_overflow_menu */ false, // TODO(b/441483549): Log priv
            // banner opened from overflow menu
        )
    }

    override fun allContactsLoadingStarted() {
        loggingData?.let { it.loadingContactsStartTimeMs = SystemClock.elapsedRealtime() }
    }

    override fun allContactsLoadingFinished() {
        loggingData?.let {
            if (it.loadingContactsStartTimeMs == LOADING_TIME_UNSET) {
                return
            }
            val loadingTimeMs = SystemClock.elapsedRealtime() - it.loadingContactsStartTimeMs
            it.loadingTimeMs = maxOf(it.loadingTimeMs, loadingTimeMs)
            it.loadingContactsStartTimeMs = LOADING_TIME_UNSET
        }
    }

    override fun previewOpened() {
        loggingData?.let { it.previewOpened = true }
    }

    override fun searchUsed() {
        loggingData?.let { it.searchUsed = true }
    }

    override fun privacyBannerDismissedByUser() {
        loggingData?.let { it.privacyBannerDismissed = true }
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
