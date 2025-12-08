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
 */
data class ContactsPickerRequestConfig(
    val queryMode: ContactsQueryMode,
    val pickerAction: ContactsPickerAction,
    val isMultiSelectEnabled: Boolean,
    val maxSelectionLimit: Int,
    // TODO(b/463940164): revisit deriving mime types from ContactsQueryMode
    val requestedMimeTypes: List<String>,
    val matchAllRequestedMimeTypes: Boolean,
) {
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
        ): ContactsPickerRequestConfig {
            val pickerAction = getPickerAction(intentAction)
            val isMultiSelect =
                intentExtras?.getBoolean(Intent.EXTRA_ALLOW_MULTIPLE, false) ?: false
            val (queryMode, requestedMimeTypes) =
                ContactsQueryMode.getQueryModeAndMimeTypes(pickerAction, intentType, intentExtras)
            val selectionLimit = getSelectionLimit(intentExtras, isMultiSelect)

            val matchAllRequestedMimeTypes =
                (queryMode as? ContactsQueryMode.Custom)?.matchAllRequestedMimeTypes ?: false

            return ContactsPickerRequestConfig(
                queryMode = queryMode,
                pickerAction = pickerAction,
                isMultiSelectEnabled = isMultiSelect,
                maxSelectionLimit = selectionLimit,
                requestedMimeTypes = requestedMimeTypes,
                matchAllRequestedMimeTypes = matchAllRequestedMimeTypes,
            )
        }

        private fun getPickerAction(action: String?): ContactsPickerAction {
            return when (action) {
                Intent.ACTION_PICK -> ContactsPickerAction.ACTION_PICK
                ContactsPickerSessionContract.ACTION_PICK_CONTACTS ->
                    ContactsPickerAction.ACTION_PICK_CONTACTS

                else -> throw IllegalArgumentException("Unsupported intent action: $action")
            }
        }

        private fun getSelectionLimit(intentExtras: Bundle?, isMultiSelect: Boolean): Int {
            if (!isMultiSelect) {
                return 1
            }

            val defaultLimit = DEFAULT_SELECTION_LIMIT
            val maxLimit = MAX_SELECTION_LIMIT

            val limit =
                intentExtras?.getInt(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT,
                    defaultLimit,
                ) ?: defaultLimit

            if (limit <= 0) {
                throw IllegalArgumentException(
                    "Selection limit must be a positive number. Received $limit."
                )
            }
            if (limit > maxLimit) {
                throw IllegalArgumentException(
                    "Selection limit cannot exceed $maxLimit. Received $limit."
                )
            }
            return limit
        }
    }
}
