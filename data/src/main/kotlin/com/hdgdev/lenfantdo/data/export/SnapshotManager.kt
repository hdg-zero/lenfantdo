/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.data.export

import com.hdgdev.lenfantdo.domain.model.SleepSession
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class SnapshotManager {

    fun serializeToCsv(sessions: List<SleepSession>, outputStream: OutputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

        fun writeRecord(line: String) {
            writer.write(line)
            writer.write("\n")
            val bytes = (line + "\n").toByteArray(StandardCharsets.UTF_8)
            digest.update(bytes)
        }

        writeRecord("sid,start,stop,rating,comment,wakes")

        for (session in sessions.sortedBy { it.startEpochMs }) {
            val escapedComment = escapeCsv(session.note)
            val line = "${session.id},${session.startEpochMs},${session.stopEpochMs},${session.rating ?: 0},$escapedComment,${session.wakeups}"
            writeRecord(line)
        }

        writer.flush()
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun escapeCsv(text: String): String {
        if (text.isEmpty()) return ""
        val containsSpecial = text.contains(',') || text.contains('\"') || text.contains('\n') || text.contains('\r')
        return if (containsSpecial) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }

    fun computeSha256(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data)
            .joinToString("") { "%02x".format(it) }
}
