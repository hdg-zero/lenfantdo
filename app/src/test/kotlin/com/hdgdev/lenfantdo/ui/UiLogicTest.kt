/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.ui

import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.ui.component.PeriodOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class UiLogicTest {

    @Test
    fun periodOption_daysValuesAreCorrect() {
        assertEquals(7L, PeriodOption.LAST_7_DAYS.days)
        assertEquals(14L, PeriodOption.LAST_14_DAYS.days)
        assertEquals(30L, PeriodOption.LAST_30_DAYS.days)
        assertEquals(365L, PeriodOption.LAST_YEAR.days)
        assertEquals(null, PeriodOption.ALL_TIME.days)
    }

    @Test
    fun journalFiltering_filtersByNoteCorrectly() {
        val session1 = SleepSession(id = 1, startEpochMs = 1000, stopEpochMs = 2000, note = "Sieste calme")
        val session2 = SleepSession(id = 2, startEpochMs = 3000, stopEpochMs = 4000, note = "Nuit agitée")
        val session3 = SleepSession(id = 3, startEpochMs = 5000, stopEpochMs = 6000, note = "")

        val list = listOf(session1, session2, session3)

        val query = "sieste"
        val filtered = list.filter { it.note.lowercase().contains(query.lowercase()) }

        assertEquals(1, filtered.size)
        assertEquals(1L, filtered.first().id)
    }

    @Test
    fun journalGrouping_groupsByMonthAndYear() {
        val zoneId = ZoneId.of("UTC")
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)

        // 2026-03-15T00:00:00Z -> 1773532800000L
        val sessionMarch = SleepSession(id = 1, startEpochMs = 1773504000000L, stopEpochMs = 1773532800000L)
        // 2026-04-10T00:00:00Z -> 1775779200000L
        val sessionApril = SleepSession(id = 2, startEpochMs = 1775750400000L, stopEpochMs = 1775779200000L)

        val sessions = listOf(sessionApril, sessionMarch)

        val grouped = sessions.groupBy { session ->
            val zdt = Instant.ofEpochMilli(session.stopEpochMs).atZone(zoneId)
            zdt.format(monthFormatter).replaceFirstChar { it.uppercase() }
        }

        assertTrue(grouped.containsKey("Avril 2026"))
        assertTrue(grouped.containsKey("Mars 2026"))
        assertEquals(1, grouped["Avril 2026"]?.size)
        assertEquals(1, grouped["Mars 2026"]?.size)
    }
}
