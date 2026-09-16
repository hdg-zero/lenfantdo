/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.analytics

import kotlin.math.sqrt

/**
 * Summary of descriptive statistics computed using Welford's algorithm.
 *
 * @param count Number of elements processed.
 * @param mean Arithmetic mean.
 * @param sampleVariance Sample variance (divided by N - 1, 0.0 if count <= 1).
 * @param sampleStandardDeviation Sample standard deviation.
 * @param min Minimum value observed (or null if empty).
 * @param max Maximum value observed (or null if empty).
 */
data class WelfordSummary(
    val count: Long,
    val mean: Double,
    val sampleVariance: Double,
    val sampleStandardDeviation: Double,
    val min: Double?,
    val max: Double?
)

/**
 * Numerically stable single-pass accumulator for mean, variance, and standard deviation.
 * Based on B. P. Welford (1962).
 */
class WelfordAccumulator {
    var count: Long = 0L
        private set

    var mean: Double = 0.0
        private set

    private var m2: Double = 0.0

    var min: Double? = null
        private set

    var max: Double? = null
        private set

    fun add(value: Double) {
        count++
        val delta = value - mean
        mean += delta / count
        val delta2 = value - mean
        m2 += delta * delta2

        min = min?.let { kotlin.math.min(it, value) } ?: value
        max = max?.let { kotlin.math.max(it, value) } ?: value
    }

    val sampleVariance: Double
        get() = if (count > 1L) m2 / (count - 1L) else 0.0

    val populationVariance: Double
        get() = if (count > 0L) m2 / count else 0.0

    val sampleStandardDeviation: Double
        get() = sqrt(sampleVariance)

    val populationStandardDeviation: Double
        get() = sqrt(populationVariance)

    fun toSummary(): WelfordSummary = WelfordSummary(
        count = count,
        mean = mean,
        sampleVariance = sampleVariance,
        sampleStandardDeviation = sampleStandardDeviation,
        min = min,
        max = max
    )
}

/**
 * Convenience extensions for calculating Welford statistics.
 */
fun Iterable<Double>.welfordStats(): WelfordSummary {
    val acc = WelfordAccumulator()
    for (item in this) {
        acc.add(item)
    }
    return acc.toSummary()
}

fun Iterable<Long>.welfordStatsOfLong(): WelfordSummary {
    val acc = WelfordAccumulator()
    for (item in this) {
        acc.add(item.toDouble())
    }
    return acc.toSummary()
}
