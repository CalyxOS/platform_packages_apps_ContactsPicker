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
import com.android.contactspicker.config.ConfigErrorType
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
        var privacyDetailsBannerOpened: Boolean = false,
        var privacyDetailsOverflowMenuOpened: Boolean = false,
        var loadingTimeMs: Long = LOADING_TIME_UNSET,
    )

    override fun logContactsPickerSessionStarted(
        callingAppUid: Int,
        callingAppTargetSdk: Int,
        pickerIntentAction: ContactsPickerAction?,
        requestedMimeTypes: List<MimeType>?,
        useSystemContactsPicker: Boolean,
        matchAllRequestedMimeTypes: Boolean,
    ) {
        val actionEnumValue =
            pickerIntentAction?.toLoggingEnumValue()
                ?: ContactsPickerStatsLog
                    .CONTACTS_PICKER_SESSION_STARTED_REPORTED__INTENT_ACTION__INTENT_ACTION_TYPE_UNSPECIFIED

        val mimeTypesEnumArray =
            requestedMimeTypes?.convertToLoggingEnumList()
                ?: intArrayOf(
                    ContactsPickerStatsLog
                        .CONTACTS_PICKER_SESSION_STARTED_REPORTED__REQUESTED_MIMETYPES__MIME_TYPE_UNSPECIFIED
                )

        val currentLoggingData =
            LoggingSessionData(
                    callingAppUid = callingAppUid,
                    callingAppTargetSdk = callingAppTargetSdk,
                    pickerIntentAction = actionEnumValue,
                    requestedMimeTypes = mimeTypesEnumArray,
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
        logSessionFinishedInternal(
            sessionResult =
                ContactsPickerStatsLog
                    .CONTACTS_PICKER_SESSION_FINISHED_REPORTED__SESSION_RESULT__SESSION_RESULT_SUCCESS,
            numContactsSelected = numContactsSelected,
            contactsSelectedFromFavorites = contactsSelectedFromFavorites,
            contactsSelectedFromSearch = contactsSelectedFromSearch,
        )
    }

    override fun logContactsPickerSessionFailed(errorType: ConfigErrorType) {
        val statsdErrorType =
            when (errorType) {
                ConfigErrorType.UNSUPPORTED_ACTION ->
                    ContactsPickerStatsLog
                        .CONTACTS_PICKER_SESSION_FINISHED_REPORTED__ERROR_TYPE__ERROR_UNSUPPORTED_ACTION
                ConfigErrorType.UNSUPPORTED_MIME_TYPE ->
                    ContactsPickerStatsLog
                        .CONTACTS_PICKER_SESSION_FINISHED_REPORTED__ERROR_TYPE__ERROR_UNSUPPORTED_MIME_TYPE
                ConfigErrorType.UNSUPPORTED_SELECTION_LIMIT ->
                    ContactsPickerStatsLog
                        .CONTACTS_PICKER_SESSION_FINISHED_REPORTED__ERROR_TYPE__ERROR_UNSUPPORTED_SELECTION_LIMIT
                // TODO(441483549): Add new ERROR_MISSING_REQUESTED_MIME_TYPE enum and change to it
                ConfigErrorType.EMPTY_REQUESTED_MIME_TYPE ->
                    ContactsPickerStatsLog
                        .CONTACTS_PICKER_SESSION_FINISHED_REPORTED__ERROR_TYPE__ERROR_UNSUPPORTED_MIME_TYPE
            }

        logSessionFinishedInternal(
            sessionResult =
                ContactsPickerStatsLog
                    .CONTACTS_PICKER_SESSION_FINISHED_REPORTED__SESSION_RESULT__SESSION_RESULT_FAILED,
            errorType = statsdErrorType,
        )
    }

    override fun logContactsPickerSessionCancelled() {
        logSessionFinishedInternal(
            sessionResult =
                ContactsPickerStatsLog
                    .CONTACTS_PICKER_SESSION_FINISHED_REPORTED__SESSION_RESULT__SESSION_RESULT_CANCELLED_BY_USER
        )
    }

    override fun logContactsPickerSessionForwarded() {
        logSessionFinishedInternal(
            sessionResult =
                ContactsPickerStatsLog
                    .CONTACTS_PICKER_SESSION_FINISHED_REPORTED__SESSION_RESULT__SESSION_RESULT_FORWARDED
        )
    }

    /**
     * Shared helper to write the final CONTACTS_PICKER_SESSION_FINISHED_REPORTED atom. Default
     * values represent a session with no selection or error.
     */
    private fun logSessionFinishedInternal(
        sessionResult: Int,
        errorType: Int = 0,
        numContactsSelected: Int = 0,
        contactsSelectedFromFavorites: Boolean = false,
        contactsSelectedFromSearch: Boolean = false,
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
            /* session_result */ sessionResult,
            /* error_type */ errorType,
            /* session_duration_ms */ sessionDurationMs,
            /* startup_loading_time_ms */ currentLoggingData.loadingTimeMs,
            /* num_contacts_selected */ numContactsSelected,
            /* contacts_selected_from_favorites */ contactsSelectedFromFavorites,
            /* contacts_selected_from_search_results */ contactsSelectedFromSearch,
            /* preview_opened */ currentLoggingData.previewOpened,
            /* search_used */ currentLoggingData.searchUsed,
            /* count_search_load_time_above_tolerance */ 0, // TODO(b/441483549): Log long searches
            /* privacy_banner_more_details_opened_by_user */ currentLoggingData
                .privacyDetailsBannerOpened,
            /* privacy_banner_dismissed_by_user */ currentLoggingData.privacyBannerDismissed,
            /* privacy_banner_opened_from_overflow_menu */ currentLoggingData
                .privacyDetailsOverflowMenuOpened,
        )

        // Clear logging data to prevent duplicate log submissions for the same session
        loggingData = null
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

    override fun privacyDetailsBannerOpened() {
        loggingData?.let { it.privacyDetailsBannerOpened = true }
    }

    override fun privacyDetailsOverflowMenuOpened() {
        loggingData?.let { it.privacyDetailsOverflowMenuOpened = true }
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
