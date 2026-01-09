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
package com.android.contactspicker.ui.scrubber

import android.content.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.android.contactspicker.ui.pickerscreen.SectionKey
import com.google.common.truth.Truth.assertThat
import java.util.SortedMap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(JUnit4::class)
class ScrubberPositionToListIndexMapperTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun toListIndex_noContacts_returnsZero() {
        val mapper = ScrubberPositionToListIndexMapper(sortedMapOf(), showPrivacyBanner = false)
        assertThat(mapper.toListIndex(0.5f)).isEqualTo(0)
        assertThat(mapper.toListIndex(1f)).isEqualTo(0)
    }

    @Test
    fun toListIndex_singleSection_noPrivacyBanner_mapsCorrectly() {
        val contacts = ContactTestDataFactory.createContactList(11)
        val sections: SortedMap<SectionKey, List<Contact>> =
            sortedMapOf(SectionKey.LetterKey('A') to contacts)
        val mapper = ScrubberPositionToListIndexMapper(sections, showPrivacyBanner = false)
        // Expected Result is (11 - 1)*verticalOffsetFraction + stickyHeaderOffset(1)
        assertThat(mapper.toListIndex(0f)).isEqualTo(0)
        assertThat(mapper.toListIndex(0.4f)).isEqualTo(5)
        assertThat(mapper.toListIndex(0.8f)).isEqualTo(9)
        assertThat(mapper.toListIndex(1f)).isEqualTo(11)
    }

    @Test
    fun toListIndex_singleSection_withPrivacyBanner_mapsCorrectly() {
        val contacts = ContactTestDataFactory.createContactList(11)
        val sections: SortedMap<SectionKey, List<Contact>> =
            sortedMapOf(SectionKey.LetterKey('A') to contacts)
        val mapper = ScrubberPositionToListIndexMapper(sections, showPrivacyBanner = true)
        // Expected Result is (11 - 1)*verticalOffsetFraction + stickyHeaderOffset(1) +
        // PrivacyBannerOffset(1)
        assertThat(mapper.toListIndex(0f)).isEqualTo(0)
        assertThat(mapper.toListIndex(0.4f)).isEqualTo(6)
        assertThat(mapper.toListIndex(0.8f)).isEqualTo(10)
        assertThat(mapper.toListIndex(1f)).isEqualTo(12)
    }

    @Test
    fun toListIndex_multipleSections_noPrivacyBanner_mapsCorrectly() {
        val contacts = ContactTestDataFactory.createContactList(21)
        val sections: SortedMap<SectionKey, List<Contact>> =
            sortedMapOf(
                SectionKey.LetterKey('A') to contacts.subList(0, 5),
                SectionKey.LetterKey('B') to contacts.subList(5, 10),
                SectionKey.LetterKey('C') to contacts.subList(10, 15),
                SectionKey.LetterKey('D') to contacts.subList(15, 21),
            )
        val mapper = ScrubberPositionToListIndexMapper(sections, showPrivacyBanner = false)
        // Edge case where we map 0f verticalOffsetFraction to top of the list, instead of first
        // contact
        assertThat(mapper.toListIndex(0f)).isEqualTo(0)
        // Expected Result is (21 - 1)*verticalOffsetFraction + stickyHeaderOffset(1)
        assertThat(mapper.toListIndex(0.2f)).isEqualTo(5)
        // Expected Result is (21 - 1)*verticalOffsetFraction + stickyHeaderOffset(2)
        assertThat(mapper.toListIndex(0.4f)).isEqualTo(10)
        // Expected Result is (21 - 1)*verticalOffsetFraction + stickyHeaderOffset(3)
        assertThat(mapper.toListIndex(0.6f)).isEqualTo(15)
        // Expected Result is (21 - 1)*verticalOffsetFraction + stickyHeaderOffset(4)
        assertThat(mapper.toListIndex(0.8f)).isEqualTo(20)
        // Expected Result is (21 - 1)*verticalOffsetFraction + stickyHeaderOffset(4)
        assertThat(mapper.toListIndex(1f)).isEqualTo(24)
    }

    @Test
    fun toListIndex_multipleSections_withPrivacyBanner() {
        val contacts = ContactTestDataFactory.createContactList(21)
        val sections: SortedMap<SectionKey, List<Contact>> =
            sortedMapOf(
                SectionKey.LetterKey('A') to contacts.subList(0, 5),
                SectionKey.LetterKey('B') to contacts.subList(5, 10),
                SectionKey.LetterKey('C') to contacts.subList(10, 15),
                SectionKey.LetterKey('D') to contacts.subList(15, 21),
            )
        val mapper = ScrubberPositionToListIndexMapper(sections, showPrivacyBanner = true)
        // Edge case where we map 0f verticalOffsetFraction to top of the list, instead of first
        // contact
        assertThat(mapper.toListIndex(0f)).isEqualTo(0)
        // Expected Result is (21 - 1)*verticalOffsetFraction + stickyHeaderOffset(1) +
        // PrivacyBannerOffset(1)
        assertThat(mapper.toListIndex(0.2f)).isEqualTo(6)
        // Expected Result is (21 - 1)*verticalOffsetFraction + stickyHeaderOffset(2) +
        // PrivacyBannerOffset(1)
        assertThat(mapper.toListIndex(0.4f)).isEqualTo(11)
        // Expected Result is (21 - 1)*verticalOffsetFraction + stickyHeaderOffset(3) +
        // PrivacyBannerOffset(1)
        assertThat(mapper.toListIndex(0.6f)).isEqualTo(16)
        // Expected Result is (21 - 1)*verticalOffsetFraction + stickyHeaderOffset4) +
        // PrivacyBannerOffset(1)
        assertThat(mapper.toListIndex(0.8f)).isEqualTo(21)
        // Expected Result is (21 - 1)*verticalOffsetFraction + stickyHeaderOffset(4) +
        // PrivacyBannerOffset(1)
        assertThat(mapper.toListIndex(1f)).isEqualTo(25)
    }

    @Test
    fun toVerticalOffsetFraction_noContacts_returnsZero() {
        val mapper = ScrubberPositionToListIndexMapper(sortedMapOf(), showPrivacyBanner = false)
        assertThat(mapper.toVerticalOffsetFraction(10f)).isEqualTo(0f)
    }

    @Test
    fun toVerticalOffsetFraction_SingleContact_returnsZero() {
        val sections: SortedMap<SectionKey, List<Contact>> =
            sortedMapOf(
                SectionKey.LetterKey('A') to
                    listOf(ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT)
            )

        val mapper = ScrubberPositionToListIndexMapper(sections, showPrivacyBanner = false)
        assertThat(mapper.toVerticalOffsetFraction(10f)).isEqualTo(0f)
    }

    @Test
    fun toVerticalOffsetFraction_multipleSections_noPrivacyBanner_mapsCorrectly() {
        val contacts = ContactTestDataFactory.createContactList(11)
        val sections: SortedMap<SectionKey, List<Contact>> =
            sortedMapOf(
                SectionKey.LetterKey('A') to contacts.subList(0, 5),
                SectionKey.LetterKey('B') to contacts.subList(5, 11),
            )
        val mapper = ScrubberPositionToListIndexMapper(sections, showPrivacyBanner = false)
        // First section sticky header maps to 0f verticalOffsetFraction
        assertThat(mapper.toVerticalOffsetFraction(0f)).isEqualTo(0f)
        // First contact in first section maps to 0f verticalOffsetFraction
        assertThat(mapper.toVerticalOffsetFraction(1f)).isEqualTo(0f)
        // Expected value is (listIndex - stickyHeader(1)) / (11 - 1)
        assertThat(mapper.toVerticalOffsetFraction(2f)).isEqualTo(1f / 10f)
        assertThat(mapper.toVerticalOffsetFraction(5f)).isWithin(0.01f).of(4f / 10f)
        // Second section sticky header maps to same position as first contact in second section
        assertThat(mapper.toVerticalOffsetFraction(6f)).isWithin(0.01f).of(5f / 10f)
        // Expected value is (listIndex - stickyHeader(2)) / (11 - 1)
        assertThat(mapper.toVerticalOffsetFraction(7f)).isWithin(0.01f).of(5f / 10f)
        assertThat(mapper.toVerticalOffsetFraction(9f)).isWithin(0.01f).of(7f / 10f)
        assertThat(mapper.toVerticalOffsetFraction(11f)).isWithin(0.01f).of(9f / 10f)
        assertThat(mapper.toVerticalOffsetFraction(12f)).isEqualTo(1f)
    }

    @Test
    fun toVerticalOffsetFraction_multipleSections_withPrivacyBanner() {
        val contacts = ContactTestDataFactory.createContactList(11)
        val sections: SortedMap<SectionKey, List<Contact>> =
            sortedMapOf(
                SectionKey.LetterKey('A') to contacts.subList(0, 5),
                SectionKey.LetterKey('B') to contacts.subList(5, 11),
            )
        val mapper = ScrubberPositionToListIndexMapper(sections, showPrivacyBanner = true)

        // First item is PrivacyBanner
        assertThat(mapper.toVerticalOffsetFraction(0f)).isEqualTo(0f)
        // First section sticky header maps to 0f verticalOffsetFraction
        assertThat(mapper.toVerticalOffsetFraction(1f)).isEqualTo(0f)
        // First contact in first section maps to 0f verticalOffsetFraction
        assertThat(mapper.toVerticalOffsetFraction(2f)).isEqualTo(0f)
        // Expected value is (listIndex - stickyHeader(1) - PrivacyBanner(1)) / (11 - 1)
        assertThat(mapper.toVerticalOffsetFraction(3f)).isEqualTo(1f / 10f)
        assertThat(mapper.toVerticalOffsetFraction(5f)).isWithin(0.01f).of(3f / 10f)
        assertThat(mapper.toVerticalOffsetFraction(6f)).isWithin(0.01f).of(4f / 10f)
        // Second section sticky header maps to same position as first contact in second section
        assertThat(mapper.toVerticalOffsetFraction(7f)).isWithin(0.01f).of(5f / 10f)
        // Expected value is (listIndex - stickyHeader(2) - privacyBanner(1)) / (11 - 1)
        assertThat(mapper.toVerticalOffsetFraction(8f)).isWithin(0.01f).of(5f / 10f)
        assertThat(mapper.toVerticalOffsetFraction(10f)).isWithin(0.01f).of(7f / 10f)
        assertThat(mapper.toVerticalOffsetFraction(12f)).isWithin(0.01f).of(9f / 10f)
        assertThat(mapper.toVerticalOffsetFraction(13f)).isEqualTo(1f)
    }

    @Test
    fun toVerticalOffsetFraction_preciseListIndex_mapsCorrectly() {
        val contacts = ContactTestDataFactory.createContactList(11)
        val sections: SortedMap<SectionKey, List<Contact>> =
            sortedMapOf(
                SectionKey.LetterKey('A') to contacts.subList(0, 5),
                SectionKey.LetterKey('B') to contacts.subList(5, 11),
            )
        val mapper = ScrubberPositionToListIndexMapper(sections, showPrivacyBanner = false)

        // The scrubber's fraction is based on the contact index, not the layout index. This means
        // we must subtract non-contact items (headers, banners) from the calculation.
        //
        // Formula: (preciseListIndex - nonContactOffset) / totalContacts
        //
        // Example (1 header, 10 contacts, preciseListIndex of 1.5f):
        // (1.5f - 1) / 10 = 0.5f / 10 = 0.05f
        assertThat(mapper.toVerticalOffsetFraction(1.5f)).isWithin(0.01f).of(0.05f)
    }
}
