/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.test.FakeSleepRepository
import com.hdgdev.lenfantdo.tracking.TrackingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val fakeRepo = FakeSleepRepository()
    private lateinit var trackingManager: TrackingManager
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        trackingManager = TrackingManager.createForTesting(fakeRepo)
        val app = Application()
        viewModel = HomeViewModel(app, trackingManager, fakeRepo)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiState_whenNoActiveTracking_hasNullTracking() = testScope.runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.activeTracking)
        assertEquals(0L, state.elapsedSeconds)
        assertEquals(0, state.totalRecordedNights)
        collectJob.cancel()
    }

    @Test
    fun startTracking_activatesTrackingAndUpdatesElapsed() = testScope.runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        runCurrent()

        val fiveSecondsAgo = System.currentTimeMillis() - 5000L
        fakeRepo.startTracking(fiveSecondsAgo)
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.activeTracking)
        assertTrue("Elapsed should be at least 4s, was ${state.elapsedSeconds}", state.elapsedSeconds >= 4L)
        collectJob.cancel()
    }

    @Test
    fun cancelTracking_resetsActiveTracking() = testScope.runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.startTracking()
        runCurrent()
        assertNotNull(viewModel.uiState.value.activeTracking)

        viewModel.cancelTracking()
        runCurrent()

        assertNull(viewModel.uiState.value.activeTracking)
        collectJob.cancel()
    }

    @Test
    fun stopTracking_recordsSessionAndCallsCallback() = testScope.runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.startTracking()
        runCurrent()

        var stoppedSession: SleepSession? = null
        viewModel.stopTracking { session ->
            stoppedSession = session
        }
        runCurrent()

        assertNotNull(stoppedSession)
        assertNull(viewModel.uiState.value.activeTracking)
        assertEquals(1, viewModel.uiState.value.totalRecordedNights)
        collectJob.cancel()
    }
}
