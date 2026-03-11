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

import android.content.Context
import android.content.flags.Flags
import android.icu.text.MessageFormat
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.contactspicker.R
import com.android.contactspicker.data.model.Contact
import com.android.contactspicker.data.model.DisplayNameContact
import com.android.contactspicker.data.model.EmailContact
import com.android.contactspicker.data.model.PhoneContact
import com.android.contactspicker.testdata.ContactTestDataFactory
import com.android.contactspicker.ui.components.AVATAR_TEST_TAG
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SYSTEM_CONTACTS_PICKER)
@RunWith(AndroidJUnit4::class)
class ContactItemTest {

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun contactItem_withDisplayNameContact_showsNameOnly() {
        val testDisplayNameContact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        createContactItemWithEmptySelection(testDisplayNameContact)

        composeTestRule.onNodeWithText(testDisplayNameContact.displayName).assertIsDisplayed()
        assertRowClickLabel(testDisplayNameContact.displayName, null)
    }

    @Test
    fun contactItem_withSingleEmail_showsNameAndEmail() {
        val testSingleEmailContact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        createContactItemWithEmptySelection(testSingleEmailContact)

        composeTestRule.onNodeWithText(testSingleEmailContact.displayName).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(testSingleEmailContact.emails.first().address)
            .assertIsDisplayed()
        assertRowClickLabel(testSingleEmailContact.displayName, null)
    }

    @Test
    fun contactItem_withSinglePhone_showsNameAndPhoneNumber() {
        val testSinglePhoneContact = ContactTestDataFactory.GENERIC_PHONE_CONTACT
        createContactItemWithEmptySelection(testSinglePhoneContact)

        composeTestRule.onNodeWithText(testSinglePhoneContact.displayName).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(testSinglePhoneContact.phones.first().number)
            .assertIsDisplayed()
        assertRowClickLabel(testSinglePhoneContact.displayName, null)
    }

    @Test
    fun contactItem_withMultipleEmails_showsEmailCountAndIsExpandable() {
        val testMultiEmailContact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        createContactItemWithEmptySelection(testMultiEmailContact)

        val totalCountMessage =
            totalCountMessage(R.string.contact_item_emails_count, testMultiEmailContact.emails.size)
        composeTestRule.onNodeWithText(testMultiEmailContact.displayName).assertIsDisplayed()
        composeTestRule.onNodeWithText(totalCountMessage).assertIsDisplayed()

        val expandLabel = context.getString(R.string.contact_item_expand_button_content_description)
        assertRowClickLabel(testMultiEmailContact.displayName, expandLabel)

        // Emails should not be visible initially
        composeTestRule
            .onNodeWithText(testMultiEmailContact.emails.first().address)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(testMultiEmailContact.emails.last().address)
            .assertDoesNotExist()
    }

    @Test
    fun contactItem_withMultipleEmails_partialSelection_showsSelectedCount() {
        val testMultiEmailContact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        val totalCount = testMultiEmailContact.emails.size
        // Select only the first email
        val selectedEntries = setOf(testMultiEmailContact.emails.first().id)

        createContactItem(testMultiEmailContact, selectedEntries)

        val selectedCountMessage =
            selectedCountMessage(
                R.string.contact_item_emails_selected_count,
                totalCount,
                selectedEntries.size,
            )
        composeTestRule.onNodeWithText(selectedCountMessage).assertIsDisplayed()

        // The total count string is not be displayed
        val totalCountMessage = totalCountMessage(R.string.contact_item_emails_count, totalCount)
        composeTestRule.onNodeWithText(totalCountMessage).assertDoesNotExist()
    }

    @Test
    fun contactItem_withMultiplePhones_showsPhoneCountAndIsExpandable() {
        val testMultiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        createContactItemWithEmptySelection(testMultiPhoneContact)

        val totalCountMessage =
            totalCountMessage(R.string.contact_item_phones_count, testMultiPhoneContact.phones.size)

        composeTestRule.onNodeWithText(testMultiPhoneContact.displayName).assertIsDisplayed()
        composeTestRule.onNodeWithText(totalCountMessage).assertIsDisplayed()

        val expandLabel = context.getString(R.string.contact_item_expand_button_content_description)
        assertRowClickLabel(testMultiPhoneContact.displayName, expandLabel)

        // Phones should not be visible initially
        composeTestRule
            .onNodeWithText(testMultiPhoneContact.phones.first().number)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(testMultiPhoneContact.phones.last().number)
            .assertDoesNotExist()
    }

