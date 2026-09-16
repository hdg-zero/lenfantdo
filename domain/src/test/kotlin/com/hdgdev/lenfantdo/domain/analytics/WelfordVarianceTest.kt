/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.sqrt

class WelfordVarianceTest {

    @Test
    fun `empty list returns empty summary`() {
        val summary = emptyList<Double>().welfordStats()
        assertEquals(0L, summary.count)
        assertEquals(0.0, summary.mean, 0.001)
        assertEquals(0.0, summary.sampleVariance, 0.001)
        assertEquals(0.0, summary.sampleStandardDeviation, 0.001)
        assertNull(summary.min)
        assertNull(summary.max)
    }

    @Test
    fun `single value returns mean and zero sample variance`() {
        val summary = listOf(42.0).welfordStats()
        assertEquals(1L, summary.count)
        assertEquals(42.0, summary.mean, 0.001)
        assertEquals(0.0, summary.sampleVariance, 0.001)
        assertEquals(42.0, summary.min!!, 0.001)
        assertEquals(42.0, summary.max!!, 0.001)
    }

    @Test
    fun `known dataset matches exact mathematical values`() {
        // Dataset: 2, 4, 4, 4, 5, 5, 7, 9
        // Sum = 40, N = 8, Mean = 5
        // Differences from mean: -3, -1, -1, -1, 0, 0, 2, 4
        // Squared differences: 9 + 1 + 1 + 1 + 0 + 0 + 4 + 16 = 32
        // Population variance = 32 / 8 = 4.0, Pop StdDev = 2.0
        // Sample variance = 32 / 7 = 4.571428..., Sample StdDev = sqrt(32/7) = 2.1380899...
        val data = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
        val summary = data.welfordStats()

        assertEquals(8L, summary.count)
        assertEquals(5.0, summary.mean, 0.0001)
        assertEquals(32.0 / 7.0, summary.sampleVariance, 0.0001)
        assertEquals(sqrt(32.0 / 7.0), summary.sampleStandardDeviation, 0.0001)
        assertEquals(2.0, summary.min!!, 0.0001)
        assertEquals(9.0, summary.max!!, 0.0001)
    }

    @Test
    fun `numerical stability with huge base offset`() {
        // Test with values that would cause catastrophic cancellation in naive formula (E[x^2] - E[x]^2)
        val base = 1_000_000_000.0
        val data = listOf(base + 1.0, base + 2.0, base + 3.0)
        val summary = data.welfordStats()

        assertEquals(3L, summary.count)
        assertEquals(base + 2.0, summary.mean, 0.0001)
        assertEquals(1.0, summary.sampleVariance, 0.0001)
        assertEquals(1.0, summary.sampleStandardDeviation, 0.0001)
    }
}
