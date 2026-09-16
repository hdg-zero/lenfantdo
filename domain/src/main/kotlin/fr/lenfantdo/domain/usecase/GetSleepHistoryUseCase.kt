/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.domain.usecase

import fr.lenfantdo.domain.model.SleepSession
import fr.lenfantdo.domain.repository.SleepRepository
import kotlinx.coroutines.flow.Flow

class GetSleepHistoryUseCase(
    private val sleepRepository: SleepRepository
) {
    fun observe(): Flow<List<SleepSession>> = sleepRepository.observeAllSessions()
    suspend fun getAll(): List<SleepSession> = sleepRepository.getAllSessions()
    suspend fun getById(id: Long): SleepSession? = sleepRepository.getSessionById(id)
}
