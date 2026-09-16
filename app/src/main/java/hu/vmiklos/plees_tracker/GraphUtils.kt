/*
 * Copyright 2023 Miklos Vajna
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package hu.vmiklos.plees_tracker

import java.util.Calendar
import java.util.Date
import kotlin.math.sqrt

/** Generates a cumulative moving average list of the provided day-value points. */
fun List<Pair<Number, Number>>.cumulativeAverage(): List<Pair<Number, Number>> {
    if (isEmpty()) return emptyList()
    return mutableListOf(first()).also {
        subList(1, size).forEach { (day, value) ->
            val oldAvg = it.last().second.toFloat()
            val newAvg = ((oldAvg * it.size) + value.toFloat()) / (it.size + 1)
            it.add(day to newAvg)
        }
    }
}

/** Given a list of sleeps, returns the cumulative variance. */
fun <K : Number> Sequence<Pair<K, Number>>.cumulativeVariance(): Sequence<Pair<K, Float>> {
    var sumSquared = 0f
    var sum = 0f

    return mapIndexed { index, (key, sleepNum) ->
        val sleep = sleepNum.toFloat()
        sumSquared += sleep * sleep
        sum += sleep
        val itemNum = index + 1

        // E[X^2] - E[X]^2
        val mean1 = sumSquared / itemNum
        val mean2 = (sum / itemNum).let { it * it }
        val variance = (mean1 - mean2)

        key to variance
    }
}

fun List<Pair<Number, Number>>.varianceToDeviation(): List<Pair<Number, Number>> =
    map { (date, num) -> date to sqrt(num.toFloat()) }

/** Generates a cumulative sum list of the provided day-value points. */
fun List<Pair<Number, Number>>.cumulativeSum(): List<Pair<Number, Number>> {
    if (isEmpty()) return emptyList()
    return mutableListOf(first()).also {
        subList(1, size).forEach { (day, value) ->
            it.add(day to value.toFloat() + it.last().second.toFloat())
        }
    }
}

/** Strips the time from a UNIX epoch timestamp (ms). */
fun Long.stripTime(): Long = Date(this).stripTime().time

/** Strips the time from a [Date], leaving only the date. */
fun Date.stripTime(): Date {
    return Calendar.getInstance().apply {
        time = this@stripTime
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
}
