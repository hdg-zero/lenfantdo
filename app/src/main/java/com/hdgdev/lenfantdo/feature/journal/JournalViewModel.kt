/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.journal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hdgdev.lenfantdo.data.repository.SettingsRepositoryImpl
import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.domain.repository.SettingsRepository
import com.hdgdev.lenfantdo.domain.repository.SleepRepository
import com.hdgdev.lenfantdo.domain.usecase.DeleteSleepSessionUseCase
import com.hdgdev.lenfantdo.domain.usecase.GetSleepHistoryUseCase
import com.hdgdev.lenfantdo.tracking.TrackingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class JournalUiState(
    val sessions: List<SleepSession> = emptyList(),
    val groupedSessions: Map<String, List<SleepSession>> = emptyMap(),
    val searchQuery: String = "",
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val isCompactView: Boolean = false
)

class JournalViewModel(
    application: Application,
    private val repository: SleepRepository = TrackingManager.getInstance(application).repository,
    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl.getInstance(application)
) : AndroidViewModel(application) {

    private val deleteUseCase = DeleteSleepSessionUseCase(repository)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)
    private val zoneId = ZoneId.systemDefault()

    val uiState: StateFlow<JournalUiState> = combine(
        repository.observeAllSessions(),
        _searchQuery,
        settingsRepository.observeSettings()
    ) { allSessions, query, prefs ->
        val sorted = allSessions.sortedByDescending { it.stopEpochMs }
        val filtered = if (query.isBlank()) {
            sorted
        } else {
            val q = query.trim().lowercase()
            sorted.filter { session ->
                session.note.lowercase().contains(q)
            }
        }

        val grouped = filtered.groupBy { session ->
            val zdt = Instant.ofEpochMilli(session.stopEpochMs).atZone(zoneId)
            zdt.format(monthFormatter).replaceFirstChar { it.uppercase() }
        }

        JournalUiState(
            sessions = filtered,
            groupedSessions = grouped,
            searchQuery = query,
            totalCount = allSessions.size,
            isLoading = false,
            isCompactView = prefs.compactView
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JournalUiState(isLoading = true)
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            deleteUseCase(sessionId)
        }
    }
}
