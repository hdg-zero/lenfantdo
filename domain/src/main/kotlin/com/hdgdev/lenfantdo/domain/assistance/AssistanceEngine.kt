/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.assistance

import com.hdgdev.lenfantdo.domain.model.SleepSession

enum class AssistanceWarningType {
    ACTIVE_TRACKING_EXCESSIVE,
    SESSION_TOO_SHORT,
    SESSION_TOO_LONG,
    SESSION_OVERLAPS
}

data class AssistanceWarning(
    val type: AssistanceWarningType,
    val message: String
)

/**
 * Deterministic, explainable assistance engine to detect common tracking anomalies.
 * Operates purely offline with zero heuristic ML/LLM models.
 */
object AssistanceEngine {
    // 14 hours in milliseconds
    const val MAX_REASONABLE_ACTIVE_MS = 14 * 60 * 60 * 1000L

    // 30 minutes in milliseconds
    const val MIN_REASONABLE_SESSION_MS = 30 * 60 * 1000L

    // 18 hours in milliseconds for a recorded session
    const val MAX_REASONABLE_SESSION_MS = 18 * 60 * 60 * 1000L

    /**
     * Checks whether an ongoing tracking session has exceeded reasonable limits.
     */
    fun inspectActiveTracking(startEpochMs: Long, currentEpochMs: Long): AssistanceWarning? {
        val elapsed = currentEpochMs - startEpochMs
        if (elapsed > MAX_REASONABLE_ACTIVE_MS) {
            val hours = elapsed / (60 * 60 * 1000L)
            return AssistanceWarning(
                type = AssistanceWarningType.ACTIVE_TRACKING_EXCESSIVE,
                message = "Le suivi est actif depuis $hours heures. Avez-vous oublié d'arrêter le suivi à votre réveil ?"
            )
        }
        return null
    }

    /**
     * Inspects a session before confirmation or saving.
     */
    fun inspectSession(
        session: SleepSession,
        existingSessions: List<SleepSession> = emptyList()
    ): List<AssistanceWarning> {
        val warnings = mutableListOf<AssistanceWarning>()
        val duration = session.durationMs

        if (duration < MIN_REASONABLE_SESSION_MS) {
            val minutes = duration / (60 * 1000L)
            warnings.add(
                AssistanceWarning(
                    type = AssistanceWarningType.SESSION_TOO_SHORT,
                    message = "Cette session ne dure que $minutes minute${if (minutes > 1) "s" else ""}. S'agit-il d'une courte sieste ou d'une erreur de manipulation ?"
                )
            )
        } else if (duration > MAX_REASONABLE_SESSION_MS) {
            val hours = duration / (60 * 60 * 1000L)
            warnings.add(
                AssistanceWarning(
                    type = AssistanceWarningType.SESSION_TOO_LONG,
                    message = "Cette session dure $hours heures. Vérifiez que l'heure de réveil est exacte."
                )
            )
        }

        // Check for overlaps with existing confirmed sessions
        val hasOverlap = existingSessions.any { existing ->
            existing.id != session.id &&
                session.startEpochMs < existing.stopEpochMs &&
                session.stopEpochMs > existing.startEpochMs
        }
        if (hasOverlap) {
            warnings.add(
                AssistanceWarning(
                    type = AssistanceWarningType.SESSION_OVERLAPS,
                    message = "Cet intervalle chevauche une autre session de sommeil déjà enregistrée."
                )
            )
        }

        return warnings
    }
}
