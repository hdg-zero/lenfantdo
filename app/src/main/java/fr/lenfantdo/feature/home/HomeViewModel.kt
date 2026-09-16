/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.lenfantdo.domain.assistance.AssistanceEngine
import fr.lenfantdo.domain.assistance.AssistanceWarning
import fr.lenfantdo.domain.model.ActiveTracking
import fr.lenfantdo.domain.model.SleepSession
import fr.lenfantdo.tracking.TrackingManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val activeTracking: ActiveTracking? = null,
    val elapsedSeconds: Long = 0L,
    val lastSession: SleepSession? = null,
    val activeWarning: AssistanceWarning? = null,
    val totalRecordedNights: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingManager = TrackingManager.getInstance(application)
    private val repository = trackingManager.repository

    // Emit a ticker every second when tracking is active
    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        trackingManager.observeActiveTracking(),
        repository.observeAllSessions(),
        tickerFlow
    ) { active, allSessions, currentTime ->
        val elapsed = if (active != null) {
            ((currentTime - active.startEpochMs) / 1000L).coerceAtLeast(0L)
        } else 0L

        val warning = if (active != null) {
            AssistanceEngine.inspectActiveTracking(active.startEpochMs, currentTime)
        } else null

        val last = allSessions.maxByOrNull { it.stopEpochMs }

        HomeUiState(
            activeTracking = active,
            elapsedSeconds = elapsed,
            lastSession = last,
            activeWarning = warning,
            totalRecordedNights = allSessions.size
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
