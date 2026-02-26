/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.android.contactspicker.logging

import android.content.flags.Flags
import android.os.statsd.contactspicker.ContactMimeType
import android.os.statsd.contactspicker.ContactsPickerSessionResult
import android.os.statsd.contactspicker.IntentActionType
import android.platform.test.annotations.RequiresFlagsEnabled
import android.util.StatsEvent
import android.util.StatsEventTestUtils
import android.util.StatsLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.ContactsPickerSessionFinishedReported
import com.android.contactspicker.ContactsPickerSessionStartedReported
import com.android.contactspicker.ContactspickerExtensionAtoms
import com.android.contactspicker.config.ContactsPickerAction
import com.android.contactspicker.data.model.MimeType
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ExtensionRegistryLite
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.kotlin.any
import org.mockito.quality.Strictness

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactsPickerLoggerImplTest {
    private val DEFAULT_TEST_CALLING_APP_UID = 123
    private val DEFAULT_TEST_CALLING_APP_TARGET_SDK = 33

    private val DEFAULT_NUM_SELECTED_CONTACTS = 3

    private val registry = ExtensionRegistryLite.newInstance()
    private val logger = ContactsPickerLoggerImpl()

    val capturedSessionStartedAtoms = mutableListOf<ContactsPickerSessionStartedReported>()
    val capturedSessionFinishedAtoms = mutableListOf<ContactsPickerSessionFinishedReported>()

    private lateinit var mockitoSession: MockitoSession

    @Before
    fun setUp() {
        mockitoSession =
            mockitoSession()
                .mockStatic(StatsLog::class.java)
                // lenient needed because some tests assert no interactions with static mock
                .strictness(Strictness.LENIENT)
                .startMocking()
        registry.add(ContactspickerExtensionAtoms.contactsPickerSessionStartedReported)
        registry.add(ContactspickerExtensionAtoms.contactsPickerSessionFinishedReported)
        ExtendedMockito.doAnswer { invocation ->
                val event = invocation.arguments[0] as StatsEvent
                val atom = StatsEventTestUtils.convertToAtom(event, registry)
                if (
                    atom.hasExtension(
                        ContactspickerExtensionAtoms.contactsPickerSessionStartedReported
                    )
                ) {
                    capturedSessionStartedAtoms.add(
                        atom.getExtension(
                            ContactspickerExtensionAtoms.contactsPickerSessionStartedReported
                        )
                    )
                } else if (
                    atom.hasExtension(
                        ContactspickerExtensionAtoms.contactsPickerSessionFinishedReported
                    )
                ) {
                    capturedSessionFinishedAtoms.add(
                        atom.getExtension(
                            ContactspickerExtensionAtoms.contactsPickerSessionFinishedReported
                        )
                    )
                }
                null
            }
            .`when` { StatsLog.write(any()) }
    }

    @After
    fun tearDown() {
        mockitoSession.finishMocking()
    }

    @Test
    fun logContactsPickerSessionStarted_logsCorrectCallingAppUid() {
        for (uid in listOf(1111, 1234)) {
            verifySessionStartedEventFields(callingAppPackageUid = uid)
        }
    }

    @Test
    fun logContactsPickerSessionStarted_logsCorrectCallingAppTargetSdk() {
        for (targetSdk in (35..40)) {
            verifySessionStartedEventFields(callingAppTargetSdk = targetSdk)
        }
    }

    @Test
    fun logContactsPickerSessionStarted_logsCorrectAction() {
        ContactsPickerAction.entries.forEach { intentAction ->
            verifySessionStartedEventFields(intentAction = intentAction)
        }
    }

    @Test
    fun logContactsPickerSessionStarted_logsCorrectMimeType() {
        // Check each individual
        MimeType.entries.forEach { mimeType ->
            verifySessionStartedEventFields(requestedMimetypesList = listOf(mimeType))
        }

        // Check a list of all
        verifySessionStartedEventFields(requestedMimetypesList = MimeType.entries)
    }

    @Test
    fun logContactsPickerSessionStarted_logsCorrectUseSystemContactsPicker() {
        for (useSystemContactsPicker in listOf(true, false)) {
            verifySessionStartedEventFields(useSystemContactsPicker = useSystemContactsPicker)
        }
    }

    @Test
    fun logContactsPickerSessionStarted_logsCorrectMatchAllRequestedMimeTypes() {
        for (matchAllRequestedMimeTypes in listOf(true, false)) {
            verifySessionStartedEventFields(matchAllRequestedMimeTypes = matchAllRequestedMimeTypes)
        }
    }

    @Test
    fun logContactsPickerSessionFinished_noSessionStartedCalled_doesNotLog() {
        capturedSessionFinishedAtoms.clear()
        logger.logContactsPickerSessionFinishedSuccessfully(DEFAULT_NUM_SELECTED_CONTACTS)

        assertThat(capturedSessionFinishedAtoms).isEmpty()
    }

    @Test
    fun logContactsPickerSessionFinished_logsCorrectNumSelectedContacts() {
        for (numSelectedContacts in listOf(1, 10, 100)) verifySessionFinishedEventFields(
            numSelectedContacts = numSelectedContacts
        )
    }

    @Test
    fun logContactsPickerSessionFinished_logsCorrectFieldsFromStartSessionCall() {
        verifySessionFinishedEventFields(
            callingAppPackageUid = 9876,
            callingAppTargetSdk = 40,
            intentAction = ContactsPickerAction.ACTION_PICK,
            requestedMimetypesList =
                listOf(MimeType.PHONE, MimeType.EMAIL, MimeType.STRUCTURED_NAME),
            useSystemContactsPicker = true,
            matchAllRequestedMimeTypes = true,
            numSelectedContacts = 12,
        )
    }

    private fun verifySessionStartedEventFields(
        callingAppPackageUid: Int = DEFAULT_TEST_CALLING_APP_UID,
        callingAppTargetSdk: Int = DEFAULT_TEST_CALLING_APP_TARGET_SDK,
        intentAction: ContactsPickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
        requestedMimetypesList: List<MimeType> = listOf(MimeType.PHONE, MimeType.STRUCTURED_NAME),
        useSystemContactsPicker: Boolean = false,
        matchAllRequestedMimeTypes: Boolean = false,
    ) {
        capturedSessionStartedAtoms.clear()

        logger.logContactsPickerSessionStarted(
            callingAppPackageUid,
            callingAppTargetSdk,
            intentAction,
            requestedMimetypesList,
            useSystemContactsPicker,
            matchAllRequestedMimeTypes,
        )

        assertThat(capturedSessionStartedAtoms).hasSize(1)
        val event = capturedSessionStartedAtoms[0]

        assertThat(event.callingAppPackageUid).isEqualTo(callingAppPackageUid)
        assertThat(event.callingAppTargetSdk).isEqualTo(callingAppTargetSdk)
        assertThat(event.intentAction)
            .isEqualTo(IntentActionType.forNumber(intentAction.toLoggingEnumValue()))
        assertThat(event.requestedMimetypesList)
            .containsExactlyElementsIn(
                requestedMimetypesList.convertToLoggingEnumList().map {
                    ContactMimeType.forNumber(it)
                }
            )
        assertThat(event.intentExtraUseSystemContactsPicker).isEqualTo(useSystemContactsPicker)
        assertThat(event.intentExtraPickContactsMatchAllDataFields)
            .isEqualTo(matchAllRequestedMimeTypes)
    }

    private fun verifySessionFinishedEventFields(
        callingAppPackageUid: Int = DEFAULT_TEST_CALLING_APP_UID,
        callingAppTargetSdk: Int = DEFAULT_TEST_CALLING_APP_TARGET_SDK,
        intentAction: ContactsPickerAction = ContactsPickerAction.ACTION_PICK_CONTACTS,
        requestedMimetypesList: List<MimeType> = listOf(MimeType.PHONE, MimeType.STRUCTURED_NAME),
        useSystemContactsPicker: Boolean = false,
        matchAllRequestedMimeTypes: Boolean = false,
        sessionResult: ContactsPickerSessionResult =
            ContactsPickerSessionResult.SESSION_RESULT_SUCCESS,
        numSelectedContacts: Int = DEFAULT_NUM_SELECTED_CONTACTS,
    ) {
        capturedSessionFinishedAtoms.clear()

        // have to log session started to populate the fields
        logger.logContactsPickerSessionStarted(
            callingAppPackageUid,
            callingAppTargetSdk,
            intentAction,
            requestedMimetypesList,
            useSystemContactsPicker,
            matchAllRequestedMimeTypes,
        )

        logger.logContactsPickerSessionFinishedSuccessfully(numSelectedContacts)

        assertThat(capturedSessionFinishedAtoms).hasSize(1)
        val event = capturedSessionFinishedAtoms[0]

        assertThat(event.callingAppPackageUid).isEqualTo(callingAppPackageUid)
        assertThat(event.callingAppTargetSdk).isEqualTo(callingAppTargetSdk)
        assertThat(event.intentAction)
            .isEqualTo(IntentActionType.forNumber(intentAction.toLoggingEnumValue()))
        assertThat(event.requestedMimetypesList)
            .containsExactlyElementsIn(
                requestedMimetypesList.convertToLoggingEnumList().map {
                    ContactMimeType.forNumber(it)
                }
            )
        assertThat(event.intentExtraUseSystemContactsPicker).isEqualTo(useSystemContactsPicker)
        assertThat(event.intentExtraPickContactsMatchAllDataFields)
            .isEqualTo(matchAllRequestedMimeTypes)
        assertThat(event.sessionResult).isEqualTo(sessionResult)
        assertThat(event.numContactsSelected).isEqualTo(numSelectedContacts)
    }
}
