/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.analytics

import com.hdgdev.lenfantdo.domain.model.SleepSession
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class DateResolutionStrategy {
    /**
     * Resolve session to the local date on which the user woke up (session end).
     * Common convention in sleep science ("Tuesday's morning wake-up").
     */
    WAKE_DATE,

    /**
     * Resolve session to the local date on which the user went to bed (session start).
     */
    START_DATE
}

/**
 * Detailed temporal breakdown of a sleep session in a specific time zone.
 */
data class ResolvedSleepSession(
    val session: SleepSession,
    val logicalDate: LocalDate,
    val startDateTime: ZonedDateTime,
    val stopDateTime: ZonedDateTime,
    val startTime: LocalTime,
    val stopTime: LocalTime,
    val durationSeconds: Long
)

object SleepDateResolver {

    /**
     * Resolves a [SleepSession] into local dates and times taking into account timezone and DST transitions.
     */
    fun resolve(
        session: SleepSession,
        zoneId: ZoneId = ZoneId.systemDefault(),
        strategy: DateResolutionStrategy = DateResolutionStrategy.WAKE_DATE
    ): ResolvedSleepSession {
        val startDateTime = Instant.ofEpochMilli(session.startEpochMs).atZone(zoneId)
        val stopDateTime = Instant.ofEpochMilli(session.stopEpochMs).atZone(zoneId)

        val logicalDate = when (strategy) {
            DateResolutionStrategy.WAKE_DATE -> stopDateTime.toLocalDate()
            DateResolutionStrategy.START_DATE -> startDateTime.toLocalDate()
        }

        val durationSeconds = (session.stopEpochMs - session.startEpochMs) / 1000L

        return ResolvedSleepSession(
            session = session,
            logicalDate = logicalDate,
            startDateTime = startDateTime,
            stopDateTime = stopDateTime,
            startTime = startDateTime.toLocalTime(),
            stopTime = stopDateTime.toLocalTime(),
            durationSeconds = durationSeconds
        )
    }

    /**
     * Resolves a collection of sleep sessions and groups them by their logical date.
     */
    fun groupByLogicalDate(
        sessions: List<SleepSession>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        strategy: DateResolutionStrategy = DateResolutionStrategy.WAKE_DATE
    ): Map<LocalDate, List<ResolvedSleepSession>> {
        return sessions
            .map { resolve(it, zoneId, strategy) }
            .groupBy { it.logicalDate }
    }
}
