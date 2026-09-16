/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.analytics

import com.hdgdev.lenfantdo.domain.model.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SleepDateResolverAndCoverageTest {

    private val zoneUtc = ZoneId.of("UTC")

    @Test
    fun `resolve session crossing midnight maps to wake date by default`() {
        // Bedtime: 2026-03-10 23:30 UTC
        // Wake time: 2026-03-11 07:15 UTC
        val startEpoch = ZonedDateTime.of(2026, 3, 10, 23, 30, 0, 0, zoneUtc).toInstant().toEpochMilli()
        val stopEpoch = ZonedDateTime.of(2026, 3, 11, 7, 15, 0, 0, zoneUtc).toInstant().toEpochMilli()

        val session = SleepSession(
            id = 1L,
            startEpochMs = startEpoch,
            stopEpochMs = stopEpoch,
            rating = 4L,
            note = "Good sleep",
            wakeups = 1
        )

        val resolved = SleepDateResolver.resolve(session, zoneUtc, DateResolutionStrategy.WAKE_DATE)
        assertEquals(LocalDate.of(2026, 3, 11), resolved.logicalDate)
        assertEquals(23, resolved.startTime.hour)
        assertEquals(30, resolved.startTime.minute)
        assertEquals(7, resolved.stopTime.hour)
        assertEquals(15, resolved.stopTime.minute)
        assertEquals((7 * 3600 + 45 * 60).toLong(), resolved.durationSeconds)

        val resolvedStart = SleepDateResolver.resolve(session, zoneUtc, DateResolutionStrategy.START_DATE)
        assertEquals(LocalDate.of(2026, 3, 10), resolvedStart.logicalDate)
    }

    @Test
    fun `coverage calculator calculates exact ratio and missing days`() {
        val start = LocalDate.of(2026, 3, 1)
        val end = LocalDate.of(2026, 3, 7) // 7 days total

        val tracked = setOf(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 2),
            LocalDate.of(2026, 3, 4),
            LocalDate.of(2026, 3, 7)
        ) // 4 days tracked, 3 missing

        val coverage = CoverageCalculator.calculateCoverage(start, end, tracked)
        assertEquals(7L, coverage.totalCalendarDays)
        assertEquals(4L, coverage.trackedDaysCount)
        assertEquals(3L, coverage.missingDaysCount)
        assertEquals(4.0 / 7.0, coverage.coverageRatio, 0.0001)
    }

    @Test
    fun `compute period analytics calculates mean duration over recorded sessions without corrupting with missing days`() {
        val start = LocalDate.of(2026, 3, 1)
        val end = LocalDate.of(2026, 3, 7)

        // Two 8-hour sessions (28800000 ms) on March 1 and March 2
        val s1Start = ZonedDateTime.of(2026, 3, 1, 0, 0, 0, 0, zoneUtc).toInstant().toEpochMilli()
        val s1Stop = ZonedDateTime.of(2026, 3, 1, 8, 0, 0, 0, zoneUtc).toInstant().toEpochMilli()

        val s2Start = ZonedDateTime.of(2026, 3, 2, 0, 0, 0, 0, zoneUtc).toInstant().toEpochMilli()
        val s2Stop = ZonedDateTime.of(2026, 3, 2, 8, 0, 0, 0, zoneUtc).toInstant().toEpochMilli()

        val sessions = listOf(
            SleepSession(id = 1, startEpochMs = s1Start, stopEpochMs = s1Stop, rating = 5L, wakeups = 0),
            SleepSession(id = 2, startEpochMs = s2Start, stopEpochMs = s2Stop, rating = 3L, wakeups = 2)
        )

        val analytics = CoverageCalculator.computePeriodAnalytics(sessions, start, end, zoneUtc)
        assertEquals(2, analytics.totalSessionsCount)
        assertEquals(2L, analytics.coverage.trackedDaysCount)
        assertEquals(5L, analytics.coverage.missingDaysCount)
        assertEquals(2.0 / 7.0, analytics.coverage.coverageRatio, 0.0001)

        // Average duration MUST be 8.0 hours (28800000 ms), NOT divided by 7 days!
        assertEquals(28800000.0, analytics.durationSummaryMs.mean, 0.001)
        assertEquals(4.0, analytics.averageRating!!, 0.001)
        assertEquals(1.0, analytics.averageWakeups, 0.001)
        val bedtimeMean = analytics.bedtimeCircular.meanTime
        assertNotNull(bedtimeMean)
        assertEquals(0, bedtimeMean!!.hour)
        assertEquals(0, bedtimeMean.minute)
        val wakeTimeMean = analytics.wakeTimeCircular.meanTime
        assertNotNull(wakeTimeMean)
        assertEquals(8, wakeTimeMean!!.hour)
        assertEquals(0, wakeTimeMean.minute)
    }
}
