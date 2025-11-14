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

/** Defines the originating intent action, which controls the expected result format. */
enum class ContactsPickerAction {
    /**
     * For Intent.ACTION_PICK. Results are one or more data/contact content URIs (legacy format).
     */
    ACTION_PICK,

    /** For ContactsPickerSessionContract.ACTION_PICK_CONTACTS. Result is a single session URI. */
    ACTION_PICK_CONTACTS,
}
