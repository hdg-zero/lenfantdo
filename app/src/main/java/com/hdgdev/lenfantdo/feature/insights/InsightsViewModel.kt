/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hdgdev.lenfantdo.domain.analytics.CoverageCalculator
import com.hdgdev.lenfantdo.domain.analytics.DateResolutionStrategy
import com.hdgdev.lenfantdo.domain.analytics.PeriodSleepAnalytics
import com.hdgdev.lenfantdo.domain.analytics.SleepDateResolver
import com.hdgdev.lenfantdo.domain.repository.SleepRepository
import com.hdgdev.lenfantdo.tracking.TrackingManager
import com.hdgdev.lenfantdo.ui.component.PeriodOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class DailyChartBar(
    val date: LocalDate,
    val durationHours: Double,
    val isTracked: Boolean,
    val formattedDuration: String,
    val startTimeFormatted: String? = null,
    val stopTimeFormatted: String? = null,
    val rating: Long? = null,
    val wakeups: Int = 0,
    val dayOfWeekShort: String = "",
    val dayOfMonth: Int = 0,
    val deltaFromMeanMinutes: Int = 0,
    val customLabel: String? = null
)

data class InsightsUiState(
    val selectedPeriod: PeriodOption = PeriodOption.LAST_7_DAYS,
    val analytics: PeriodSleepAnalytics? = null,
    val chartBars: List<DailyChartBar> = emptyList(),
    val totalRecordedSessions: Int = 0,
    val hasEnoughData: Boolean = false,
    val regularityScore: Int? = null,
    val regularityLabel: String? = null,
    val weekdayMeanHours: Double? = null,
    val weekendMeanHours: Double? = null,
    val weekdayWeekendDeltaMinutes: Int? = null,
    val totalHoursSlept: Double = 0.0
)

