/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contactspicker.inject

import com.android.contactspicker.data.repository.ContactsPickerSessionProviderRepository
import com.android.contactspicker.data.repository.ContactsPickerSessionProviderRepositoryImpl
import com.android.contactspicker.data.repository.ContactsRepository
import com.android.contactspicker.data.repository.ContactsRepositoryImpl
import com.android.contactspicker.data.repository.PrivacyBannerRepository
import com.android.contactspicker.data.repository.PrivacyBannerRepositoryImpl
import com.android.contactspicker.data.repository.UserRepository
import com.android.contactspicker.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindContactsRepository(
        contactsRepositoryImpl: ContactsRepositoryImpl
    ): ContactsRepository

    @Binds
    abstract fun bindContactsPickerSessionProviderRepository(
        contactsPickerSessionProviderRepositoryImpl: ContactsPickerSessionProviderRepositoryImpl
    ): ContactsPickerSessionProviderRepository

    @Binds
    abstract fun bindPrivacyRepository(
        privacyRepositoryImpl: PrivacyBannerRepositoryImpl
    ): PrivacyBannerRepository

    @Binds abstract fun bindUserRepository(userRepositoryImpl: UserRepositoryImpl): UserRepository
}