    @Test
    fun contactItem_withMultiplePhones_partialSelection_showsSelectedCount() {
        val testMultiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        val totalCount = testMultiPhoneContact.phones.size
        // Select only the first phone
        val selectedEntries = setOf(testMultiPhoneContact.phones.first().id)

        createContactItem(testMultiPhoneContact, selectedEntries)

        val selectedCountMessage =
            selectedCountMessage(
                R.string.contact_item_phones_selected_count,
                testMultiPhoneContact.phones.size,
                selectedEntries.size,
            )

        // Shows "1 of X phone numbers"
        composeTestRule.onNodeWithText(selectedCountMessage).assertIsDisplayed()

        // The total count string is not displayed
        composeTestRule
            .onNodeWithText(totalCountMessage(R.string.contact_item_phones_count, totalCount))
            .assertDoesNotExist()
    }

    @Test
    fun contactItem_withMultipleEmails_expandsAndCollapsesOnClick() {
        val testMultiEmailContact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        createContactItemWithEmptySelection(testMultiEmailContact)

        val expandLabel = context.getString(R.string.contact_item_expand_button_content_description)
        val collapseLabel =
            context.getString(R.string.contact_item_collapse_button_content_description)

        // Expand
        composeTestRule
            .onNodeWithText(testMultiEmailContact.displayName, useUnmergedTree = true)
            .performClick()

        assertRowClickLabel(testMultiEmailContact.displayName, collapseLabel)

        testMultiEmailContact.emails.forEach { email ->
            composeTestRule.onNodeWithText(email.address).assertIsDisplayed()
            assertThat(email.label).isNotNull()
            composeTestRule.onNodeWithText(email.label!!).assertIsDisplayed()
        }

        // Collapse
        composeTestRule.onNodeWithText(testMultiEmailContact.displayName).performClick()

        assertRowClickLabel(testMultiEmailContact.displayName, expandLabel)

        composeTestRule
            .onNodeWithText(testMultiEmailContact.emails.first().address)
            .assertDoesNotExist()
    }

    @Test
    fun contactItem_withMultiplePhones_expandsAndCollapsesOnClick() {
        val testMultiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        createContactItemWithEmptySelection(testMultiPhoneContact)

        val expandLabel = context.getString(R.string.contact_item_expand_button_content_description)
        val collapseLabel =
            context.getString(R.string.contact_item_collapse_button_content_description)

        // Expand
        composeTestRule.onNodeWithText(testMultiPhoneContact.displayName).performClick()

        assertRowClickLabel(testMultiPhoneContact.displayName, collapseLabel)

        testMultiPhoneContact.phones.forEach { phone ->
            composeTestRule.onNodeWithText(phone.number).assertIsDisplayed()
            assertThat(phone.label).isNotNull()
            composeTestRule.onNodeWithText(phone.label!!).assertIsDisplayed()
        }

        // Collapse
        composeTestRule.onNodeWithText(testMultiPhoneContact.displayName).performClick()

        assertRowClickLabel(testMultiPhoneContact.displayName, expandLabel)

        composeTestRule
            .onNodeWithText(testMultiPhoneContact.phones.first().number)
            .assertDoesNotExist()
    }

    @Test
    fun contactItem_phoneContact_secondaryTextHasVerbatimAnnotation() {
        val testSinglePhoneContact = ContactTestDataFactory.GENERIC_PHONE_CONTACT
        createContactItemWithEmptySelection(testSinglePhoneContact)

        val phoneNumber = testSinglePhoneContact.phones.first().number
        val node = composeTestRule.onNodeWithText(phoneNumber).fetchSemanticsNode()

        val annotatedString =
            node.config[SemanticsProperties.Text].find { it.text == phoneNumber }
                ?: error("Phone number text not found in semantics node")

        // Explicitly verify the VerbatimTtsAnnotation exists
        val annotations = annotatedString.getTtsAnnotations(0, phoneNumber.length)
        val hasVerbatimAnnotation = annotations.any { it.item is VerbatimTtsAnnotation }
        assertThat(hasVerbatimAnnotation).isTrue()
    }

