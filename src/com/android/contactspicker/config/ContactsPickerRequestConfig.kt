/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contactspicker.config

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsPickerSessionContract
import com.android.contactspicker.data.model.MimeType

/** Sealed interface to facilitate handling of errors in parsing. */
sealed interface ContactsPickerConfigResult

/**
 * Signals error in parsing of the intent. Includes parsed fields and error type for logging
 * purposes.
 */
data class ContactsPickerConfigError(
    val errorType: ConfigErrorType,
    val message: String,
    val parsedAction: ContactsPickerAction? = null,
    val parsedMimeTypes: List<MimeType>? = null,
    val parsedMatchAll: Boolean = false,
) : ContactsPickerConfigResult

/**
 * A configuration object, parsed from the incoming Intent, that dictates the contacts picker's
 * behavior and data requirements.
 *
 * @param queryMode defines the query for the contacts data to fetch from the ContactsRepository.
 * @param pickerAction The intent action that initiated the picker, which determines the result
 *   format (contacts/data uri URI vs. session URI).
 * @param isMultiSelectEnabled Whether multiple contacts/entries can be selected.
 * @param maxSelectionLimit The maximum number of items that can be selected (used for UI warnings).
 * @param requestedMimeTypes The explicit list of mimetypes requested by the caller.
 * @return [ContactsPickerRequestConfig] if success else [ContactsPickerConfigError]
 */
data class ContactsPickerRequestConfig(
    val queryMode: ContactsQueryMode,
    val pickerAction: ContactsPickerAction,
    val isMultiSelectEnabled: Boolean,
    val maxSelectionLimit: Int,
    val requestedMimeTypes: List<MimeType>,
    val matchAllRequestedMimeTypes: Boolean,
) : ContactsPickerConfigResult {
    companion object {
        /** Default selection limit if not specified for multi-select. */
        const val DEFAULT_SELECTION_LIMIT = 50

        /** Maximum selection limit allowed by the picker. */
        const val MAX_SELECTION_LIMIT = 100

        /**
         * Parses the given Intent parameters and returns a [ContactsPickerRequestConfig] or throws
         * an [IllegalArgumentException].
         *
         * @param intentAction The intent action.
         * @param intentType The intent MIME type.
         * @param intentExtras The intent extras bundle.
         * @return [ContactsPickerRequestConfig] on success.
         * @throws [IllegalArgumentException] if the intent parameters are invalid.
         */
        fun create(
            intentAction: String?,
            intentType: String?,
            intentExtras: Bundle?,
        ): ContactsPickerConfigResult {

            val pickerAction =
                getPickerAction(intentAction)
                    ?: return ContactsPickerConfigError(
                        errorType = ConfigErrorType.UNSUPPORTED_ACTION,
                        message = "Unsupported intent action: $intentAction",
                    )

            val isMultiSelect =
                intentExtras?.getBoolean(Intent.EXTRA_ALLOW_MULTIPLE, false) ?: false

            val queryModeResult =
                ContactsQueryMode.getQueryMode(pickerAction, intentType, intentExtras)
            if (queryModeResult is ParseResult.Error) {
                return ContactsPickerConfigError(
                    errorType = queryModeResult.errorType,
                    message = queryModeResult.message,
                    parsedAction = pickerAction,
                )
            }
            val queryMode = (queryModeResult as ParseResult.Success).value
            val requestedMimeTypes = queryMode.getMimeTypes()
            val matchAllRequestedMimeTypes =
                intentExtras?.getBoolean(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_MATCH_ALL_DATA_FIELDS,
                    false,
                ) ?: false

            val selectionLimitResult = getSelectionLimit(intentExtras, isMultiSelect)
            if (selectionLimitResult is ParseResult.Error) {
                return ContactsPickerConfigError(
                    errorType = selectionLimitResult.errorType,
                    message = selectionLimitResult.message,
                    parsedAction = pickerAction,
                    parsedMimeTypes = requestedMimeTypes,
                    parsedMatchAll = matchAllRequestedMimeTypes,
                )
            }

            // Success
            return ContactsPickerRequestConfig(
                queryMode = queryMode,
                pickerAction = pickerAction,
                isMultiSelectEnabled = isMultiSelect,
                maxSelectionLimit = (selectionLimitResult as ParseResult.Success).value,
                matchAllRequestedMimeTypes = matchAllRequestedMimeTypes,
                requestedMimeTypes = requestedMimeTypes,
            )
        }

        private fun getPickerAction(action: String?): ContactsPickerAction? {
            return when (action) {
                Intent.ACTION_PICK -> ContactsPickerAction.ACTION_PICK
                ContactsPickerSessionContract.ACTION_PICK_CONTACTS ->
                    ContactsPickerAction.ACTION_PICK_CONTACTS
                else -> null
            }
        }

        private fun getSelectionLimit(
            intentExtras: Bundle?,
            isMultiSelect: Boolean,
        ): ParseResult<Int> {
            if (!isMultiSelect) return ParseResult.Success(1)

            val limit =
                intentExtras?.getInt(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT,
                    DEFAULT_SELECTION_LIMIT,
                ) ?: DEFAULT_SELECTION_LIMIT

            if (limit <= 0) {
                return ParseResult.Error(
                    ConfigErrorType.UNSUPPORTED_SELECTION_LIMIT,
                    "Selection limit must be a positive number. Received $limit.",
                )
            }
            if (limit > MAX_SELECTION_LIMIT) {
                return ParseResult.Error(
                    ConfigErrorType.UNSUPPORTED_SELECTION_LIMIT,
                    "Selection limit cannot exceed $MAX_SELECTION_LIMIT. Received $limit.",
                )
            }
            return ParseResult.Success(limit)
        }
    }
}
