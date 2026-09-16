/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.data.repository

import androidx.room.withTransaction
import com.hdgdev.lenfantdo.data.local.AppDatabase
import com.hdgdev.lenfantdo.data.local.entity.ActiveTrackingEntity
import com.hdgdev.lenfantdo.data.local.entity.SleepSessionEntity
import com.hdgdev.lenfantdo.domain.model.ActiveTracking
import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.domain.repository.SleepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SleepRepositoryImpl(
    private val database: AppDatabase
) : SleepRepository {

    private val sleepDao = database.sleepSessionDao()
    private val activeDao = database.activeTrackingDao()

    override fun observeAllSessions(): Flow<List<SleepSession>> {
        return sleepDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAllSessions(): List<SleepSession> {
        return sleepDao.getAll().map { it.toDomain() }
    }

    override suspend fun getSessionById(id: Long): SleepSession? {
        return sleepDao.getById(id)?.toDomain()
    }

    override suspend fun insertSession(session: SleepSession): Long {
        return sleepDao.insert(SleepSessionEntity.fromDomain(session))
    }

    override suspend fun insertSessions(sessions: List<SleepSession>) {
        sleepDao.insertAll(sessions.map { SleepSessionEntity.fromDomain(it) })
    }

    override suspend fun updateSession(session: SleepSession) {
        sleepDao.update(SleepSessionEntity.fromDomain(session))
    }

    override suspend fun deleteSession(session: SleepSession) {
        sleepDao.delete(SleepSessionEntity.fromDomain(session))
    }

    override suspend fun deleteSessionById(id: Long) {
        sleepDao.deleteById(id)
    }

    override suspend fun deleteAllSessions() {
        database.withTransaction {
            sleepDao.deleteAll()
            activeDao.clearActive()
        }
    }

    override suspend fun countSessions(): Int {
        return sleepDao.count()
    }

    override fun observeActiveTracking(): Flow<ActiveTracking?> {
        return activeDao.observeActive().map { it?.toDomain() }
    }

    override suspend fun getActiveTracking(): ActiveTracking? {
        return activeDao.getActive()?.toDomain()
    }

    override suspend fun startTracking(startEpochMs: Long): Boolean {
        return database.withTransaction {
            val existing = activeDao.getActive()
            if (existing != null) {
                return@withTransaction false
            }
            activeDao.setActive(ActiveTrackingEntity(slot = 1, startEpochMs = startEpochMs))
            true
        }
    }

    override suspend fun stopTracking(
        stopEpochMs: Long,
        rating: Long?,
        note: String,
        wakeups: Int
    ): SleepSession? {
        return database.withTransaction {
            val active = activeDao.getActive() ?: return@withTransaction null
            val entity = SleepSessionEntity(
                id = 0,
                startDate = active.startEpochMs,
                stopDate = stopEpochMs,
                rating = rating ?: 0L,
                comment = note,
                wakes = wakeups
            )
            val newId = sleepDao.insert(entity)
            activeDao.clearActive()
            entity.copy(id = newId).toDomain()
        }
    }

    override suspend fun cancelTracking(): Boolean {
        return database.withTransaction {
            val active = activeDao.getActive() ?: return@withTransaction false
            activeDao.clearActive()
            true
        }
    }
}
