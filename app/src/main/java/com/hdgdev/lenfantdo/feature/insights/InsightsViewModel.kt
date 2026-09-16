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
import com.hdgdev.lenfantdo.tracking.TrackingManager
import com.hdgdev.lenfantdo.ui.component.PeriodOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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
    val deltaFromMeanMinutes: Int = 0
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

class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingManager = TrackingManager.getInstance(application)
    private val repository = trackingManager.repository
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

        // Generate daily bars for charting and accessible table
        val resolvedMap = SleepDateResolver.groupByLogicalDate(allSessions, zoneId)
        val daysCount = if (period.days != null) period.days.toInt() else 30.coerceAtMost(analytics.coverage.totalCalendarDays.toInt())
        val chartBars = (0 until daysCount).map { i ->
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
                deltaFromMeanMinutes = deltaMinutes
            )
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
