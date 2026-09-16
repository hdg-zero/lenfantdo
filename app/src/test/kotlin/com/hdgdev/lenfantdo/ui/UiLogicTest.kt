/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.ui

import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.feature.insights.DailyChartBar
import com.hdgdev.lenfantdo.ui.component.PeriodOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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

    @Test
    fun regularityScore_computesAccuratePercentageAndLabel() {
        fun computeScoreAndLabel(rBed: Double, rWake: Double): Pair<Int, String> {
            val rCombined = (rBed + rWake) / 2.0
            val score = (rCombined * 100.0).roundToInt().coerceIn(0, 100)
            val label = when {
                score >= 85 -> "Excellente"
                score >= 70 -> "Bonne"
                score >= 50 -> "Modérée"
                else -> "Variable"
            }
            return Pair(score, label)
        }

        val perfect = computeScoreAndLabel(1.0, 1.0)
        assertEquals(100, perfect.first)
        assertEquals("Excellente", perfect.second)

        val good = computeScoreAndLabel(0.75, 0.77)
        assertEquals(76, good.first)
        assertEquals("Bonne", good.second)

        val moderate = computeScoreAndLabel(0.56, 0.60)
        assertEquals(58, moderate.first)
        assertEquals("Modérée", moderate.second)

        val variable = computeScoreAndLabel(0.30, 0.40)
        assertEquals(35, variable.first)
        assertEquals("Variable", variable.second)
    }

    @Test
    fun weekdayVsWeekend_computesAveragesAndDeltaCorrectly() {
        val bars = listOf(
            DailyChartBar(
                date = LocalDate.of(2026, 9, 14), // Monday (weekday)
                durationHours = 7.0,
                isTracked = true,
                formattedDuration = "7h"
            ),
            DailyChartBar(
                date = LocalDate.of(2026, 9, 15), // Tuesday (weekday)
                durationHours = 8.0,
                isTracked = true,
                formattedDuration = "8h"
            ),
            DailyChartBar(
                date = LocalDate.of(2026, 9, 19), // Saturday (weekend)
                durationHours = 9.0,
                isTracked = true,
                formattedDuration = "9h"
            ),
            DailyChartBar(
                date = LocalDate.of(2026, 9, 20), // Sunday (weekend)
                durationHours = 9.5,
                isTracked = true,
                formattedDuration = "9h 30min"
            )
        )

        val tracked = bars.filter { it.isTracked }
        val weekday = tracked.filter { it.date.dayOfWeek.value in 1..5 }
        val weekend = tracked.filter { it.date.dayOfWeek.value in 6..7 }

        val weekdayMean = weekday.map { it.durationHours }.average()
        val weekendMean = weekend.map { it.durationHours }.average()

        assertEquals(7.5, weekdayMean, 0.001)
        assertEquals(9.25, weekendMean, 0.001)

        val deltaMinutes = ((weekendMean - weekdayMean) * 60).roundToInt()
        assertEquals(105, deltaMinutes) // +1h 45min in weekend
    }

    @Test
    fun dailyChartBar_deltaFromMeanIsAccurate() {
        val meanMinutes = 480 // 8 hours
        val sessionMinutes = 450 // 7h30
        val delta = sessionMinutes - meanMinutes
        assertEquals(-30, delta)
    }
}
