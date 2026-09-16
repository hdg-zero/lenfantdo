/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.test

import com.hdgdev.lenfantdo.domain.model.ActiveTracking
import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.domain.repository.SettingsRepository
import com.hdgdev.lenfantdo.domain.repository.SleepRepository
import com.hdgdev.lenfantdo.domain.repository.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSleepRepository : SleepRepository {
    val sessions = mutableListOf<SleepSession>()
    private val sessionsFlow = MutableStateFlow<List<SleepSession>>(emptyList())
    private val activeFlow = MutableStateFlow<ActiveTracking?>(null)

    private fun updateSessionsFlow() {
        sessionsFlow.value = sessions.toList()
    }

    override fun observeAllSessions(): Flow<List<SleepSession>> = sessionsFlow.asStateFlow()

    override suspend fun getAllSessions(): List<SleepSession> = sessions.toList()

    override suspend fun getSessionById(id: Long): SleepSession? = sessions.find { it.id == id }

    override suspend fun insertSession(session: SleepSession): Long {
        val newId = if (session.id <= 0) (sessions.maxOfOrNull { it.id } ?: 0L) + 1 else session.id
        val newSession = session.copy(id = newId)
        sessions.removeAll { it.id == newId }
        sessions.add(newSession)
        updateSessionsFlow()
        return newId
    }

    override suspend fun insertSessions(sessions: List<SleepSession>) {
        for (s in sessions) {
            insertSession(s)
        }
    }

    override suspend fun updateSession(session: SleepSession) {
        sessions.removeAll { it.id == session.id }
        sessions.add(session)
        updateSessionsFlow()
    }

    override suspend fun deleteSession(session: SleepSession) {
        sessions.removeAll { it.id == session.id }
        updateSessionsFlow()
    }

    override suspend fun deleteSessionById(id: Long) {
        sessions.removeAll { it.id == id }
        updateSessionsFlow()
    }

    override suspend fun deleteAllSessions() {
        sessions.clear()
        updateSessionsFlow()
    }

    override suspend fun countSessions(): Int = sessions.size

    override fun observeActiveTracking(): Flow<ActiveTracking?> = activeFlow.asStateFlow()

    override suspend fun getActiveTracking(): ActiveTracking? = activeFlow.value

    override suspend fun startTracking(startEpochMs: Long): Boolean {
        activeFlow.value = ActiveTracking(startEpochMs = startEpochMs)
        return true
    }

    override suspend fun stopTracking(
        stopEpochMs: Long,
        rating: Long?,
        note: String,
        wakeups: Int
    ): SleepSession? {
        val active = activeFlow.value ?: return null
        activeFlow.value = null
        val session = SleepSession(
            id = (sessions.maxOfOrNull { it.id } ?: 0L) + 1,
            startEpochMs = active.startEpochMs,
            stopEpochMs = stopEpochMs,
            rating = rating,
            note = note,
            wakeups = wakeups
        )
        sessions.add(session)
        updateSessionsFlow()
        return session
    }

    override suspend fun cancelTracking(): Boolean {
        activeFlow.value = null
        return true
    }

    fun setActiveTracking(startEpochMs: Long) {
        activeFlow.value = ActiveTracking(startEpochMs = startEpochMs)
    }
}

class FakeSettingsRepository(
    initialPrefs: UserPreferences = UserPreferences()
) : SettingsRepository {
    private val prefsFlow = MutableStateFlow(initialPrefs)

    override fun observeSettings(): Flow<UserPreferences> = prefsFlow.asStateFlow()

    override suspend fun getSettings(): UserPreferences = prefsFlow.value

    override suspend fun updateTheme(theme: String) {
        prefsFlow.value = prefsFlow.value.copy(appTheme = theme)
    }

    override suspend fun updateCompactView(compact: Boolean) {
        prefsFlow.value = prefsFlow.value.copy(compactView = compact)
    }

    override suspend fun updateBedtime(hour: Int, minute: Int) {
        prefsFlow.value = prefsFlow.value.copy(bedtimeHour = hour, bedtimeMinute = minute)
    }

    override suspend fun updateWakeup(hour: Int, minute: Int) {
        prefsFlow.value = prefsFlow.value.copy(wakeupHour = hour, wakeupMinute = minute)
    }

    override suspend fun updateReminders(bedtime: Boolean, wakeup: Boolean) {
        prefsFlow.value = prefsFlow.value.copy(reminderBedtimeEnabled = bedtime, reminderWakeupEnabled = wakeup)
    }

    override suspend fun updateAutoBackup(enabled: Boolean, path: String?) {
        prefsFlow.value = prefsFlow.value.copy(autoBackupEnabled = enabled, autoBackupPath = path)
    }
}
