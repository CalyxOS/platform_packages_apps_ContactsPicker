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
package com.android.contactspicker.fakes

import android.net.Uri
import com.android.contactspicker.data.repository.ContactsPickerSessionProviderRepository

class FakeContactsPickerSessionProviderRepository : ContactsPickerSessionProviderRepository {

    private val results = mutableMapOf<Pair<List<Long>, Int>, Uri>()

    /**
     * Registers a specific Session URI to be returned when createSession is called with the exact
     * [dataIds] and [callingUid].
     */
    fun setSessionResult(dataIds: List<Long>, callingUid: Int, resultUri: Uri) {
        results[dataIds to callingUid] = resultUri
    }

    override suspend fun createSession(dataIds: List<Long>, callingUid: Int): Uri {
        return results[dataIds to callingUid] ?: Uri.EMPTY
    }
}
