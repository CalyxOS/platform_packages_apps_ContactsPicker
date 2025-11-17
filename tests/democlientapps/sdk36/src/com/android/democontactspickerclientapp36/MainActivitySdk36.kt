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

package com.android.democontactspickerclientapp36

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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

class MainActivitySdk36 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { Sdk36Screen(applicationContext.applicationInfo.targetSdkVersion) }
        }
    }
}

@Composable
private fun Sdk36Screen(targetSdk: Int) {
    var legacyConfig by remember { mutableStateOf(LegacyDemoConfigState()) }
    var allowMultiple by remember { mutableStateOf(false) }
    var useSystemPicker by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val pickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Handle picker result to get structured data
            val pickerResult = handlePickerResult(context, result, legacyConfig.legacyPickerType)
            // Update state with structured results and status text
            legacyConfig = legacyConfig.copy(pickerResult = pickerResult)
        }

    Box(Modifier.systemBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScreenTitle(targetSdk = targetSdk)
            Spacer(modifier = Modifier.height(20.dp))

            LegacyActionPickConfiguration(legacyConfig) { newConfig -> legacyConfig = newConfig }
            Spacer(modifier = Modifier.height(20.dp))

            CommonOptions(allowMultiple) { allowMultiple = it }
            if (android.content.flags.Flags.enableSystemContactsPicker()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text("Use System Picker", style = MaterialTheme.typography.bodyLarge)
                    }
                    Switch(checked = useSystemPicker, onCheckedChange = { useSystemPicker = it })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LaunchPickerButton {
                val intent = buildLegacyPickerIntent(legacyConfig, allowMultiple, useSystemPicker)
                try {
                    pickerLauncher.launch(intent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(
                            context,
                            "No handler for intent ${intent.action} found.",
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Pass status and structured results to the display composable
            ResultDisplay(legacyConfig.pickerResult)
        }
    }
}
