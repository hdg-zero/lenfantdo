/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.data.export

import com.hdgdev.lenfantdo.domain.model.SleepSession
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvParserAndSnapshotTest {

    private val parser = LegacyCsvParser()
    private val snapshotManager = SnapshotManager()

    @Test
    fun parseStandardCsvWithHeader() {
        val csv = """
            sid,start,stop,rating,comment,wakes
            1,1625105400000,1625121600000,4,"Bonne nuit",1
            2,1625191800000,1625208000000,5,"Parfait",0
        """.trimIndent()

        val result = parser.parse(StringReader(csv))
        assertEquals(2, result.sessions.size)
        assertEquals(0, result.skippedLinesCount)

        val first = result.sessions[0]
        assertEquals(1L, first.id)
        assertEquals(1625105400000L, first.startEpochMs)
        assertEquals(1625121600000L, first.stopEpochMs)
        assertEquals(4L, first.rating)
        assertEquals("Bonne nuit", first.note)
        assertEquals(1, first.wakeups)
    }

    @Test
    fun parseTimestampsInSecondsAutomaticallyNormalizesToMilliseconds() {
        val csv = """
            sid,start,stop,rating,comment
            1,1625105400,1625121600,3,test
        """.trimIndent()

        val result = parser.parse(StringReader(csv))
        assertEquals(1, result.sessions.size)
        assertEquals(1625105400000L, result.sessions[0].startEpochMs)
        assertEquals(1625121600000L, result.sessions[0].stopEpochMs)
    }

    @Test
    fun parseWithBomAndQuotedNewlines() {
        val bomCsv = "\uFEFFsid,start,stop,rating,comment,wakes\n1,100000,200000,4,\"Ligne 1, avec virgule\",0\n"
        val result = parser.parse(StringReader(bomCsv))
        assertEquals(1, result.sessions.size)
        assertEquals("Ligne 1, avec virgule", result.sessions[0].note)
    }

    @Test
    fun parseCorruptedLinesAreSkippedGracefully() {
        val csv = """
            sid,start,stop,rating,comment
            1,corrupted_date,10000,3,bad
            2,100000,200000,4,good
            3,300000,100000,4,inverted_time
            4,400000,500000,5,good_too
        """.trimIndent()

        val result = parser.parse(StringReader(csv))
        assertEquals(2, result.sessions.size)
        assertEquals(2, result.skippedLinesCount)
        assertEquals(2L, result.sessions[0].id)
        assertEquals(4L, result.sessions[1].id)
    }

    @Test
    fun snapshotSerializeAndRoundTrip() {
        val sessions = listOf(
            SleepSession(id = 1, startEpochMs = 1625105400000L, stopEpochMs = 1625121600000L, rating = 4L, note = "Test \"note\", ok", wakeups = 2),
            SleepSession(id = 2, startEpochMs = 1625191800000L, stopEpochMs = 1625208000000L, rating = null, note = "Simple", wakeups = 0)
        )

        val output = ByteArrayOutputStream()
        val checksum = snapshotManager.serializeToCsv(sessions, output)

        assertNotNull(checksum)
        assertTrue(checksum.length == 64) // SHA-256 hex string

        val parsedBack = parser.parse(StringReader(output.toString(StandardCharsets.UTF_8.name())))
        assertEquals(2, parsedBack.sessions.size)
        assertEquals(sessions[0].startEpochMs, parsedBack.sessions[0].startEpochMs)
        assertEquals(sessions[0].note, parsedBack.sessions[0].note)
        assertEquals(sessions[0].rating, parsedBack.sessions[0].rating)
        assertEquals(sessions[1].rating, parsedBack.sessions[1].rating)
    }
}
