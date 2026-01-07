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

import android.Manifest
import android.app.ApplicationPackageManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_EXCLUDE_COMPONENTS
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Trace
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.android.contactspicker.provider.CallingPackageProvider
import com.android.contactspicker.ui.components.ContactsPickerBottomSheet
import com.android.contactspicker.ui.theme.ContactsPickerAppTheme
import com.android.contactspicker.viewmodel.ContactsViewModel
import com.android.contactspicker.viewmodel.PickerResultEvent
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

    @Inject lateinit var appPackageManager: ApplicationPackageManager

    @Inject lateinit var callingPackageProvider: CallingPackageProvider

    private val contactsViewModel: ContactsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Trace.beginSection("$TAG#coldStart")
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            setupComposeUi()
            return
        }

        routeIntent(intent)
        Trace.endSection()
    }

    override fun onNewIntent(intent: Intent) {
        Trace.beginSection("$TAG#onNewIntent")
        super.onNewIntent(intent)
        setIntent(intent)
        routeIntent(intent)
        Trace.endSection()
    }

    /**
     * Determines the correct handling for the intent based on the presence of the
     * [Intent.EXTRA_USE_SYSTEM_CONTACTS_PICKER] extra and the calling package target SDK.
     */
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
            val callingAppName = appPackageManager.getApplicationLabel(appInfo)?.toString()
            if (
                intent.getBooleanExtra(Intent.EXTRA_USE_SYSTEM_CONTACTS_PICKER, false) ||
                    appInfo.targetSdkVersion >= ACTION_PICK_TAKEOVER_TARGET_SDK_THRESHOLD ||
                    Flags.enableActionPickTakeoverInDroidfood()
            ) {
                // It's safe to handle internally. Process the data and show the UI.
                Log.d(
                    TAG,
                    "Handling ${intent.action} for $callingPackage (targetSDK=${appInfo.targetSdkVersion}) internally.",
                )
                processIntentAndSetupUi(intent, callingAppName, appInfo.uid)
            } else {
                Log.d(
                    TAG,
                    "Forwarding ACTION_PICK for $callingPackage (targetSDK=${appInfo.targetSdkVersion}) to system.",
                )
                val targetIntent =
                    Intent(intent).apply {
                        component = null // Ensure it's implicit
                    }
                // First forward to a preferred handler if set
                if (forwardToPreferredActionPickHandler(targetIntent)) {
                    return
                }
                // Otherwise forward to *all* handlers (except this app). Chooser Activity will be
                // started in case more than one handler (except this app) is present.
                forwardToOtherActionPickHandlersWithChooser(targetIntent)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Calling package not found: $callingPackage", e)
            processIntentAndSetupUi(intent, null, -1)
        }
    }

    /**
     * Forwards the intent to a preselected preferred handler, if present.
     *
     * @return `true` if the intent was forwarded, `false` otherwise.
     */
    private fun forwardToPreferredActionPickHandler(targetIntent: Intent): Boolean {
        val filters = mutableListOf<IntentFilter>()
        val activities = mutableListOf<ComponentName>()
        appPackageManager.getPreferredActivities(filters, activities, null)

        val preferredActivity =
            filters.zip(activities).find { (filter, _) ->
                // Only match preferred activities within the current user's context, as reading
                // cross-profile contacts data will anyways fail, even if the user selects a
                // cross-profile app, due to CP2 limitation.
                filter.match(contentResolver, targetIntent, false, TAG) > 0
            }

        if (preferredActivity != null) {
            val (_, component) = preferredActivity
            Log.d(TAG, "Starting Preferred activity. Component: $component")
            val preferredActivityIntent = Intent(intent).apply { this.component = component }
            startActivity(preferredActivityIntent)
            finish()
            return true
        }

        Log.d(TAG, "No PreferredActivity Found")
        return false
    }

    // Processes the intent which will trigger querying CP2 for contacts and sets up the UI.
    private fun processIntentAndSetupUi(intent: Intent, appName: String?, appUid: Int) {
        contactsViewModel.processIntent(
            intentAction = intent.action,
            intentType = intent.resolveType(this),
            intentExtras = intent.extras,
            callingAppName = appName,
            callingAppUid = appUid,
        )
        setupComposeUi()
    }

    // Sets up the Compose UI. Should be used only when the contacts are already loaded, e.g. on
    // configuration change.
    private fun setupComposeUi() {
        setContent {
            LaunchedEffect(Unit) {
                contactsViewModel.pickerResultEvents.collect { event ->
                    when (event) {
                        is PickerResultEvent.SetResultAndFinish -> {
                            setResult(RESULT_OK, event.intent)
                            finish()
                        }
                        is PickerResultEvent.CancelAndFinish -> {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    }
                }
            }

            val uiState = contactsViewModel.uiState.collectAsState()
            ContactsPickerAppTheme {
                ReadContactsPermissionCheckedContent(contactsViewModel) {
                    ContactsPickerBottomSheet(
                        onDismissRequest = { finish() },
                        uiState = uiState,
                        snackbarEvents = contactsViewModel.snackbarEvents,
                        onToggleContactSelection = contactsViewModel::toggleContactSelection,
                        onToggleEntrySelection = contactsViewModel::toggleEntrySelection,
                        onClearSelection = contactsViewModel::clearSelection,
                        onPrivacyBannerDismissRequest = contactsViewModel::hidePrivacyBanner,
                        onDoneClicked = contactsViewModel::onDoneClicked,
                        onQueryChange = contactsViewModel::onSearchQueryChanged,
                        onExitSearch = contactsViewModel::exitSearch,
                        onPreviewClicked = contactsViewModel::onPreviewClicked,
                        onBackFromPreview = contactsViewModel::onBackFromPreview,
                    )
                }
            }
        }
    }

    private fun forwardToOtherActionPickHandlersWithChooser(targetIntent: Intent) {
        val excludedComponents = arrayOf(ComponentName(this, ContactsPickerActivity::class.java))
        val chooserIntent =
            Intent.createChooser(
                    targetIntent,
                    getString(R.string.contacts_picker_chooser_activity_title),
                )
                .apply {
                    putExtra(EXTRA_EXCLUDE_COMPONENTS, excludedComponents)

                    addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)

                    if (targetIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    if (targetIntent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0) {
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

    // TODO(b/12345678): remove once the permission is pregranted
    private fun hasReadContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    }

    @Composable
    fun ReadContactsPermissionCheckedContent(
        viewModel: ContactsViewModel,
        content: @Composable () -> Unit,
    ) {
        val context = LocalContext.current as ComponentActivity

        var hasPermission by remember { mutableStateOf(hasReadContactsPermission(context)) }
        var permissionResultProcessed by remember { mutableStateOf(hasPermission) }

        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                isGranted ->
                val newlyGranted = isGranted && !hasPermission
                hasPermission = isGranted
                permissionResultProcessed = true

                if (newlyGranted) {
                    Log.i(TAG, "Permission newly granted by launcher.")
                    viewModel.onContactsPermissionGranted()
                }
            }

        // Request permission on initial composition if not already granted.
        LaunchedEffect(Unit) {
            if (!hasPermission) {
                launcher.launch(Manifest.permission.READ_CONTACTS)
            }
        }

        // Effect to sync permission state when the Activity resumes
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            val currentSystemPermission = hasReadContactsPermission(context)
            if (currentSystemPermission != hasPermission) {
                hasPermission = currentSystemPermission
                permissionResultProcessed = true

                if (currentSystemPermission) { // newly granted
                    Log.i(TAG, "Permission newly granted (e.g., in settings).")
                    viewModel.onContactsPermissionGranted()
                }
            }
        }

        when {
            hasPermission -> {
                content() // Display the main content
            }
            permissionResultProcessed -> {
                // Permission is not granted, and the system dialog has been shown at least once.
                // Show the fallback dialog.
                PermanentPermissionDeniedDialog(
                    onOpenSettings = {
                        Log.i(TAG, "Dialog: Open App Settings")
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        context.startActivity(intent)
                    },
                    onClose = {
                        Log.i(TAG, "Dialog: Close Picker")
                        context.finish()
                    },
                )
            }
            else -> {
                // Initial load, permission not granted, system dialog is likely being shown.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    @Composable
    fun PermanentPermissionDeniedDialog(onOpenSettings: () -> Unit, onClose: () -> Unit) {
        AlertDialog(
            onDismissRequest = { /* Intentionally non-dismissable by clicking outside */ },
            title = { Text("Permission Required") },
            text = {
                Text(
                    "You have to grant the Contacts permission to the System Contacts Picker. Please enable it in the app settings."
                )
            },
            confirmButton = { Button(onClick = onOpenSettings) { Text("Open Settings") } },
            dismissButton = { Button(onClick = onClose) { Text("Close Picker") } },
        )
    }
}
