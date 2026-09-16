/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.data.export

import com.hdgdev.lenfantdo.domain.model.SleepSession
import java.io.BufferedReader
import java.io.Reader

class LegacyCsvParser {

    data class ParseResult(
        val sessions: List<SleepSession>,
        val skippedLinesCount: Int
    )

    fun parse(reader: Reader): ParseResult {
        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader)
        val lines = bufferedReader.readLines()
        if (lines.isEmpty()) return ParseResult(emptyList(), 0)

        val sessions = mutableListOf<SleepSession>()
        var skippedCount = 0
        var headerProcessed = false
        var colMap: Map<String, Int> = emptyMap()

        for (rawLine in lines) {
            val line = rawLine.trim().removePrefix("\uFEFF")
            if (line.isBlank()) continue

            val tokens = parseCsvLine(line)
            if (tokens.isEmpty()) continue

            if (!headerProcessed) {
                if (isHeaderLine(tokens)) {
                    colMap = tokens.mapIndexed { idx, name ->
                        name.lowercase().trim() to idx
                    }.toMap()
                    headerProcessed = true
                    continue
                } else {
                    colMap = mapOf(
                        "sid" to 0,
                        "start" to 1,
                        "stop" to 2,
                        "rating" to 3,
                        "comment" to 4,
                        "wakes" to 5
                    )
                    headerProcessed = true
                }
            }

            val session = parseSessionRow(tokens, colMap)
            if (session != null) {
                sessions.add(session)
            } else {
                skippedCount++
            }
        }

        return ParseResult(sessions, skippedCount)
    }

    private fun isHeaderLine(tokens: List<String>): Boolean {
        val first = tokens.firstOrNull()?.lowercase()?.trim() ?: return false
        return first == "sid" || first == "start" || first == "id"
    }

    private fun parseSessionRow(tokens: List<String>, colMap: Map<String, Int>): SleepSession? {
        try {
            val startIdx = colMap["start"] ?: 1
            val stopIdx = colMap["stop"] ?: 2
            if (startIdx >= tokens.size || stopIdx >= tokens.size) return null

            var start = tokens[startIdx].toLongOrNull() ?: return null
            var stop = tokens[stopIdx].toLongOrNull() ?: return null

            if (start in 1..9_999_999_999L) start *= 1000
            if (stop in 1..9_999_999_999L) stop *= 1000

            if (start <= 0 || stop < start) return null

            val sidIdx = colMap["sid"] ?: 0
            val sid = if (sidIdx < tokens.size) tokens[sidIdx].toLongOrNull() ?: 0L else 0L

            val ratingIdx = colMap["rating"] ?: 3
            val rawRating = if (ratingIdx < tokens.size) tokens[ratingIdx].toLongOrNull() else null
            val rating = if (rawRating != null && rawRating in 1..5) rawRating else null

            val commentIdx = colMap["comment"] ?: 4
            val comment = if (commentIdx < tokens.size) tokens[commentIdx] else ""

            val wakesIdx = colMap["wakes"] ?: 5
            val wakes = if (wakesIdx < tokens.size) {
                (tokens[wakesIdx].toIntOrNull() ?: 0).coerceAtLeast(0)
            } else 0

            return SleepSession(
                id = sid,
                startEpochMs = start,
                stopEpochMs = stop,
                rating = rating,
                note = comment,
                wakeups = wakes
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
