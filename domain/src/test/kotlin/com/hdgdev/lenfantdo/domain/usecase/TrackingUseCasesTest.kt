/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.usecase

import com.hdgdev.lenfantdo.domain.model.ActiveTracking
import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.domain.repository.SleepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSleepRepository : SleepRepository {
    private val sessions = mutableListOf<SleepSession>()
    private val sessionsFlow = MutableStateFlow<List<SleepSession>>(emptyList())
    private var activeTracking: ActiveTracking? = null
    private val activeTrackingFlow = MutableStateFlow<ActiveTracking?>(null)
    private var nextId = 1L

    override fun observeAllSessions(): Flow<List<SleepSession>> = sessionsFlow.asStateFlow()
    override suspend fun getAllSessions(): List<SleepSession> = sessions.toList()
    override suspend fun getSessionById(id: Long): SleepSession? = sessions.find { it.id == id }

    override suspend fun insertSession(session: SleepSession): Long {
        val id = if (session.id == 0L) nextId++ else session.id
        val newSession = session.copy(id = id)
        sessions.add(newSession)
        sessionsFlow.value = sessions.toList()
        return id
    }

    override suspend fun insertSessions(sessions: List<SleepSession>) {
        sessions.forEach { insertSession(it) }
    }

    override suspend fun updateSession(session: SleepSession) {
        val index = sessions.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            sessions[index] = session
            sessionsFlow.value = sessions.toList()
        }
    }

    override suspend fun deleteSession(session: SleepSession) {
        sessions.removeAll { it.id == session.id }
        sessionsFlow.value = sessions.toList()
    }

    override suspend fun deleteSessionById(id: Long) {
        sessions.removeAll { it.id == id }
        sessionsFlow.value = sessions.toList()
    }

    override suspend fun deleteAllSessions() {
        sessions.clear()
        sessionsFlow.value = emptyList()
    }

    override suspend fun countSessions(): Int = sessions.size

    override fun observeActiveTracking(): Flow<ActiveTracking?> = activeTrackingFlow.asStateFlow()
    override suspend fun getActiveTracking(): ActiveTracking? = activeTracking

    override suspend fun startTracking(startEpochMs: Long): Boolean {
        if (activeTracking != null) return false
        activeTracking = ActiveTracking(startEpochMs)
        activeTrackingFlow.value = activeTracking
        return true
    }

    override suspend fun stopTracking(
        stopEpochMs: Long,
        rating: Long?,
        note: String,
        wakeups: Int
    ): SleepSession? {
        val active = activeTracking ?: return null
        val session = SleepSession(
            id = nextId++,
            startEpochMs = active.startEpochMs,
            stopEpochMs = stopEpochMs,
            rating = rating,
            note = note,
            wakeups = wakeups
        )
        sessions.add(session)
        sessionsFlow.value = sessions.toList()
        activeTracking = null
        activeTrackingFlow.value = null
        return session
    }

    override suspend fun cancelTracking(): Boolean {
        if (activeTracking == null) return false
        activeTracking = null
        activeTrackingFlow.value = null
        return true
    }
}

class TrackingUseCasesTest {
    private lateinit var repository: FakeSleepRepository
    private lateinit var startTracking: StartTrackingUseCase
    private lateinit var stopTracking: StopTrackingUseCase
    private lateinit var cancelTracking: CancelTrackingUseCase
    private lateinit var recordSleep: RecordCompletedSleepUseCase
    private lateinit var updateSleep: UpdateSleepSessionUseCase
    private lateinit var deleteSleep: DeleteSleepSessionUseCase
    private lateinit var getHistory: GetSleepHistoryUseCase

    @Before
    fun setup() {
        repository = FakeSleepRepository()
        startTracking = StartTrackingUseCase(repository)
        stopTracking = StopTrackingUseCase(repository)
        cancelTracking = CancelTrackingUseCase(repository)
        recordSleep = RecordCompletedSleepUseCase(repository)
        updateSleep = UpdateSleepSessionUseCase(repository)
        deleteSleep = DeleteSleepSessionUseCase(repository)
        getHistory = GetSleepHistoryUseCase(repository)
    }

    @Test
    fun startAndStopTrackingProducesValidSession() = runTest {
        val startResult = startTracking(1000L)
        assertTrue(startResult.isSuccess)
        assertNotNull(repository.getActiveTracking())

        val stopResult = stopTracking(
            stopEpochMs = 28800000L + 1000L,
            rating = 5L,
            note = "Excellente nuit",
            wakeups = 0
        )
        assertTrue(stopResult.isSuccess)
        val session = stopResult.getOrThrow()
        assertEquals(1000L, session.startEpochMs)
        assertEquals(28801000L, session.stopEpochMs)
        assertEquals(28800000L, session.durationMs)
        assertEquals(5L, session.rating)
        assertEquals("Excellente nuit", session.note)
        assertNull(repository.getActiveTracking())
    }

    @Test
    fun startingTrackingTwiceFails() = runTest {
        val first = startTracking(1000L)
        assertTrue(first.isSuccess)

        val second = startTracking(2000L)
        assertTrue(second.isFailure)
    }

    @Test
    fun stoppingWithoutActiveTrackingFails() = runTest {
        val result = stopTracking(5000L)
        assertTrue(result.isFailure)
    }

    @Test
    fun stoppingWithTimeBeforeStartFails() = runTest {
        startTracking(5000L)
        val result = stopTracking(4000L)
        assertTrue(result.isFailure)
    }

    @Test
    fun cancelTrackingClearsActiveWithoutSavingSession() = runTest {
        startTracking(1000L)
        assertNotNull(repository.getActiveTracking())

        val cancelResult = cancelTracking()
        assertTrue(cancelResult.isSuccess)
        assertNull(repository.getActiveTracking())
        assertEquals(0, repository.countSessions())
    }

    @Test
    fun recordAndUpdateAndDeleteSession() = runTest {
        val session = SleepSession(
            startEpochMs = 10000L,
            stopEpochMs = 30000L,
            rating = 3L
        )
        val recordResult = recordSleep(session)
        assertTrue(recordResult.isSuccess)
        val id = recordResult.getOrThrow()

        val retrieved = getHistory.getById(id)
        assertNotNull(retrieved)
        assertEquals(3L, retrieved?.rating)

        val updated = retrieved!!.copy(rating = 4L, note = "Mis à jour")
        val updateResult = updateSleep(updated)
        assertTrue(updateResult.isSuccess)
        assertEquals(4L, getHistory.getById(id)?.rating)

        val deleteResult = deleteSleep.byId(id)
        assertTrue(deleteResult.isSuccess)
        assertNull(getHistory.getById(id))
    }
}
