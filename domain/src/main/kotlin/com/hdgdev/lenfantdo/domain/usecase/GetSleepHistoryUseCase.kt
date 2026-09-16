/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.usecase

import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.domain.repository.SleepRepository
import kotlinx.coroutines.flow.Flow

class GetSleepHistoryUseCase(
    private val sleepRepository: SleepRepository
) {
    fun observe(): Flow<List<SleepSession>> = sleepRepository.observeAllSessions()
    suspend fun getAll(): List<SleepSession> = sleepRepository.getAllSessions()
    suspend fun getById(id: Long): SleepSession? = sleepRepository.getSessionById(id)
}
