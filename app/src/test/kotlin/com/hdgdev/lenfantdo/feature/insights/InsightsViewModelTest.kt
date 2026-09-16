/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.insights

import android.app.Application
import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.test.FakeSleepRepository
import com.hdgdev.lenfantdo.ui.component.PeriodOption
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val fakeRepo = FakeSleepRepository()
    private lateinit var viewModel: InsightsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val app = Application()
        viewModel = InsightsViewModel(app, fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun last7Days_generates7DailyBars() = testScope.runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // Insert a recent session
        val now = System.currentTimeMillis()
        fakeRepo.insertSession(
            SleepSession(id = 1, startEpochMs = now - 8 * 3600 * 1000, stopEpochMs = now)
        )
        advanceUntilIdle()

        viewModel.selectPeriod(PeriodOption.LAST_7_DAYS)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(7, state.chartBars.size)
        // Should not be monthly
        assertEquals(null, state.chartBars.first().customLabel)
        collectJob.cancel()
    }

    @Test
    fun lastYear_generates12MonthlyAggregatedBars() = testScope.runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val now = System.currentTimeMillis()
        fakeRepo.insertSession(
            SleepSession(id = 1, startEpochMs = now - 8 * 3600 * 1000, stopEpochMs = now)
        )
        advanceUntilIdle()

        viewModel.selectPeriod(PeriodOption.LAST_YEAR)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Must generate 12 monthly bars instead of 365 crushed bars
        assertEquals(12, state.chartBars.size)
        // Must have customLabel with month name
        assertNotNull(state.chartBars.first().customLabel)
        collectJob.cancel()
    }

    @Test
    fun allTime_withLongHistory_aggregatesMonthlyWithoutEarlyTruncation() = testScope.runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val now = Instant.now()
        // Session 120 days ago (~4 months)
        val session1 = SleepSession(
            id = 1,
            startEpochMs = now.minus(120, ChronoUnit.DAYS).toEpochMilli(),
            stopEpochMs = now.minus(120, ChronoUnit.DAYS).plus(8, ChronoUnit.HOURS).toEpochMilli()
        )
        // Session today
        val session2 = SleepSession(
            id = 2,
            startEpochMs = now.minus(8, ChronoUnit.HOURS).toEpochMilli(),
            stopEpochMs = now.toEpochMilli()
        )
        fakeRepo.insertSessions(listOf(session1, session2))
        advanceUntilIdle()

        viewModel.selectPeriod(PeriodOption.ALL_TIME)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Spans ~5 months
        assertTrue("Monthly bars should be between 4 and 6", state.chartBars.size in 4..6)
        // Must have customLabel
        assertNotNull(state.chartBars.first().customLabel)
        // Coverage analytics must include all sessions
        assertEquals(2, state.analytics?.totalSessionsCount)
        collectJob.cancel()
    }
}
