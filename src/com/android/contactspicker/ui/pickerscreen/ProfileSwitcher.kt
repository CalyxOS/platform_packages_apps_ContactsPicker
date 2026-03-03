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
package com.android.contactspicker.ui.pickerscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.android.contactspicker.R
import com.android.contactspicker.data.model.PickerUserState
import com.android.contactspicker.data.model.UserProfile
import com.android.contactspicker.data.model.UserType

private val ACCOUNT_ICON_SIZE = 22.dp
private val DROPDOWN_ARROW_SIZE = 20.dp
private val DROPDOWN_MIN_WIDTH = 200.dp

@Composable
fun ProfileSwitcher(
    userState: PickerUserState,
    onProfileClicked: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val successState = userState as? PickerUserState.Success
    val userMap = successState?.userIdToAvailableUsersMap

    val visibleUsers =
        remember(userMap) {
            userMap
                ?.values
                ?.mapNotNull { profile -> profile.switchableInfo?.let { info -> profile to info } }
                ?.sortedBy { (profile, _) -> profile.userType } ?: emptyList()
        }

    if (visibleUsers.size <= 1) {
        return
    }

    var expanded by rememberSaveable { mutableStateOf(false) }

    val currentUser = successState?.let { it.userIdToAvailableUsersMap[it.selectedUserId] }
    val currentSwitchableInfo = currentUser?.switchableInfo

    Box(modifier = modifier, contentAlignment = Alignment.TopEnd) {
        FilledTonalButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
            colors =
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            modifier =
                Modifier.semantics { currentSwitchableInfo?.label?.let { stateDescription = it } },
        ) {
            ProfileIcon(
                icon = currentSwitchableInfo?.icon,
                userType = currentUser?.userType,
                contentDescription = stringResource(R.string.profile_switcher_content_description),
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(DROPDOWN_ARROW_SIZE),
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            visibleUsers.forEach { (userProfile, switchableInfo) ->
                val isSelected = userProfile.userId == currentUser?.userId
                val surfaceColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                val surfaceContentColor = contentColorFor(surfaceColor)
                val iconColor =
                    if (userProfile.pausedInfo != null) {
                        MenuDefaults.itemColors().disabledLeadingIconColor
                    } else {
                        surfaceContentColor
                    }

                Surface(
                    modifier = Modifier.widthIn(min = DROPDOWN_MIN_WIDTH),
                    color = surfaceColor,
                    contentColor = surfaceContentColor,
                ) {
                    DropdownMenuItem(
                        text = { Text(text = switchableInfo.label, color = surfaceContentColor) },
                        leadingIcon = {
                            ProfileIcon(
                                icon = switchableInfo.icon,
                                userType = userProfile.userType,
                                tint = iconColor,
                            )
                        },
                        onClick = {
                            expanded = false
                            onProfileClicked(userProfile)
                        },
                        modifier = Modifier.semantics { selected = isSelected },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileIcon(
    icon: ImageBitmap?,
    userType: UserType?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
) {
    if (icon != null) {
        Icon(
            bitmap = icon,
            contentDescription = contentDescription,
            modifier = modifier.size(ACCOUNT_ICON_SIZE),
            tint = tint,
        )
    } else {
        Icon(
            imageVector = getFallbackIcon(userType),
            contentDescription = contentDescription,
            modifier = modifier.size(ACCOUNT_ICON_SIZE),
            tint = tint,
        )
    }
}

@Composable
private fun getFallbackIcon(userType: UserType?): ImageVector {
    return when (userType) {
        UserType.PERSONAL -> Icons.Filled.AccountCircle
        UserType.WORK -> Icons.Filled.Work
        UserType.PRIVATE -> Icons.Filled.Lock
        else -> Icons.Filled.Person
    }
}
