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
import com.hdgdev.lenfantdo.domain.model.SleepSession
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

data class DailyChartBar(
    val date: LocalDate,
    val durationHours: Double,
    val isTracked: Boolean,
    val formattedDuration: String
)

data class InsightsUiState(
    val selectedPeriod: PeriodOption = PeriodOption.LAST_7_DAYS,
    val analytics: PeriodSleepAnalytics? = null,
    val chartBars: List<DailyChartBar> = emptyList(),
    val totalRecordedSessions: Int = 0,
    val hasEnoughData: Boolean = false
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingManager = TrackingManager.getInstance(application)
    private val repository = trackingManager.repository
    private val zoneId = ZoneId.systemDefault()

    private val _selectedPeriod = MutableStateFlow(PeriodOption.LAST_7_DAYS)
    val selectedPeriod: StateFlow<PeriodOption> = _selectedPeriod.asStateFlow()

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
            val minEpoch = allSessions.minOf { it.startEpochMs }
            SleepDateResolver.resolve(allSessions.minBy { it.startEpochMs }, zoneId).logicalDate
        }

        val analytics = CoverageCalculator.computePeriodAnalytics(
            sessions = allSessions,
            startDate = startDate,
            endDate = today,
            zoneId = zoneId,
            strategy = DateResolutionStrategy.WAKE_DATE
        )

        // Generate daily bars for charting and accessible table
        val resolvedMap = SleepDateResolver.groupByLogicalDate(allSessions, zoneId)
        val daysCount = if (period.days != null) period.days.toInt() else 30.coerceAtMost((analytics.coverage.totalCalendarDays).toInt())
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

            DailyChartBar(
                date = date,
                durationHours = hours,
                isTracked = sessionsOnDate.isNotEmpty(),
                formattedDuration = formatted
            )
        }

        InsightsUiState(
            selectedPeriod = period,
            analytics = analytics,
            chartBars = chartBars,
            totalRecordedSessions = allSessions.size,
            hasEnoughData = analytics.totalSessionsCount > 0
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
