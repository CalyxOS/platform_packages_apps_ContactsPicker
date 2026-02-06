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
package com.android.contactspicker.provider

import android.app.Activity
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

/**
 * An abstraction to provide the calling package name. This allows for faking the caller's identity
 * in tests.
 */
interface CallingPackageProvider {
    fun get(): String?

    fun getCallingAppUid(): Int
}

/** Production implementation that retrieves the real calling package from the Activity. */
@ActivityScoped
class CallingPackageProviderImpl @Inject constructor(private val activity: Activity) :
    CallingPackageProvider {
    override fun get(): String? = activity.callingPackage

    override fun getCallingAppUid(): Int = activity.launchedFromUid
}
