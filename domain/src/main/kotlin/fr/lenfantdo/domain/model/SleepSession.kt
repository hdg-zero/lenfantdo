/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.domain.model

data class SleepSession(
    val id: Long = 0,
    val startEpochMs: Long,
    val stopEpochMs: Long,
    val rating: Long? = null,
    val note: String = "",
    val wakeups: Int = 0
) {
    init {
        require(startEpochMs > 0) { "Start timestamp must be positive" }
        require(stopEpochMs >= startEpochMs) { "Stop timestamp ($stopEpochMs) cannot precede start ($startEpochMs)" }
        require(wakeups >= 0) { "Wakeups count cannot be negative" }
        rating?.let {
            require(it in 1..5) { "Rating must be between 1 and 5 (got $it)" }
        }
    }

    val durationMs: Long get() = stopEpochMs - startEpochMs
}
