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
package com.android.contactspicker.inject

import android.app.Application
import android.app.ApplicationPackageManager
import android.content.pm.PackageManager
import androidx.room.Room
import com.android.contactspicker.room.dao.PrivacyBannerShownDao
import com.android.contactspicker.room.database.PrivacyBannerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides dependencies that are scoped to the application's lifecycle. These
 * dependencies are available as singletons throughout the app.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides a singleton instance of the application's [PackageManager].
     *
     * @param app The application instance, automatically provided by Hilt.
     * @return [ApplicationPackageManager] impl of application's [PackageManager].
     */
    @Provides
    @Singleton
    fun providePackageManager(app: Application): ApplicationPackageManager {
        return app.packageManager as ApplicationPackageManager
    }

    /**
     * Provides a singleton instance of the Room [PrivacyBannerDatabase].
     *
     * This function builds the Room database, which is the primary entry point for accessing the
     * application's structured, persisted data.
     *
     * @param app The application context, provided by Hilt, used to build the database.
     * @return The singleton [PrivacyBannerDatabase] instance.
     */
    @Provides
    @Singleton
    fun providePrivacyBannerDatabase(app: Application): PrivacyBannerDatabase {
        return Room.databaseBuilder(app, PrivacyBannerDatabase::class.java, "privacy_banner.db")
            .build()
    }

    /**
     * Provides a singleton instance of the [PrivacyBannerShownDao].
     *
     * The DAO is retrieved from the provided [PrivacyBannerDatabase] instance and is used to
     * interact with the `privacy_banner_shown` table.
     *
     * @param db The singleton [PrivacyBannerDatabase] instance, provided by Hilt.
     * @return The singleton [PrivacyBannerShownDao] instance.
     */
    @Provides
    @Singleton
    fun providePrivacyBannerDao(db: PrivacyBannerDatabase): PrivacyBannerShownDao {
        return db.privacyBannerShownDao()
    }
}
