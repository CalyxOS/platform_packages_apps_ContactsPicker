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

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/**
 * A wrapper around [NavHost] that provides default horizontal slide animations for transitions
 * between screens.
 *
 * The animations create a standard "push" and "pop" effect:
 * - **Forward Navigation**: The new screen slides in from the right, pushing the old screen out to
 *   the left.
 * - **Back Navigation**: The previous screen slides in from the left, as the current screen slides
 *   out to the right.
 */
@Composable
fun AnimatedNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        // When navigating to a new screen, new screen slides in from the right.
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        // When navigating to a new screen, current screen slides out to the left.
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
        // When popping back, the previous screen slides in from the left.
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
        // When popping back, the current screen slides out to the right.
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        builder = builder,
    )
}
