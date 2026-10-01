/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature

import android.app.Application
import com.hdgdev.lenfantdo.feature.detail.SessionDetailViewModel
import com.hdgdev.lenfantdo.feature.home.HomeViewModel
import com.hdgdev.lenfantdo.feature.insights.InsightsViewModel
import com.hdgdev.lenfantdo.feature.journal.JournalViewModel
import com.hdgdev.lenfantdo.feature.review.SessionReviewViewModel
import com.hdgdev.lenfantdo.feature.settings.SettingsViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test

class ViewModelConstructorTest {

    @Test
    fun allViewModels_haveApplicationConstructorForFactoryReflection() {
        val viewModelClasses = listOf(
            HomeViewModel::class.java,
            JournalViewModel::class.java,
            InsightsViewModel::class.java,
            SettingsViewModel::class.java,
            SessionReviewViewModel::class.java,
            SessionDetailViewModel::class.java
        )

        for (vmClass in viewModelClasses) {
            val constructor = try {
                vmClass.getConstructor(Application::class.java)
            } catch (e: NoSuchMethodException) {
                null
            }
            assertNotNull(
                "ViewModel ${vmClass.name} must declare a public constructor(Application) for AndroidViewModelFactory reflection",
                constructor
            )
        }
    }
}
