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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.android.contactspicker.ContactsUiState
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.PickerUserStates
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.ui.pickerscreen.ContactsPickerScreen
import com.android.contactspicker.ui.privacydetails.PrivacyDetailsScreen

@Composable
fun ContactsPickerNavHost(
    navController: NavHostController,
    uiState: State<ContactsUiState>,
    userStates: PickerUserStates?,
    onToggleContactSelection: (Contact) -> Unit,
    onToggleEntrySelection: (Long, Long) -> Unit,
    onPrivacyBannerDismissRequest: () -> Unit,
    onExpandRequest: () -> Unit,
    onQueryChange: (String) -> Unit,
    onExitSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onBackFromPreview: () -> Unit,
    onProfileClicked: (UserProfile) -> Unit,
    onDismissProfileBlockedDialog: () -> Unit,
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = ContactsPickerRoute.route,
        modifier = modifier,
    ) {
        composable(ContactsPickerRoute.route) {
            ContactsPickerScreen(
                uiState = uiState,
                userStates = userStates,
                onToggleContactSelection = onToggleContactSelection,
                onToggleEntrySelection = onToggleEntrySelection,
                onNavigateToPrivacyDetails = {
                    navController.navigateToAndPopUpToStart(PrivacyDetailsRoute.route)
                },
                onPrivacyBannerDismissRequest = onPrivacyBannerDismissRequest,
                onExpandRequest = onExpandRequest,
                onQueryChange = onQueryChange,
                onExitSearch = onExitSearch,
                onBackFromPreview = onBackFromPreview,
                onProfileClicked = onProfileClicked,
                onDismissProfileBlockedDialog = onDismissProfileBlockedDialog,
            )
        }

        composable(PrivacyDetailsRoute.route) {
            PrivacyDetailsScreen(
                onBackPressed = { navController.popBackStack() },
                uiState = uiState,
            )
        }
    }
}

/**
 * Configures navigation with specific backstack and state management behaviors:
 * 1. `launchSingleTop`: Prevents creating a new instance of a destination if it's already at the
 *    top of the backstack. This avoids stacks like [A, B, B].
 * 2. `saveState` & `restoreState`: Together, these flags preserve a screen's saveable UI state even
 *    after it has been popped from the stack and is later revisited.
 * 3. `popUpTo(startDestination)`: Clears the navigation stack back to the start destination before
 *    navigating. This keeps the backstack shallow, ensuring a maximum of two destinations in our
 *    graph at any time.
 */
private fun NavHostController.navigateToAndPopUpToStart(route: String) =
    this.navigate(route) {
        popUpTo(this@navigateToAndPopUpToStart.graph.findStartDestination().id) {
            saveState = true
            inclusive = false
        }
        launchSingleTop = true
        restoreState = true
    }
