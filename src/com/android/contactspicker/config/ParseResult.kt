/*
 * Copyright (C) 2026 The Android Open Source Project
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

/** Enum for error result wrapping */
enum class ConfigErrorType {
    UNSUPPORTED_ACTION,
    UNSUPPORTED_MIME_TYPE,
    UNSUPPORTED_SELECTION_LIMIT,
    EMPTY_REQUESTED_MIME_TYPE,
}

/** Generic parse result for helper functions */
sealed interface ParseResult<out T> {
    data class Success<out T>(val value: T) : ParseResult<T>

    data class Error(val errorType: ConfigErrorType, val message: String) : ParseResult<Nothing>
}
