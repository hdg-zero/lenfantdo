/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hdgdev.lenfantdo.domain.assistance.AssistanceEngine
import com.hdgdev.lenfantdo.domain.assistance.AssistanceWarning
import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.domain.usecase.DeleteSleepSessionUseCase
import com.hdgdev.lenfantdo.domain.usecase.UpdateSleepSessionUseCase
import com.hdgdev.lenfantdo.tracking.TrackingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val session: SleepSession? = null,
    val isEditing: Boolean = false,
    val editStartMs: Long = 0L,
    val editStopMs: Long = 0L,
    val editRating: Long? = null,
    val editWakeups: Int = 0,
    val editNote: String = "",
    val warnings: List<AssistanceWarning> = emptyList(),
    val isDeleted: Boolean = false
) {
    val durationMs: Long
        get() = if (isEditing) {
            (editStopMs - editStartMs).coerceAtLeast(0L)
        } else {
            session?.durationMs ?: 0L
        }
}

class SessionDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingManager = TrackingManager.getInstance(application)
    private val repository = trackingManager.repository
    private val updateUseCase = UpdateSleepSessionUseCase(repository)
    private val deleteUseCase = DeleteSleepSessionUseCase(repository)

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var allSessions: List<SleepSession> = emptyList()

    init {
        viewModelScope.launch {
            repository.observeAllSessions().collect { list ->
                allSessions = list
            }
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            if (session != null) {
                _uiState.value = DetailUiState(
                    session = session,
                    editStartMs = session.startEpochMs,
                    editStopMs = session.stopEpochMs,
                    editRating = session.rating,
                    editWakeups = session.wakeups,
                    editNote = session.note
                )
            }
        }
    }

    fun startEditing() {
        val s = _uiState.value.session ?: return
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            editStartMs = s.startEpochMs,
            editStopMs = s.stopEpochMs,
            editRating = s.rating,
            editWakeups = s.wakeups,
            editNote = s.note
        )
        updateWarnings()
    }

    fun cancelEditing() {
        val s = _uiState.value.session ?: return
        _uiState.value = _uiState.value.copy(
            isEditing = false,
            editStartMs = s.startEpochMs,
            editStopMs = s.stopEpochMs,
            editRating = s.rating,
            editWakeups = s.wakeups,
            editNote = s.note,
            warnings = emptyList()
        )
    }

    fun updateTimes(startMs: Long, stopMs: Long) {
        if (stopMs >= startMs) {
            _uiState.value = _uiState.value.copy(
                editStartMs = startMs,
                editStopMs = stopMs
            )
            updateWarnings()
        }
    }

    fun updateRating(rating: Long?) {
        _uiState.value = _uiState.value.copy(editRating = rating)
    }

    fun updateWakeups(wakeups: Int) {
        _uiState.value = _uiState.value.copy(editWakeups = wakeups.coerceAtLeast(0))
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(editNote = note)
    }

    private fun updateWarnings() {
        val s = _uiState.value
        val candidate = SleepSession(
            id = s.session?.id ?: 0L,
            startEpochMs = s.editStartMs,
            stopEpochMs = s.editStopMs,
            rating = s.editRating,
            note = s.editNote,
            wakeups = s.editWakeups
        )
        val warnings = AssistanceEngine.inspectSession(candidate, allSessions)
        _uiState.value = _uiState.value.copy(warnings = warnings)
    }

    fun saveChanges() {
        viewModelScope.launch {
            val s = _uiState.value
            val current = s.session ?: return@launch
            val updated = current.copy(
                startEpochMs = s.editStartMs,
                stopEpochMs = s.editStopMs,
                rating = s.editRating,
                wakeups = s.editWakeups,
                note = s.editNote
            )
            updateUseCase(updated)
            _uiState.value = _uiState.value.copy(
                session = updated,
                isEditing = false,
                warnings = emptyList()
            )
        }
    }

    fun deleteSession(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value.session ?: return@launch
            deleteUseCase(s.id)
            _uiState.value = _uiState.value.copy(isDeleted = true)
            onDeleted()
        }
    }
}
