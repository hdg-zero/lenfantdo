/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SleepSessionTest {

    @Test
    fun validSessionCalculatesDurationCorrectly() {
        val session = SleepSession(
            id = 1L,
            startEpochMs = 1000L,
            stopEpochMs = 3600000L + 1000L,
            rating = 4L,
            note = "Bonne nuit",
            wakeups = 1
        )
        assertEquals(3600000L, session.durationMs)
        assertEquals(4L, session.rating)
        assertEquals(1, session.wakeups)
    }

    @Test
    fun sessionWithNegativeStartThrowsException() {
        assertThrows(IllegalArgumentException::class.java) {
            SleepSession(
                startEpochMs = -1L,
                stopEpochMs = 1000L
            )
        }
    }

    @Test
    fun sessionWithStopBeforeStartThrowsException() {
        assertThrows(IllegalArgumentException::class.java) {
            SleepSession(
                startEpochMs = 2000L,
                stopEpochMs = 1000L
            )
        }
    }

    @Test
    fun sessionWithNegativeWakeupsThrowsException() {
        assertThrows(IllegalArgumentException::class.java) {
            SleepSession(
                startEpochMs = 1000L,
                stopEpochMs = 2000L,
                wakeups = -1
            )
        }
    }

    @Test
    fun sessionWithInvalidRatingThrowsException() {
        assertThrows(IllegalArgumentException::class.java) {
            SleepSession(
                startEpochMs = 1000L,
                stopEpochMs = 2000L,
                rating = 6L
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            SleepSession(
                startEpochMs = 1000L,
                stopEpochMs = 2000L,
                rating = 0L
            )
        }
    }
}
