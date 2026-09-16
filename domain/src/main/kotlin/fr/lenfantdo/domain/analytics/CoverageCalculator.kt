/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.domain.analytics

import fr.lenfantdo.domain.model.SleepSession
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Summary of tracking coverage and consistency over a calendar period.
 */
data class CoverageSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalCalendarDays: Long,
    val trackedDaysCount: Long,
    val missingDaysCount: Long,
    val coverageRatio: Double // 0.0 to 1.0
)

/**
 * Comprehensive sleep metrics aggregated for a defined calendar period.
 */
data class PeriodSleepAnalytics(
    val coverage: CoverageSummary,
    val totalSessionsCount: Int,
    val durationSummaryMs: WelfordSummary,
    val bedtimeCircular: CircularTimeSummary,
    val wakeTimeCircular: CircularTimeSummary,
    val averageRating: Double?,
    val averageWakeups: Double
)

object CoverageCalculator {

    /**
     * Calculates coverage statistics between [startDate] and [endDate] (inclusive).
     */
    fun calculateCoverage(
        startDate: LocalDate,
        endDate: LocalDate,
        trackedDates: Set<LocalDate>
    ): CoverageSummary {
        require(!endDate.isBefore(startDate)) { "endDate ($endDate) cannot be before startDate ($startDate)" }
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1L
        val trackedInWindow = trackedDates.count { it in startDate..endDate }.toLong()
        val missingDays = (totalDays - trackedInWindow).coerceAtLeast(0L)
        val ratio = if (totalDays > 0L) trackedInWindow.toDouble() / totalDays else 0.0

        return CoverageSummary(
            startDate = startDate,
            endDate = endDate,
            totalCalendarDays = totalDays,
            trackedDaysCount = trackedInWindow,
            missingDaysCount = missingDays,
            coverageRatio = ratio
        )
    }

    /**
     * Aggregates full sleep analytics for a list of [sessions] within a calendar range [startDate]..[endDate].
     * Missing days are reported in coverage but NOT treated as 0-duration sleeps in duration averages.
     */
    fun computePeriodAnalytics(
        sessions: List<SleepSession>,
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
        strategy: DateResolutionStrategy = DateResolutionStrategy.WAKE_DATE
    ): PeriodSleepAnalytics {
        val resolvedSessions = sessions
            .map { SleepDateResolver.resolve(it, zoneId, strategy) }
            .filter { it.logicalDate in startDate..endDate }

        val trackedDates = resolvedSessions.map { it.logicalDate }.toSet()
        val coverage = calculateCoverage(startDate, endDate, trackedDates)

        val durationSummary = resolvedSessions.map { it.session.durationMs }.welfordStatsOfLong()

        val bedtimes = resolvedSessions.map { it.startTime }
        val bedtimeCirc = CircularTimeStatistics.compute(bedtimes)

        val wakeTimes = resolvedSessions.map { it.stopTime }
        val wakeTimeCirc = CircularTimeStatistics.compute(wakeTimes)

        val ratings = resolvedSessions.mapNotNull { it.session.rating }
        val avgRating = if (ratings.isNotEmpty()) ratings.average() else null

        val wakeupsList = resolvedSessions.map { it.session.wakeups }
        val avgWakeups = if (wakeupsList.isNotEmpty()) wakeupsList.average() else 0.0

        return PeriodSleepAnalytics(
            coverage = coverage,
            totalSessionsCount = resolvedSessions.size,
            durationSummaryMs = durationSummary,
            bedtimeCircular = bedtimeCirc,
            wakeTimeCircular = wakeTimeCirc,
            averageRating = avgRating,
            averageWakeups = avgWakeups
        )
    }
}
