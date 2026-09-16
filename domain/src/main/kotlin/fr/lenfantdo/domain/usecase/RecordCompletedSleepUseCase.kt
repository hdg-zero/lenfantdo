/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.domain.usecase

import fr.lenfantdo.domain.model.SleepSession
import fr.lenfantdo.domain.repository.SleepRepository

class RecordCompletedSleepUseCase(
    private val sleepRepository: SleepRepository
) {
    suspend operator fun invoke(session: SleepSession): Result<Long> {
        return try {
            val id = sleepRepository.insertSession(session)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend operator fun invoke(
        startEpochMs: Long,
        stopEpochMs: Long,
        rating: Long? = null,
        note: String = "",
        wakeups: Int = 0
    ): Result<Long> {
        return try {
            val session = SleepSession(
                startEpochMs = startEpochMs,
                stopEpochMs = stopEpochMs,
                rating = rating,
                note = note,
                wakeups = wakeups
            )
            val id = sleepRepository.insertSession(session)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