    @Test
    fun contactItem_emailContact_secondaryTextDoesNotHaveVerbatimAnnotation() {
        val testSinglePhoneContact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        createContactItemWithEmptySelection(testSinglePhoneContact)

        val email = testSinglePhoneContact.emails.first().address
        val node = composeTestRule.onNodeWithText(email).fetchSemanticsNode()

        val annotatedString =
            node.config[SemanticsProperties.Text].find { it.text == email }
                ?: error("Email address text not found in semantics node")

        // Explicitly verify the VerbatimTtsAnnotation does not exist
        val annotations = annotatedString.getTtsAnnotations(0, email.length)
        val hasVerbatimAnnotation = annotations.any { it.item is VerbatimTtsAnnotation }
        assertThat(hasVerbatimAnnotation).isFalse()
    }

    @Test
    fun contactItem_withMultiplePhones_hasVerbatimAnnotation() {
        val testMultiPhoneContact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        createContactItemWithEmptySelection(testMultiPhoneContact)
        // Expand
        composeTestRule.onNodeWithText(testMultiPhoneContact.displayName).performClick()

        testMultiPhoneContact.phones.forEach { phone ->
            val node = composeTestRule.onNodeWithText(phone.number).fetchSemanticsNode()

            val annotatedString =
                node.config[SemanticsProperties.Text].find { it.text == phone.number }
                    ?: error("Phone number text not found in semantics node")

            // Explicitly verify the VerbatimTtsAnnotation exists
            val annotations = annotatedString.getTtsAnnotations(0, phone.number.length)
            val hasVerbatimAnnotation = annotations.any { it.item is VerbatimTtsAnnotation }
            assertThat(hasVerbatimAnnotation).isTrue()
        }
    }

    @Test
    fun contactItem_withMultipleEmails_doesNotHaveVerbatimAnnotation() {
        val testMultiEmailContact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        createContactItemWithEmptySelection(testMultiEmailContact)
        // Expand
        composeTestRule.onNodeWithText(testMultiEmailContact.displayName).performClick()

        testMultiEmailContact.emails.forEach { email ->
            val node = composeTestRule.onNodeWithText(email.address).fetchSemanticsNode()

            val annotatedString =
                node.config[SemanticsProperties.Text].find { it.text == email.address }
                    ?: error("Email address text not found in semantics node")

            // Explicitly verify the VerbatimTtsAnnotation does not exist
            val annotations = annotatedString.getTtsAnnotations(0, email.address.length)
            val hasVerbatimAnnotation = annotations.any { it.item is VerbatimTtsAnnotation }
            assertThat(hasVerbatimAnnotation).isFalse()
        }
    }

