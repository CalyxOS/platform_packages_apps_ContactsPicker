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
package com.android.contactspicker.ui.utils

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents an icon resource that can be either a Jetpack Compose [ImageVector] or an XML drawable
 * resource.
 */
sealed class IconResource {
    /** A vector-based icon from [androidx.compose.material.icons.Icons]. */
    data class Vector(val imageVector: ImageVector) : IconResource()

    /** An icon from a drawable resource ID (e.g., R.drawable.my_icon). */
    data class Painter(@DrawableRes val id: Int) : IconResource()
}
