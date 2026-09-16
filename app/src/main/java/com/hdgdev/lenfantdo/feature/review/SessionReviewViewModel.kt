/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hdgdev.lenfantdo.domain.assistance.AssistanceEngine
import com.hdgdev.lenfantdo.domain.assistance.AssistanceWarning
import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.domain.usecase.DeleteSleepSessionUseCase
import com.hdgdev.lenfantdo.domain.usecase.RecordCompletedSleepUseCase
import com.hdgdev.lenfantdo.domain.usecase.UpdateSleepSessionUseCase
import com.hdgdev.lenfantdo.tracking.TrackingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewUiState(
    val sessionId: Long = 0L,
    val isNewManualSession: Boolean = false,
    val startEpochMs: Long = 0L,
    val stopEpochMs: Long = 0L,
    val rating: Long? = null,
    val note: String = "",
    val wakeups: Int = 0,
    val warnings: List<AssistanceWarning> = emptyList(),
    val isSaved: Boolean = false
) {
    val durationMs: Long get() = (stopEpochMs - startEpochMs).coerceAtLeast(0L)
}

class SessionReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingManager = TrackingManager.getInstance(application)
    private val repository = trackingManager.repository

    private val updateSessionUseCase = UpdateSleepSessionUseCase(repository)
    private val recordCompletedUseCase = RecordCompletedSleepUseCase(repository)
    private val deleteSessionUseCase = DeleteSleepSessionUseCase(repository)

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private var allSessions: List<SleepSession> = emptyList()

    init {
        viewModelScope.launch {
            repository.observeAllSessions().collect { list ->
                allSessions = list
                updateWarnings()
            }
        }
    }

    fun initFromSession(session: SleepSession) {
        _uiState.value = ReviewUiState(
            sessionId = session.id,
            isNewManualSession = session.id == 0L,
            startEpochMs = session.startEpochMs,
            stopEpochMs = session.stopEpochMs,
            rating = session.rating,
            note = session.note,
            wakeups = session.wakeups
        )
        updateWarnings()
    }

    fun initForManualEntry() {
        val now = System.currentTimeMillis()
        val eightHoursAgo = now - (8 * 3600 * 1000L)
        _uiState.value = ReviewUiState(
            sessionId = 0L,
            isNewManualSession = true,
            startEpochMs = eightHoursAgo,
            stopEpochMs = now,
            rating = null,
            note = "",
            wakeups = 0
        )
        updateWarnings()
    }

    fun updateTimes(startEpochMs: Long, stopEpochMs: Long) {
        if (stopEpochMs >= startEpochMs) {
            _uiState.value = _uiState.value.copy(
                startEpochMs = startEpochMs,
                stopEpochMs = stopEpochMs
            )
            updateWarnings()
        }
    }

    fun updateRating(rating: Long?) {
        _uiState.value = _uiState.value.copy(rating = rating)
    }

    fun updateWakeups(wakeups: Int) {
        _uiState.value = _uiState.value.copy(wakeups = wakeups.coerceAtLeast(0))
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    private fun updateWarnings() {
        val s = _uiState.value
        if (s.startEpochMs > 0L && s.stopEpochMs >= s.startEpochMs) {
            val candidate = SleepSession(
                id = s.sessionId,
                startEpochMs = s.startEpochMs,
                stopEpochMs = s.stopEpochMs,
                rating = s.rating,
                note = s.note,
                wakeups = s.wakeups
            )
            val warnings = AssistanceEngine.inspectSession(candidate, allSessions)
            _uiState.value = _uiState.value.copy(warnings = warnings)
        }
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value
            if (s.isNewManualSession) {
                recordCompletedUseCase(
                    startEpochMs = s.startEpochMs,
                    stopEpochMs = s.stopEpochMs,
                    rating = s.rating,
                    note = s.note,
                    wakeups = s.wakeups
                )
            } else {
                val updated = SleepSession(
                    id = s.sessionId,
                    startEpochMs = s.startEpochMs,
                    stopEpochMs = s.stopEpochMs,
                    rating = s.rating,
                    note = s.note,
                    wakeups = s.wakeups
                )
                updateSessionUseCase(updated)
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
            onDone()
        }
    }

    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value
            if (!s.isNewManualSession && s.sessionId > 0L) {
                deleteSessionUseCase(s.sessionId)
            }
            onDone()
        }
    }
}