    @Test
    fun secondaryText_phoneNumberInRtl_isForcedLtr() {
        val contact = ContactTestDataFactory.GENERIC_PHONE_CONTACT

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ContactItem(
                    contact = contact,
                    position = ItemPosition.ONLY,
                    selectedEntries = emptySet(),
                    isMultiSelectEnabled = true,
                    isSearchMode = false,
                    onToggleContactSelection = {},
                    onToggleEntrySelection = { _, _ -> },
                )
            }
        }
        composeTestRule
            .onNodeWithText(contact.phones.first().number, useUnmergedTree = true)
            .assertTextDirection(TextDirection.Ltr, ResolvedTextDirection.Ltr)
    }

    @Test
    fun secondaryText_multiPhoneInRtl_expanded_isForcedLtr() {
        val contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ContactItem(
                    contact = contact,
                    position = ItemPosition.ONLY,
                    selectedEntries = emptySet(),
                    isMultiSelectEnabled = true,
                    isSearchMode = false,
                    onToggleContactSelection = {},
                    onToggleEntrySelection = { _, _ -> },
                )
            }
        }
        // expand
        composeTestRule.onNodeWithText(contact.displayName).performClick()

        contact.phones.forEach { phone ->
            composeTestRule
                .onNodeWithText(phone.number)
                .assertTextDirection(TextDirection.Ltr, ResolvedTextDirection.Ltr)
        }
    }

    private fun SemanticsNodeInteraction.assertTextDirection(
        requested: TextDirection,
        resolved: ResolvedTextDirection,
    ) {
        val results = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layoutResult = results.first()

        val actualRequested =
            layoutResult.layoutInput.text.paragraphStyles.firstOrNull()?.item?.textDirection
                ?: TextDirection.Unspecified
        assertThat(actualRequested).isEqualTo(requested)

        val actualResolved = layoutResult.getParagraphDirection(0)
        assertThat(actualResolved).isEqualTo(resolved)
    }

    @Test
    fun selectableAvatar_showsCheckmark_whenFullySelected() {
        val testMultiEmailContact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        val selectedEntries = testMultiEmailContact.emails.map { it.id }.toSet()
        createContactItem(testMultiEmailContact, selectedEntries)

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(
                    R.string.contact_item_selected_content_description,
                    testMultiEmailContact.displayName,
                ),
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
    }

    @Test
    fun selectableAvatar_showsCheckmark_whenPartiallySelected() {
        val testMultiEmailContact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        // Select only the first email
        val selectedEntries = setOf(testMultiEmailContact.emails.first().id)
        createContactItem(testMultiEmailContact, selectedEntries)

        // Checkmark is displayed
        composeTestRule
            .onNodeWithContentDescription(
                context.getString(
                    R.string.contact_item_selected_content_description,
                    testMultiEmailContact.displayName,
                ),
                useUnmergedTree = true,
            )
            .assertIsDisplayed()

        // Avatar with initial is not displayed instead
        val initial = testMultiEmailContact.displayName.first()
        composeTestRule
            .onNode(
                hasTestTag(AVATAR_TEST_TAG) and hasAnyDescendant(hasText(initial.toString())),
                useUnmergedTree = true,
            )
            .assertDoesNotExist()
    }

    @Test
    fun expandedEntry_inMultiSelectMode_showsCheckbox() {
        val testMultiEmailContact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        composeTestRule.setContent {
            ContactItem(
                contact = testMultiEmailContact,
                position = ItemPosition.ONLY,
                selectedEntries = emptySet(),
                isMultiSelectEnabled = true,
                isSearchMode = false,
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }

        // Expand
        composeTestRule.onNodeWithText(testMultiEmailContact.displayName).performClick()

        // Verify the expanded entry has Checkbox role
        composeTestRule
            .onNodeWithText(testMultiEmailContact.emails.first().address)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
    }

    @Test
    fun expandedEntry_inSingleSelectMode_showsRadioButton() {
        val testMultiEmailContact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        composeTestRule.setContent {
            ContactItem(
                contact = testMultiEmailContact,
                position = ItemPosition.ONLY,
                selectedEntries = emptySet(),
                isMultiSelectEnabled = false,
                isSearchMode = false,
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }

        // Expand
        composeTestRule.onNodeWithText(testMultiEmailContact.displayName).performClick()

        // Verify the expanded entry has RadioButton role
        composeTestRule
            .onNodeWithText(testMultiEmailContact.emails.first().address)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }

    @Test
    fun contactItem_rowHasCorrectSelectionSemantics() {
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        // Create item in a selected state
        createContactItem(contact, setOf(contact.id))

        composeTestRule
            .onNodeWithText(contact.displayName)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
    }

    @Test
    fun expandedEntry_reportsSelectionState() {
        val contact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        createContactItemWithEmptySelection(contact)

        // Expand
        composeTestRule.onNodeWithText(contact.displayName).performClick()

        // Verify the entry explicitly reports it is NOT selected
        composeTestRule
            .onNodeWithText(contact.emails.first().address)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
    }

    @Test
    fun contactItem_withMultiEntry_hasSelectAllCustomAction() {
        val contact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        composeTestRule.setContent {
            ContactItem(
                contact = contact,
                position = ItemPosition.ONLY,
                selectedEntries = emptySet(),
                isMultiSelectEnabled = true,
                isSearchMode = false,
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }

        val selectAllLabel = context.getString(R.string.a11y_select_all)

        composeTestRule
            .onNodeWithText(contact.displayName)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .assert(
                SemanticsMatcher("Has Select All Custom Action") { node ->
                    val customActions = node.config.getOrNull(SemanticsActions.CustomActions)
                    customActions?.any { it.label == selectAllLabel } == true
                }
            )
    }

    @Test
    fun contactItem_withSingleEntry_hasNoCustomAction() {
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        createContactItemWithEmptySelection(contact)

        // Custom action is withheld because Avatar action matches Row action
        composeTestRule
            .onNodeWithText(contact.displayName)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
    }

    @Test
    fun selectableAvatarClick_withDisplayNameContactInMultiSelectMode_callsOnToggleContactSelection() {
        assertAvatarClickBehavior(
            contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT,
            isMultiSelectEnabled = true,
            expectToggleContactCalled = true,
            expectItemExpanded = false,
        )
    }

    @Test
    fun selectableAvatarClick_withSingleEmailContactInMultiSelectMode_callsOnToggleContactSelection() {
        assertAvatarClickBehavior(
            contact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT,
            isMultiSelectEnabled = true,
            expectToggleContactCalled = true,
            expectItemExpanded = false,
        )
    }

    @Test
    fun selectableAvatar_withMultiEmailContactInMultiSelectMode_callsOnToggleContactSelection() {
        assertAvatarClickBehavior(
            contact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT,
            isMultiSelectEnabled = true,
            expectToggleContactCalled = true,
            expectItemExpanded = false,
        )
    }

    @Test
    fun selectableAvatarClick_withSinglePhoneContactInMultiSelectMode_callsOnToggleContactSelection() {
        assertAvatarClickBehavior(
            contact = ContactTestDataFactory.GENERIC_PHONE_CONTACT,
            isMultiSelectEnabled = true,
            expectToggleContactCalled = true,
            expectItemExpanded = false,
        )
    }

    @Test
    fun selectableAvatar_withMultiPhoneContactInMultiSelectMode_callsOnToggleContactSelection() {
        assertAvatarClickBehavior(
            contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT,
            isMultiSelectEnabled = true,
            expectToggleContactCalled = true,
            expectItemExpanded = false,
        )
    }

    @Test
    fun selectableAvatarClick_withDisplayNameContactInSingleSelectMode_callsOnToggleContactSelection() {
        assertAvatarClickBehavior(
            contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT,
            isMultiSelectEnabled = false,
            expectToggleContactCalled = true,
            expectItemExpanded = false,
        )
    }

    @Test
    fun selectableAvatarClick_withSingleEmailContactInSingleSelectMode_callsOnToggleContactSelection() {
        assertAvatarClickBehavior(
            contact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT,
            isMultiSelectEnabled = false,
            expectToggleContactCalled = true,
            expectItemExpanded = false,
        )
    }

    @Test
    fun selectableAvatar_withMultiEmailContactInSingleSelectMode_expandsItem() {
        assertAvatarClickBehavior(
            contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT,
            isMultiSelectEnabled = false,
            expectToggleContactCalled = false,
            expectItemExpanded = true,
        )
    }

    @Test
    fun selectableAvatarClick_withSinglePhoneContactInSingleSelectMode_callsOnToggleContactSelection() {
        assertAvatarClickBehavior(
            contact = ContactTestDataFactory.GENERIC_PHONE_CONTACT,
            isMultiSelectEnabled = false,
            expectToggleContactCalled = true,
            expectItemExpanded = false,
        )
    }

    @Test
    fun selectableAvatar_withMultiPhoneContactInSingleSelectMode_expandsItem() {
        assertAvatarClickBehavior(
            contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT,
            isMultiSelectEnabled = false,
            expectToggleContactCalled = false,
            expectItemExpanded = true,
        )
    }

    @Test
    fun selectableAvatarClick_whenSelectedEntryInSingleSelectMode_callsOnToggleContactSelection() {
        val contact = ContactTestDataFactory.GENERIC_DISPLAY_NAME_CONTACT
        assertAvatarClickBehavior(
            contact = contact,
            selectedEntries = setOf(contact.id),
            isMultiSelectEnabled = false,
            expectToggleContactCalled = true,
            expectItemExpanded = false,
        )
    }

    @Test
    fun selectableAvatar_whenSelectedContactInSingleSelectMode_callsOnToggleContactSelection() {
        val contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT
        assertAvatarClickBehavior(
            contact = contact,
            selectedEntries = setOf(contact.phones.first().id),
            isMultiSelectEnabled = false,
            expectToggleContactCalled = true,
            expectItemExpanded = false,
        )
    }

    @Test
    fun entryClick_withSingleEmailContact_callsOnToggleContactSelection() {
        assertEntryClickBehavior(
            contact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT,
            expectToggleContactCalled = true,
            expectItemExpanded = false,
        )
    }

    @Test
    fun entryClick_withSinglePhoneContact_callsOnToggleContactSelection() {
        assertEntryClickBehavior(
            contact = ContactTestDataFactory.GENERIC_PHONE_CONTACT,
            expectToggleContactCalled = true,
            expectItemExpanded = false,
        )
    }

    @Test
    fun entryClick_withMultiEmailContact_expandsAndDoesNotCallSelection() {
        assertEntryClickBehavior(
            contact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT,
            expectToggleContactCalled = false,
            expectItemExpanded = true,
        )
    }

    @Test
    fun entryClick_withMultiPhoneContact_expandsAndDoesNotCallSelection() {
        assertEntryClickBehavior(
            contact = ContactTestDataFactory.GENERIC_MULTI_PHONE_CONTACT,
            expectToggleContactCalled = false,
            expectItemExpanded = true,
        )
    }

    @Test
    fun entryClick_withMultiEmailContact_callsOnToggleEntrySelection() {
        val contact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        var onToggleEntryCalled = false
        var toggledEntryId: Long? = null

        composeTestRule.setContent {
            ContactItem(
                contact = contact,
                position = ItemPosition.ONLY,
                selectedEntries = emptySet(),
                isMultiSelectEnabled = true,
                isSearchMode = false,
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, entryId ->
                    onToggleEntryCalled = true
                    toggledEntryId = entryId
                },
            )
        }

        // Expand first by clicking the row
        composeTestRule.onNodeWithText(contact.displayName).performClick()

        // Click the first email entry
        val firstEmail = contact.emails.first()
        composeTestRule.onNodeWithText(firstEmail.address).performClick()

        assertThat(onToggleEntryCalled).isTrue()
        assertThat(toggledEntryId).isEqualTo(firstEmail.id)
    }

    /** Helper function to test the click behavior of the avatar in a [ContactItem]. */
    private fun assertAvatarClickBehavior(
        contact: Contact,
        selectedEntries: Set<Long> = emptySet(), // default no selection
        isMultiSelectEnabled: Boolean,
        expectToggleContactCalled: Boolean,
        expectItemExpanded: Boolean,
    ) {
        var onToggleContactCalled = false
        var onToggleEntryCalled = false
        composeTestRule.setContent {
            ContactItem(
                contact = contact,
                selectedEntries = selectedEntries,
                position = ItemPosition.ONLY,
                isMultiSelectEnabled = isMultiSelectEnabled,
                isSearchMode = false,
                onToggleContactSelection = { onToggleContactCalled = true },
                onToggleEntrySelection = { _, _ -> onToggleEntryCalled = true },
            )
        }

        val isExpandable =
            (contact is PhoneContact && contact.phones.size > 1) ||
                (contact is EmailContact && contact.emails.size > 1)
        val hasSeparateAvatarAction =
            isExpandable && (selectedEntries.isNotEmpty() || isMultiSelectEnabled)

        // If the avatar has a separate action, its Box is explicitly clickable.
        // Otherwise, it acts purely visually and touch falls through to the Row.
        if (hasSeparateAvatarAction) {
            val visualAvatarNode =
                if (selectedEntries.isEmpty()) {
                    composeTestRule.onNode(hasTestTag(AVATAR_TEST_TAG), useUnmergedTree = true)
                } else {
                    composeTestRule.onNodeWithContentDescription(
                        context.getString(
                            R.string.contact_item_selected_content_description,
                            contact.displayName,
                        ),
                        useUnmergedTree = true,
                    )
                }
            // Navigate to the clickable SelectableAvatar Box
            visualAvatarNode.onParent().performClick()
        } else {
            // When hasSeparateAvatarAction == false, the avatar isn't distinctly clickable,
            // so TalkBack / physical touch falls through to the Row itself.
            composeTestRule.onNodeWithText(contact.displayName).performClick()
        }

        assertThat(onToggleContactCalled).isEqualTo(expectToggleContactCalled)
        assertThat(onToggleEntryCalled).isFalse()

        if (expectItemExpanded) {
            when (contact) {
                is PhoneContact ->
                    contact.phones.forEach { phoneEntry ->
                        composeTestRule.onNodeWithText(phoneEntry.number).assertIsDisplayed()
                    }
                is EmailContact ->
                    contact.emails.forEach { emailEntry ->
                        composeTestRule.onNodeWithText(emailEntry.address).assertIsDisplayed()
                    }
                is DisplayNameContact ->
                    throw AssertionError("DisplayNameContact should not be expandable")
            }
        }
    }

    /**
     * Helper function to test the click behavior of the row body (clicking the text/row area) in a
     * [ContactItem].
     */
    private fun assertEntryClickBehavior(
        contact: Contact,
        expectToggleContactCalled: Boolean,
        expectItemExpanded: Boolean,
    ) {
        var onToggleContactCalled = false
        var onToggleEntryCalled = false
        composeTestRule.setContent {
            ContactItem(
                contact = contact,
                selectedEntries = emptySet(),
                position = ItemPosition.ONLY,
                isMultiSelectEnabled = true,
                isSearchMode = false,
                onToggleContactSelection = { onToggleContactCalled = true },
                onToggleEntrySelection = { _, _ -> onToggleEntryCalled = true },
            )
        }

        // Perform click on the display name text, which bubbles up to the row click handler
        composeTestRule.onNodeWithText(contact.displayName).performClick()

        assertThat(onToggleContactCalled).isEqualTo(expectToggleContactCalled)
        assertThat(onToggleEntryCalled).isFalse()

        verifyExpansion(contact, expectItemExpanded, true)
    }

    private fun verifyExpansion(
        contact: Contact,
        expectItemExpanded: Boolean,
        isMultiSelectEnabled: Boolean,
    ) {
        if (expectItemExpanded || isMultiSelectEnabled) {
            when (contact) {
                is PhoneContact ->
                    contact.phones.forEach { phoneEntry ->
                        composeTestRule.onNodeWithText(phoneEntry.number).assertIsDisplayed()
                    }
                is EmailContact ->
                    contact.emails.forEach { emailEntry ->
                        composeTestRule.onNodeWithText(emailEntry.address).assertIsDisplayed()
                    }
                is DisplayNameContact ->
                    throw AssertionError("DisplayNameContact should not be expandable")
            }
        } else {
            // If not expected to expand, verify entries are not shown (if they exist)
            if (contact is PhoneContact && contact.phones.isNotEmpty()) {
                composeTestRule.onNodeWithText(contact.phones.first().number).assertDoesNotExist()
            }
            if (contact is EmailContact && contact.emails.isNotEmpty()) {
                composeTestRule.onNodeWithText(contact.emails.first().address).assertDoesNotExist()
            }
        }
    }

    @Test
    fun calculateShape_middlePosition() {
        val shape = calculateShape(ItemPosition.MIDDLE, false)
        assertThat(shape).isEqualTo(MIDDLE_ITEM_SHAPE)
    }

    @Test
    fun calculateShape_middlePositionWithSelectedEntries() {
        val shape = calculateShape(ItemPosition.MIDDLE, true)
        assertThat(shape).isEqualTo(SINGLE_ITEM_SHAPE)
    }

    @Test
    fun calculateShape_firstPosition() {
        val shape = calculateShape(ItemPosition.FIRST, false)
        assertThat(shape).isEqualTo(TOP_ITEM_SHAPE)
    }

    @Test
    fun calculateShape_firstPositionWithSelectedEntries() {
        val shape = calculateShape(ItemPosition.FIRST, true)
        assertThat(shape).isEqualTo(SINGLE_ITEM_SHAPE)
    }

    @Test
    fun calculateShape_lastPosition() {
        val shape = calculateShape(ItemPosition.LAST, false)
        assertThat(shape).isEqualTo(BOTTOM_ITEM_SHAPE)
    }

    @Test
    fun calculateShape_lastPositionWithSelectedEntries() {
        val shape = calculateShape(ItemPosition.LAST, true)
        assertThat(shape).isEqualTo(SINGLE_ITEM_SHAPE)
    }

    @Test
    fun calculateShape_onlyPosition() {
        val shape = calculateShape(ItemPosition.ONLY, false)
        assertThat(shape).isEqualTo(SINGLE_ITEM_SHAPE)
    }

    @Test
    fun calculateShape_onlyPositionWithSelectedEntries() {
        val shape = calculateShape(ItemPosition.ONLY, true)
        assertThat(shape).isEqualTo(SINGLE_ITEM_SHAPE)
    }

    @Test
    fun searchMode_avatarClick_togglesEntrySelection() {
        val contact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        val entryId = contact.emails.first().id
        var toggledContactId: Long? = null
        var toggledEntryId: Long? = null
        var toggleContactInvoked = false

        composeTestRule.setContent {
            ContactItem(
                contact = contact,
                position = ItemPosition.ONLY,
                selectedEntries = emptySet(),
                isMultiSelectEnabled = false,
                onToggleContactSelection = { toggleContactInvoked = true },
                onToggleEntrySelection = { cId, eId ->
                    toggledContactId = cId
                    toggledEntryId = eId
                },
                isSearchMode = true,
            )
        }

        // In searchMode, the avatar is not itself clickable, click on the row
        composeTestRule.onNodeWithText(contact.displayName).performClick()

        assertThat(toggledContactId).isEqualTo(contact.id)
        assertThat(toggledEntryId).isEqualTo(entryId)
        assertThat(toggleContactInvoked).isFalse()
    }

    @Test
    fun searchMode_itemSelected_avatarShowsCheckmark() {
        val contact = ContactTestDataFactory.GENERIC_EMAIL_CONTACT
        val entryId = contact.emails.first().id

        composeTestRule.setContent {
            ContactItem(
                contact = contact,
                position = ItemPosition.ONLY,
                selectedEntries = setOf(entryId), // Entry is selected
                isMultiSelectEnabled = false,
                isSearchMode = true,
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(
                    R.string.contact_item_selected_content_description,
                    contact.displayName,
                ),
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
    }

    @Test
    fun searchMode_expandIconHidden() {
        val contact = ContactTestDataFactory.GENERIC_MULTI_EMAIL_CONTACT
        composeTestRule.setContent {
            ContactItem(
                contact = contact,
                position = ItemPosition.ONLY,
                selectedEntries = emptySet(),
                isMultiSelectEnabled = false,
                isSearchMode = true,
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }

        assertRowClickLabel(contact.displayName, null)
    }

    private fun createContactItemWithEmptySelection(contact: Contact) {
        createContactItem(contact, emptySet())
    }

    private fun createContactItem(contact: Contact, selectedEntries: Set<Long>) {
        composeTestRule.setContent {
            ContactItem(
                contact = contact,
                position = ItemPosition.ONLY,
                selectedEntries = selectedEntries,
                isMultiSelectEnabled = true,
                isSearchMode = false,
                onToggleContactSelection = {},
                onToggleEntrySelection = { _, _ -> },
            )
        }
    }

    /** Helper function to assert that the TalkBack click label on the Row is set correctly. */
    private fun assertRowClickLabel(displayName: String, expectedLabel: String?) {
        composeTestRule
            .onNodeWithText(displayName)
            .assert(
                SemanticsMatcher("Click label matches '$expectedLabel'") { node ->
                    val action = node.config.getOrNull(SemanticsActions.OnClick)
                    action?.label == expectedLabel
                }
            )
    }

    /**
     * Helper function to create the selected count message displayed as the secondary text on
     * multi-entry contacts when at least one entry is selected. Ex: "1 of 2 emails selected"
     */
    private fun selectedCountMessage(resourceId: Int, totalCount: Int, selectedCount: Int): String {
        val msgFormat = MessageFormat(context.getString(resourceId), Locale.getDefault())
        val args = mapOf(Pair("total_count", totalCount), Pair("selected_count", selectedCount))

        return msgFormat.format(args)
    }

    /**
     * Helper function to create the total count message displayed as the secondary text on
     * multi-entry contacts when no entries are selected. Ex: "2 emails"
     */
    private fun totalCountMessage(resourceId: Int, totalCount: Int): String {
        val msgFormat = MessageFormat(context.getString(resourceId), Locale.getDefault())
        val args = mapOf(Pair("count", totalCount))
        return msgFormat.format(args)
    }
}
