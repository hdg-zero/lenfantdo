/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.domain.model

data class ActiveTracking(
    val startEpochMs: Long
) {
    init {
        require(startEpochMs > 0) { "Active tracking start timestamp must be positive" }
    }

    fun elapsedMs(currentEpochMs: Long): Long =
        (currentEpochMs - startEpochMs).coerceAtLeast(0)
}
