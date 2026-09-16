/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.domain.usecase

import fr.lenfantdo.domain.model.SleepSession
import fr.lenfantdo.domain.repository.SleepRepository

class UpdateSleepSessionUseCase(
    private val sleepRepository: SleepRepository
) {
    suspend operator fun invoke(session: SleepSession): Result<Unit> {
        return try {
            sleepRepository.updateSession(session)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
