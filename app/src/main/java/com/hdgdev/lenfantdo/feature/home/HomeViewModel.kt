/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hdgdev.lenfantdo.domain.assistance.AssistanceEngine
import com.hdgdev.lenfantdo.domain.assistance.AssistanceWarning
import com.hdgdev.lenfantdo.domain.model.ActiveTracking
import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.domain.repository.SleepRepository
import com.hdgdev.lenfantdo.tracking.TrackingManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val activeTracking: ActiveTracking? = null,
    val elapsedSeconds: Long = 0L,
    val lastSession: SleepSession? = null,
    val activeWarning: AssistanceWarning? = null,
    val totalRecordedNights: Int = 0
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    application: Application,
    private val trackingManager: TrackingManager = TrackingManager.getInstance(application),
    private val repository: SleepRepository = trackingManager.repository
) : AndroidViewModel(application) {

    private data class ActiveState(
        val active: ActiveTracking?,
        val elapsed: Long,
        val warning: AssistanceWarning?
    )

    private val activeStateFlow = trackingManager.observeActiveTracking().flatMapLatest { active ->
        if (active != null) {
            flow {
                while (true) {
                    val now = System.currentTimeMillis()
                    val elapsed = ((now - active.startEpochMs) / 1000L).coerceAtLeast(0L)
                    val warning = AssistanceEngine.inspectActiveTracking(active.startEpochMs, now)
                    emit(ActiveState(active = active, elapsed = elapsed, warning = warning))
                    delay(1000L)
                }
            }
        } else {
            kotlinx.coroutines.flow.flowOf(ActiveState(active = null, elapsed = 0L, warning = null))
        }
    }

    private data class SessionSummary(
        val lastSession: SleepSession?,
        val totalCount: Int
    )

    private val sessionsSummaryFlow = repository.observeAllSessions().map { sessions ->
        SessionSummary(
            lastSession = sessions.maxByOrNull { it.stopEpochMs },
            totalCount = sessions.size
        )
    }.distinctUntilChanged()

    val uiState: StateFlow<HomeUiState> = combine(
        activeStateFlow,
        sessionsSummaryFlow
    ) { activeState, sessionsSummary ->
        HomeUiState(
            activeTracking = activeState.active,
            elapsedSeconds = activeState.elapsed,
            lastSession = sessionsSummary.lastSession,
            activeWarning = activeState.warning,
            totalRecordedNights = sessionsSummary.totalCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun startTracking() {
        viewModelScope.launch {
            trackingManager.start()
        }
    }

    fun stopTracking(onStopped: (SleepSession) -> Unit) {
        viewModelScope.launch {
            val result = trackingManager.stop()
            result.onSuccess { session ->
                onStopped(session)
            }
        }
    }

    fun cancelTracking() {
        viewModelScope.launch {
            trackingManager.cancel()
        }
    }
}
