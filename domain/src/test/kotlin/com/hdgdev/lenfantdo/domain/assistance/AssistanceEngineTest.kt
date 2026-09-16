/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.assistance

import com.hdgdev.lenfantdo.domain.model.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistanceEngineTest {

    @Test
    fun `active tracking under 14 hours generates no warning`() {
        val start = 1_000_000_000L
        val current = start + (8 * 3600 * 1000L) // 8 hours later
        assertNull(AssistanceEngine.inspectActiveTracking(start, current))
    }

    @Test
    fun `active tracking over 14 hours generates excessive warning`() {
        val start = 1_000_000_000L
        val current = start + (15 * 3600 * 1000L) // 15 hours later
        val warning = AssistanceEngine.inspectActiveTracking(start, current)
        assertNotNull(warning)
        assertEquals(AssistanceWarningType.ACTIVE_TRACKING_EXCESSIVE, warning!!.type)
        assertTrue(warning.message.contains("15 heures"))
    }

    @Test
    fun `session under 30 minutes generates too short warning`() {
        val start = 1_000_000_000L
        val stop = start + (15 * 60 * 1000L) // 15 minutes
        val session = SleepSession(id = 1, startEpochMs = start, stopEpochMs = stop)

        val warnings = AssistanceEngine.inspectSession(session)
        assertEquals(1, warnings.size)
        assertEquals(AssistanceWarningType.SESSION_TOO_SHORT, warnings.first().type)
    }

    @Test
    fun `session over 18 hours generates too long warning`() {
        val start = 1_000_000_000L
        val stop = start + (20 * 3600 * 1000L) // 20 hours
        val session = SleepSession(id = 1, startEpochMs = start, stopEpochMs = stop)

        val warnings = AssistanceEngine.inspectSession(session)
        assertEquals(1, warnings.size)
        assertEquals(AssistanceWarningType.SESSION_TOO_LONG, warnings.first().type)
    }

    @Test
    fun `session overlapping existing session generates overlap warning`() {
        val base = 1_000_000_000L
        val existing = listOf(
            SleepSession(id = 10, startEpochMs = base, stopEpochMs = base + (8 * 3600 * 1000L))
        )
        // New session starts 2 hours before existing ends
        val overlapping = SleepSession(
            id = 11,
            startEpochMs = base + (6 * 3600 * 1000L),
            stopEpochMs = base + (12 * 3600 * 1000L)
        )

        val warnings = AssistanceEngine.inspectSession(overlapping, existing)
        assertTrue(warnings.any { it.type == AssistanceWarningType.SESSION_OVERLAPS })
    }
}
