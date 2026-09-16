/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.settings

import android.app.Application
import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.test.FakeSettingsRepository
import com.hdgdev.lenfantdo.test.FakeSleepRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val fakeRepo = FakeSleepRepository()
    private val fakeSettingsRepo = FakeSettingsRepository()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val app = Application()
        viewModel = SettingsViewModel(app, fakeRepo, fakeSettingsRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateTheme_modifiesPreferencesInState() = testScope.runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("auto", viewModel.uiState.value.preferences.appTheme)

        viewModel.updateTheme("dark")
        advanceUntilIdle()

        assertEquals("dark", viewModel.uiState.value.preferences.appTheme)

        viewModel.updateTheme("light")
        advanceUntilIdle()

        assertEquals("light", viewModel.uiState.value.preferences.appTheme)
        collectJob.cancel()
    }

    @Test
    fun updateCompactView_togglesPreferenceInState() = testScope.runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.preferences.compactView)

        viewModel.updateCompactView(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.preferences.compactView)

        viewModel.updateCompactView(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.preferences.compactView)
        collectJob.cancel()
    }

    @Test
    fun totalSessionsCount_updatesWhenSessionsInserted() = testScope.runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.totalSessionsCount)

        fakeRepo.insertSession(SleepSession(id = 1, startEpochMs = 1000, stopEpochMs = 2000))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.totalSessionsCount)
        collectJob.cancel()
    }
}
