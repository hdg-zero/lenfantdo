/*
 * Copyright 2026 Miklos Vajna
 *
 * SPDX-License-Identifier: MIT
 */

package hu.vmiklos.plees_tracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for BackupDestination: JSON (de)serialization of the local folder backup destination,
 * ensuring legacy drive destinations are safely ignored.
 */
class BackupDestinationUnitTest {

    @Test
    fun testLocalFolderRoundTrip() {
        val original = listOf<BackupDestination>(
            BackupDestination.LocalFolder("content://com.android.externalstorage/tree/primary")
        )
        val restored = BackupDestination.listFromJson(BackupDestination.listToJson(original))
        assertEquals(original, restored)
    }

    @Test
    fun testDriveEntriesAreIgnored() {
        val json = """[
            {"type":"drive","email":"user@example.com","frequency":"daily"},
            {"type":"folder","path":"content://valid"}
        ]"""
        val restored = BackupDestination.listFromJson(json)
        assertEquals(
            listOf(BackupDestination.LocalFolder("content://valid")),
            restored
        )
    }

    @Test
    fun testEmptyListSerializesToEmptyJsonArray() {
        assertEquals("[]", BackupDestination.listToJson(emptyList()))
    }

    @Test
    fun testPathWithJsonSpecialCharactersRoundTrips() {
        // Folder URIs can contain characters that must be escaped in JSON.
        val original = listOf<BackupDestination>(
            BackupDestination.LocalFolder("""content://tree/a"b\c/üñîç""")
        )
        val restored = BackupDestination.listFromJson(BackupDestination.listToJson(original))
        assertEquals(original, restored)
    }

    @Test
    fun testEmptyJsonArrayParsesToEmptyList() {
        assertTrue(BackupDestination.listFromJson("[]").isEmpty())
    }

    @Test
    fun testMalformedJsonReturnsEmptyList() {
        assertTrue(BackupDestination.listFromJson("not json at all").isEmpty())
        assertTrue(BackupDestination.listFromJson("").isEmpty())
        assertTrue(BackupDestination.listFromJson("{}").isEmpty())
    }

    @Test
    fun testUnknownTypeIsSkipped() {
        val json = """[{"type":"ftp","host":"example.com"}]"""
        assertTrue(BackupDestination.listFromJson(json).isEmpty())
    }

    @Test
    fun testFolderWithoutPathIsSkipped() {
        val json = """[{"type":"folder"}]"""
        assertTrue(BackupDestination.listFromJson(json).isEmpty())
    }

    @Test
    fun testMigrationAutoBackupOffYieldsEmpty() {
        val result = BackupDestination.fromLegacyPreferences(
            autoBackup = false,
            folderPath = "content://x"
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun testMigrationFolder() {
        val result = BackupDestination.fromLegacyPreferences(
            autoBackup = true,
            folderPath = "content://primary/backup"
        )
        assertEquals(listOf(BackupDestination.LocalFolder("content://primary/backup")), result)
    }

    @Test
    fun testMigrationBlankPathYieldsEmpty() {
        val result = BackupDestination.fromLegacyPreferences(
            autoBackup = true,
            folderPath = ""
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun testMigrationNullPathYieldsEmpty() {
        val result = BackupDestination.fromLegacyPreferences(
            autoBackup = true,
            folderPath = null
        )
        assertTrue(result.isEmpty())
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
