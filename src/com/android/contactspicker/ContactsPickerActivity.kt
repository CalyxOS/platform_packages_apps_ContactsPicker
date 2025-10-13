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
package com.android.contactspicker

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.EXTRA_EXCLUDE_COMPONENTS
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.android.contactspicker.provider.CallingPackageProvider
import com.android.contactspicker.ui.components.ContactsPickerBottomSheet
import com.android.contactspicker.ui.theme.ContactsPickerAppTheme
import com.android.contactspicker.viewmodel.ContactsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** The main activity for the Contacts Picker system app. */
@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint(ComponentActivity::class)
class ContactsPickerActivity : Hilt_ContactsPickerActivity() {

    companion object {
        private const val TAG = "ContactsPickerActivity"

        /**
         * The minimum target SDK of the caller app that will be handled by the app. Intents from
         * callers with target SDK below will be resent to the system with explicitly excluding the
         * current activity to prevent loops.
         */
        private const val ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD = 37
    }

    @Inject lateinit var appPackageManager: PackageManager

    @Inject lateinit var callingPackageProvider: CallingPackageProvider

    private val contactsViewModel: ContactsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            setupComposeUi()
            return
        }

        routeIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeIntent(intent)
    }

    /** Determines the correct handling for the intent based on the calling package target SDK. */
    private fun routeIntent(intent: Intent) {
        val callingPackage = callingPackageProvider.get()
        if (callingPackage == null) {
            Log.e(TAG, "Cannot get calling package. Finishing with RESULT_CANCELED.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        try {
            val appInfo = appPackageManager.getApplicationInfo(callingPackage, 0)
            if (appInfo.targetSdkVersion >= ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD) {
                // It's safe to handle internally. Process the data and show the UI.
                Log.d(
                    TAG,
                    "Handling ACTION_PICK for $callingPackage (targetSDK=${appInfo.targetSdkVersion}) internally.",
                )
                processIntentAndSetupUi(intent)
            } else {
                Log.d(
                    TAG,
                    "Forwarding ACTION_PICK for $callingPackage (targetSDK=${appInfo.targetSdkVersion}) to system.",
                )
                forwardToOtherActionPickHandlersWithChooser()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Calling package not found: $callingPackage", e)
            processIntentAndSetupUi(intent)
        }
    }

    // Processes the intent which will trigger querying CP2 for contacts and sets up the UI.
    private fun processIntentAndSetupUi(intent: Intent) {
        contactsViewModel.processIntent(intent.action, intent.type)
        setupComposeUi()
    }

    // Sets up the Compose UI. Should be used only when the contacts are already loaded, e.g. on
    // configuration change.
    private fun setupComposeUi() {
        setContent {
            val uiState by contactsViewModel.uiState.collectAsState()
            ContactsPickerAppTheme {
                ContactsPickerBottomSheet(
                    onDismissRequest = { finish() },
                    uiState = uiState,
                    onToggleContactSelection = contactsViewModel::toggleContactSelection,
                    onToggleEntrySelection = contactsViewModel::toggleEntrySelection,
                    onClearSelection = contactsViewModel::clearSelection,
                )
            }
        }
    }

    private fun forwardToOtherActionPickHandlersWithChooser() {
        val originalIntent = intent
        val targetIntent =
            Intent(originalIntent).apply {
                component = null // Ensure it's implicit
            }

        val excludedComponents = arrayOf(ComponentName(this, ContactsPickerActivity::class.java))

        val chooserIntent =
            Intent.createChooser(
                    targetIntent,
                    getString(R.string.contacts_picker_chooser_activity_title),
                )
                .apply {
                    putExtra(EXTRA_EXCLUDE_COMPONENTS, excludedComponents)

                    addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)

                    if (originalIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    if (originalIntent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0) {
                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                }

        try {
            startActivity(chooserIntent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No Activity found to handle the intent: $targetIntent", e)
            Toast.makeText(
                    this,
                    getString(R.string.contacts_picker_chooser_activity_no_app_can_handle_action),
                    Toast.LENGTH_SHORT,
                )
                .show()
            setResult(RESULT_CANCELED)
        }
        finish()
    }
}
