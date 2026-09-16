/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.usecase

import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.domain.repository.SleepRepository

class StopTrackingUseCase(
    private val sleepRepository: SleepRepository
) {
    suspend operator fun invoke(
        stopEpochMs: Long = System.currentTimeMillis(),
        rating: Long? = null,
        note: String = "",
        wakeups: Int = 0
    ): Result<SleepSession> {
        val currentActive = sleepRepository.getActiveTracking()
            ?: return Result.failure(IllegalStateException("No tracking session is currently active"))

        if (stopEpochMs < currentActive.startEpochMs) {
            return Result.failure(
                IllegalArgumentException("Stop time ($stopEpochMs) cannot precede start time (${currentActive.startEpochMs})")
            )
        }

        val session = sleepRepository.stopTracking(stopEpochMs, rating, note, wakeups)
            ?: return Result.failure(IllegalStateException("Failed to stop tracking session in repository"))

        return Result.success(session)
    }
}
