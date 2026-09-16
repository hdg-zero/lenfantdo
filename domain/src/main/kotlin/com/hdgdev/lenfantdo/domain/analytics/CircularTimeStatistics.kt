/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.analytics

import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Result of circular statistics on time-of-day data.
 *
 * @param meanTime The computed mean time of day, or null if sample is empty or direction is undefined.
 * @param meanSecondsFromMidnight Mean time expressed in fractional seconds from 00:00:00.
 * @param resultantVectorLength R in [0.0, 1.0], measuring concentration (1.0 = identical times, 0.0 = uniform).
 * @param circularVariance 1.0 - R (0.0 = no variance, 1.0 = maximal dispersion).
 * @param circularStandardDeviationStdHours Circular standard deviation converted to hours, or null if undefined.
 * @param sampleSize Number of observations included.
 */
data class CircularTimeSummary(
    val meanTime: LocalTime?,
    val meanSecondsFromMidnight: Double?,
    val resultantVectorLength: Double,
    val circularVariance: Double,
    val circularStandardDeviationStdHours: Double?,
    val sampleSize: Int
)

/**
 * Pure circular statistics engine for time-of-day calculations (bedtime, wake-up time).
 * Eliminates the "midnight trap" where arithmetic averages of 23:50 and 00:10 yield 12:00.
 *
 * Each time is mapped to an angle θ = 2π * (seconds / 86400) on the unit circle.
 */
object CircularTimeStatistics {
    private const val SECONDS_IN_DAY = 86400.0
    private const val TWO_PI = 2.0 * PI

    /**
     * Computes the circular mean and dispersion of a list of [LocalTime]s.
     */
    fun compute(times: List<LocalTime>): CircularTimeSummary {
        if (times.isEmpty()) {
            return CircularTimeSummary(
                meanTime = null,
                meanSecondsFromMidnight = null,
                resultantVectorLength = 0.0,
                circularVariance = 1.0,
                circularStandardDeviationStdHours = null,
                sampleSize = 0
            )
        }

        var sumCos = 0.0
        var sumSin = 0.0

        for (time in times) {
            val secondsFromMidnight = time.toNanoOfDay() / 1_000_000_000.0
            val angle = (secondsFromMidnight / SECONDS_IN_DAY) * TWO_PI
            sumCos += cos(angle)
            sumSin += sin(angle)
        }

        val n = times.size
        val meanCos = sumCos / n
        val meanSin = sumSin / n
        val r = sqrt(meanCos * meanCos + meanSin * meanSin)

        if (r < 1e-7) {
            // Direction is mathematically undefined (e.g. times perfectly opposed or uniformly distributed)
            return CircularTimeSummary(
                meanTime = null,
                meanSecondsFromMidnight = null,
                resultantVectorLength = r,
                circularVariance = 1.0,
                circularStandardDeviationStdHours = null,
                sampleSize = n
            )
        }

        var meanAngle = atan2(meanSin, meanCos)
        if (meanAngle < 0.0) {
            meanAngle += TWO_PI
        }

        val meanSeconds = (meanAngle / TWO_PI) * SECONDS_IN_DAY
        val totalNanos = kotlin.math.round(meanSeconds * 1_000_000_000.0).toLong()
        val normalizedNanos = (totalNanos % 86_400_000_000_000L + 86_400_000_000_000L) % 86_400_000_000_000L
        val meanLocalTime = LocalTime.ofNanoOfDay(normalizedNanos)
        val normalizedSeconds = normalizedNanos / 1_000_000_000.0

        val circularVariance = (1.0 - r).coerceIn(0.0, 1.0)
        // Mardia & Jupp circular standard deviation: sqrt(-2 * ln(R)) in radians
        val circularStdHours = if (r > 0.0 && r <= 1.0) {
            val stdRadians = sqrt(-2.0 * ln(r.coerceAtMost(1.0 - 1e-15)))
            (stdRadians / TWO_PI) * 24.0
        } else {
            null
        }

        return CircularTimeSummary(
            meanTime = meanLocalTime,
            meanSecondsFromMidnight = normalizedSeconds,
            resultantVectorLength = r,
            circularVariance = circularVariance,
            circularStandardDeviationStdHours = circularStdHours,
            sampleSize = n
        )
    }
}
