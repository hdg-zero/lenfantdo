/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.domain.usecase

import fr.lenfantdo.domain.repository.SleepRepository

class StartTrackingUseCase(
    private val sleepRepository: SleepRepository
) {
    suspend operator fun invoke(startEpochMs: Long = System.currentTimeMillis()): Result<Unit> {
        val currentActive = sleepRepository.getActiveTracking()
        if (currentActive != null) {
            return Result.failure(
                IllegalStateException("A tracking session is already in progress since ${currentActive.startEpochMs}")
            )
        }
        val started = sleepRepository.startTracking(startEpochMs)
        return if (started) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed to start tracking session"))
        }
    }
}