class InsightsViewModel(
    application: Application,
    private val repository: SleepRepository = TrackingManager.getInstance(application).repository
) : AndroidViewModel(application) {

    private val zoneId = ZoneId.systemDefault()

    private val _selectedPeriod = MutableStateFlow(PeriodOption.LAST_7_DAYS)
    val selectedPeriod: StateFlow<PeriodOption> = _selectedPeriod.asStateFlow()

    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE", Locale.FRENCH)

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.observeAllSessions(),
        _selectedPeriod
    ) { allSessions, period ->
        if (allSessions.isEmpty()) {
            return@combine InsightsUiState(
                selectedPeriod = period,
                totalRecordedSessions = 0,
                hasEnoughData = false
            )
        }

        val today = LocalDate.now(zoneId)
        val startDate = if (period.days != null) {
            today.minusDays(period.days - 1)
        } else {
            SleepDateResolver.resolve(allSessions.minBy { it.startEpochMs }, zoneId).logicalDate
        }

        val analytics = CoverageCalculator.computePeriodAnalytics(
            sessions = allSessions,
            startDate = startDate,
            endDate = today,
            zoneId = zoneId,
            strategy = DateResolutionStrategy.WAKE_DATE
        )

        val meanDurationMinutes = if (analytics.totalSessionsCount > 0) {
            (analytics.durationSummaryMs.mean / (1000 * 60)).roundToInt()
        } else 0

        // Generate daily or monthly aggregated bars for charting and accessible table
        val resolvedMap = SleepDateResolver.groupByLogicalDate(allSessions, zoneId)
        val isAggregatedMonthly = period == PeriodOption.LAST_YEAR ||
            (period == PeriodOption.ALL_TIME && ChronoUnit.DAYS.between(startDate, today) > 31)

        val chartBars = if (isAggregatedMonthly) {
            val startMonth = if (period == PeriodOption.LAST_YEAR) {
                YearMonth.from(today).minusMonths(11)
            } else {
                val allTimeStartMonth = YearMonth.from(startDate)
                val currentMonth = YearMonth.from(today)
                val monthsDiff = ChronoUnit.MONTHS.between(allTimeStartMonth, currentMonth)
                if (monthsDiff > 23) currentMonth.minusMonths(23) else allTimeStartMonth
            }
            val currentMonth = YearMonth.from(today)
            val monthCount = (ChronoUnit.MONTHS.between(startMonth, currentMonth) + 1).toInt().coerceAtLeast(1)

            val monthAbbrFormatter = DateTimeFormatter.ofPattern("MMM", Locale.FRENCH)
            val monthFullFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)

            (0 until monthCount).map { offset ->
                val ym = startMonth.plusMonths(offset.toLong())
                val sessionsInMonth = allSessions.filter { s ->
                    val logicalDate = SleepDateResolver.resolve(s, zoneId).logicalDate
                    YearMonth.from(logicalDate) == ym
                }

                val totalDurationSeconds = sessionsInMonth.sumOf { s ->
                    ((s.stopEpochMs - s.startEpochMs) / 1000L).coerceAtLeast(0L)
                }
                val trackedNights = sessionsInMonth.map { SleepDateResolver.resolve(it, zoneId).logicalDate }.distinct().size

                val avgSecondsPerNight = if (trackedNights > 0) totalDurationSeconds / trackedNights else 0L
                val hours = avgSecondsPerNight / 3600.0
                val h = avgSecondsPerNight / 3600
                val m = (avgSecondsPerNight % 3600) / 60

                val formatted = if (trackedNights > 0) {
                    val base = if (h > 0) "${h}h ${m}min / nuit" else "${m}min / nuit"
                    "$base ($trackedNights nuits)"
                } else "Non renseigné"

                val avgRating = sessionsInMonth.mapNotNull { it.rating }.takeIf { it.isNotEmpty() }?.average()?.roundToLong()
                val totalWakeups = sessionsInMonth.sumOf { it.wakeups }

                val rawAbbr = ym.format(monthAbbrFormatter).replace(".", "").replaceFirstChar { it.uppercase() }
                val fullMonthLabel = ym.format(monthFullFormatter).replaceFirstChar { it.uppercase() }

                val avgMinutes = (avgSecondsPerNight / 60).toInt()
                val deltaMinutes = if (trackedNights > 0) avgMinutes - meanDurationMinutes else 0

                DailyChartBar(
                    date = ym.atDay(1),
                    durationHours = hours,
                    isTracked = trackedNights > 0,
                    formattedDuration = formatted,
                    startTimeFormatted = null,
                    stopTimeFormatted = null,
                    rating = avgRating,
                    wakeups = totalWakeups,
                    dayOfWeekShort = rawAbbr,
                    dayOfMonth = ym.monthValue,
                    deltaFromMeanMinutes = deltaMinutes,
                    customLabel = fullMonthLabel
                )
            }
        } else {
            val daysCount = if (period.days != null) {
                period.days.toInt()
            } else {
                (ChronoUnit.DAYS.between(startDate, today) + 1).toInt().coerceAtLeast(1)
            }
            (0 until daysCount).map { i ->
                val date = startDate.plusDays(i.toLong())
                val sessionsOnDate = resolvedMap[date] ?: emptyList()
                val totalSeconds = sessionsOnDate.sumOf { it.durationSeconds }
                val hours = totalSeconds / 3600.0
                val h = totalSeconds / 3600
                val m = (totalSeconds % 3600) / 60
                val formatted = if (sessionsOnDate.isNotEmpty()) {
                    if (h > 0) "${h}h ${m}min" else "${m}min"
                } else "Non renseigné"

                val primarySession = sessionsOnDate.maxByOrNull { it.durationSeconds }
                val startFormatted = primarySession?.let {
                    String.format(Locale.FRENCH, "%02dh%02d", it.startTime.hour, it.startTime.minute)
                }
                val stopFormatted = primarySession?.let {
                    String.format(Locale.FRENCH, "%02dh%02d", it.stopTime.hour, it.stopTime.minute)
                }
                val totalWakeups = sessionsOnDate.sumOf { it.session.wakeups }
                val primaryRating = primarySession?.session?.rating

                val rawDayStr = date.format(dayOfWeekFormatter)
                val dayOfWeekClean = rawDayStr.replace(".", "").replaceFirstChar { it.uppercase() }

                val totalMinutes = (totalSeconds / 60).toInt()
                val deltaMinutes = if (sessionsOnDate.isNotEmpty()) totalMinutes - meanDurationMinutes else 0

                DailyChartBar(
                    date = date,
                    durationHours = hours,
                    isTracked = sessionsOnDate.isNotEmpty(),
                    formattedDuration = formatted,
                    startTimeFormatted = startFormatted,
                    stopTimeFormatted = stopFormatted,
                    rating = primaryRating,
                    wakeups = totalWakeups,
                    dayOfWeekShort = dayOfWeekClean,
                    dayOfMonth = date.dayOfMonth,
                    deltaFromMeanMinutes = deltaMinutes,
                    customLabel = null
                )
            }
        }

        // Regularity score derived from circular concentration R
        val rBed = analytics.bedtimeCircular.resultantVectorLength
        val rWake = analytics.wakeTimeCircular.resultantVectorLength
        val rCombined = (rBed + rWake) / 2.0
        val regularityScore = if (analytics.totalSessionsCount >= 2) {
            (rCombined * 100.0).roundToInt().coerceIn(0, 100)
        } else null

        val regularityLabel = regularityScore?.let { score ->
            when {
                score >= 85 -> "Excellente"
                score >= 70 -> "Bonne"
                score >= 50 -> "Modérée"
                else -> "Variable"
            }
        }

        val trackedBars = chartBars.filter { it.isTracked }
        val weekdayBars = trackedBars.filter { it.date.dayOfWeek.value in 1..5 }
        val weekendBars = trackedBars.filter { it.date.dayOfWeek.value in 6..7 }

        val weekdayMean = if (weekdayBars.isNotEmpty()) weekdayBars.map { it.durationHours }.average() else null
        val weekendMean = if (weekendBars.isNotEmpty()) weekendBars.map { it.durationHours }.average() else null
        val deltaWeekdayWeekend = if (weekdayMean != null && weekendMean != null) {
            ((weekendMean - weekdayMean) * 60).roundToInt()
        } else null

        val totalHours = chartBars.sumOf { it.durationHours }

        InsightsUiState(
            selectedPeriod = period,
            analytics = analytics,
            chartBars = chartBars,
            totalRecordedSessions = allSessions.size,
            hasEnoughData = analytics.totalSessionsCount > 0,
            regularityScore = regularityScore,
            regularityLabel = regularityLabel,
            weekdayMeanHours = weekdayMean,
            weekendMeanHours = weekendMean,
            weekdayWeekendDeltaMinutes = deltaWeekdayWeekend,
            totalHoursSlept = totalHours
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState()
    )

    fun selectPeriod(period: PeriodOption) {
        _selectedPeriod.value = period
    }
}
