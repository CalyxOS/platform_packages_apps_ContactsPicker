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
package com.android.contactspicker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Theme based on Material You dynamic colors that observes the dark or light theme of the system.
 */
@Composable
fun ContactsPickerAppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme =
        when {
            isSystemInDarkTheme() -> dynamicDarkColorScheme(context)
            else -> dynamicLightColorScheme(context)
        }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
