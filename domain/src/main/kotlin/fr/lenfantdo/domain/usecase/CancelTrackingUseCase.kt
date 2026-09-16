/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.domain.usecase

import fr.lenfantdo.domain.repository.SleepRepository

class CancelTrackingUseCase(
    private val sleepRepository: SleepRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val currentActive = sleepRepository.getActiveTracking()
            ?: return Result.failure(IllegalStateException("No tracking session is currently active"))

        val cancelled = sleepRepository.cancelTracking()
        return if (cancelled) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed to cancel tracking session"))
        }
    }
}
