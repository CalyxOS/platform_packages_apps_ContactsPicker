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
package com.android.contactspicker.navigation

/**
 * Defines a type-safe contract for all navigation destinations within the Contacts Picker.
 *
 * @see ContactsPickerNavHost where these routes are used to build the navigation graph.
 */
sealed interface NavigationRoute {
    val route: String
}

/**
 * Represents the primary screen that displays the list of contacts for the user to pick from. This
 * is the start destination of the navigation graph.
 */
object ContactsPickerRoute : NavigationRoute {
    override val route = "contacts_picker"
}

/**
 * Represents the screen that displays detailed privacy information about how contact data will be
 * shared with the calling application.
 */
object PrivacyDetailsRoute : NavigationRoute {
    override val route = "privacy_details"
}
