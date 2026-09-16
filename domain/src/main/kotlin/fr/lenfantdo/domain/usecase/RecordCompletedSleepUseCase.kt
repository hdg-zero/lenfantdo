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
}
