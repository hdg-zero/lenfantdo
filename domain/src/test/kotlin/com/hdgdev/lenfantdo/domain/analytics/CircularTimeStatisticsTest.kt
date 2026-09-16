/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class CircularTimeStatisticsTest {

    @Test
    fun `empty list returns null mean`() {
        val summary = CircularTimeStatistics.compute(emptyList())
        assertNull(summary.meanTime)
        assertNull(summary.meanSecondsFromMidnight)
        assertEquals(0, summary.sampleSize)
        assertEquals(0.0, summary.resultantVectorLength, 0.001)
        assertEquals(1.0, summary.circularVariance, 0.001)
    }

    @Test
    fun `identical times return exact time and zero variance`() {
        val times = listOf(
            LocalTime.of(23, 30),
            LocalTime.of(23, 30),
            LocalTime.of(23, 30)
        )
        val summary = CircularTimeStatistics.compute(times)
        val mean = summary.meanTime
        assertNotNull(mean)
        assertEquals(23, mean!!.hour)
        assertEquals(30, mean.minute)
        assertEquals(1.0, summary.resultantVectorLength, 0.001)
        assertEquals(0.0, summary.circularVariance, 0.001)
    }

    @Test
    fun `midnight trap test - 23h50 and 00h10 averages to 00h00, not 12h00 noon`() {
        // Bedtime 1: 23:50 (-10 min from midnight)
        // Bedtime 2: 00:10 (+10 min from midnight)
        // Arithmetic mean in seconds: (23*3600+50*60 + 10*60)/2 = 43200s = 12:00 (NOON)!
        // Circular mean must be: 00:00:00 (MIDNIGHT)!
        val times = listOf(
            LocalTime.of(23, 50),
            LocalTime.of(0, 10)
        )
        val summary = CircularTimeStatistics.compute(times)
        val mean = summary.meanTime
        assertNotNull(mean)
        assertEquals(0, mean!!.hour)
        assertEquals(0, mean.minute)
        assertEquals(0, mean.second)
        // High concentration (only 10 min off)
        assertTrue(summary.resultantVectorLength > 0.99)
    }

    @Test
    fun `symmetric times around 07h00 wake time`() {
        val times = listOf(
            LocalTime.of(6, 45),
            LocalTime.of(7, 0),
            LocalTime.of(7, 15)
        )
        val summary = CircularTimeStatistics.compute(times)
        val mean = summary.meanTime
        assertNotNull(mean)
        assertEquals(7, mean!!.hour)
        assertEquals(0, mean.minute)
        assertTrue(summary.resultantVectorLength > 0.99)
    }

    @Test
    fun `diametrically opposed times return undefined mean`() {
        // 06:00 and 18:00 are exactly opposite on the 24h circle
        val times = listOf(
            LocalTime.of(6, 0),
            LocalTime.of(18, 0)
        )
        val summary = CircularTimeStatistics.compute(times)
        assertNull(summary.meanTime)
        assertEquals(0.0, summary.resultantVectorLength, 0.001)
        assertEquals(1.0, summary.circularVariance, 0.001)
    }
}
