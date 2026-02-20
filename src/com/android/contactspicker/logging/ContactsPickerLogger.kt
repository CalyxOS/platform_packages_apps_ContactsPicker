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

import com.android.contactspicker.config.ContactsPickerAction
import com.android.contactspicker.data.model.MimeType

interface ContactsPickerLogger {
    fun logContactsPickerSessionStarted(
        callingAppUid: Int,
        callingAppTargetSdk: Int,
        pickerIntentAction: ContactsPickerAction,
        requestedMimeTypes: List<MimeType>,
        useSystemContactsPicker: Boolean,
        matchAllRequestedMimeTypes: Boolean,
    )

    fun logContactsPickerSessionFinishedSuccessfully(
        numContactsSelected: Int,
        contactsSelectedFromFavorites: Boolean,
        contactsSelectedFromSearch: Boolean,
    )
}
