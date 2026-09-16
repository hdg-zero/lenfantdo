/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.repository

import com.hdgdev.lenfantdo.domain.model.ActiveTracking
import com.hdgdev.lenfantdo.domain.model.SleepSession
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    fun observeAllSessions(): Flow<List<SleepSession>>
    suspend fun getAllSessions(): List<SleepSession>
    suspend fun getSessionById(id: Long): SleepSession?
    suspend fun insertSession(session: SleepSession): Long
    suspend fun insertSessions(sessions: List<SleepSession>)
    suspend fun updateSession(session: SleepSession)
    suspend fun deleteSession(session: SleepSession)
    suspend fun deleteSessionById(id: Long)
    suspend fun deleteAllSessions()
    suspend fun countSessions(): Int

    // Transactional active tracking
    fun observeActiveTracking(): Flow<ActiveTracking?>
    suspend fun getActiveTracking(): ActiveTracking?
    suspend fun startTracking(startEpochMs: Long): Boolean
    suspend fun stopTracking(
        stopEpochMs: Long,
        rating: Long? = null,
        note: String = "",
        wakeups: Int = 0
    ): SleepSession?
    suspend fun cancelTracking(): Boolean
}
