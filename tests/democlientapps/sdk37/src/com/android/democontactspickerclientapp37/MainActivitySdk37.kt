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
package com.android.democontactspickerclientapp37

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.democontactspickerclientapp.CommonOptions
import com.android.democontactspickerclientapp.LaunchPickerButton
import com.android.democontactspickerclientapp.LegacyActionPickConfiguration
import com.android.democontactspickerclientapp.LegacyDemoConfigState
import com.android.democontactspickerclientapp.ResultDisplay
import com.android.democontactspickerclientapp.ScreenTitle
import com.android.democontactspickerclientapp.buildLegacyPickerIntent
import com.android.democontactspickerclientapp.handlePickerResult

enum class Sdk37IntentType(val label: String) {
    LEGACY_ACTION_PICK("Legacy ACTION_PICK"),
    NEW_ACTION_PICK_CONTACTS("New ACTION_PICK_CONTACTS"),
}

class MainActivitySdk37 : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme { Sdk37Screen(applicationContext.applicationInfo.targetSdkVersion) }
        }
    }
}

@Composable
private fun Sdk37Screen(targetSdk: Int) {
    var legacyConfig by remember { mutableStateOf(LegacyDemoConfigState()) }
    var allowMultiple by remember { mutableStateOf(false) }
    var intentType by remember { mutableStateOf(Sdk37IntentType.NEW_ACTION_PICK_CONTACTS) }
    var selectedMimeTypes by remember { mutableStateOf(setOf(MimeType.EMAIL)) }
    val context = LocalContext.current

    val pickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val pickerResult = handlePickerResult(context, result, legacyConfig.legacyPickerType)
            legacyConfig = legacyConfig.copy(pickerResult = pickerResult)
        }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenTitle(targetSdk = targetSdk)
        Spacer(modifier = Modifier.height(20.dp))

        Sdk37IntentTypeSelector(intentType) { intentType = it }
        Spacer(modifier = Modifier.height(12.dp))

        when (intentType) {
            Sdk37IntentType.LEGACY_ACTION_PICK -> {
                LegacyActionPickConfiguration(legacyConfig) { newConfig ->
                    legacyConfig = newConfig
                }
            }

            Sdk37IntentType.NEW_ACTION_PICK_CONTACTS -> {
                ActionPickContactsConfiguration(selectedMimeTypes) { mimeTypes ->
                    selectedMimeTypes = mimeTypes
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        CommonOptions(allowMultiple) { allowMultiple = it }
        Spacer(modifier = Modifier.height(24.dp))

        LaunchPickerButton {
            val intent =
                when (intentType) {
                    Sdk37IntentType.LEGACY_ACTION_PICK ->
                        buildLegacyPickerIntent(legacyConfig, allowMultiple)

                    Sdk37IntentType.NEW_ACTION_PICK_CONTACTS ->
                        buildActionPickContactsIntent(selectedMimeTypes, allowMultiple, context)
                }
            intent?.let {
                try {
                    pickerLauncher.launch(it)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(
                            context,
                            "No handler for intent ${intent.action} found.",
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        ResultDisplay(legacyConfig.pickerResult)
    }
}
