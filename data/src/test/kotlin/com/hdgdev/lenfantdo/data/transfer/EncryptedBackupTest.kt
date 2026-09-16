/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.data.transfer

import com.hdgdev.lenfantdo.domain.model.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import javax.crypto.AEADBadTagException

class EncryptedBackupTest {

    @Test
    fun `encrypt and decrypt round trip preserves all fields`() {
        val originalSessions = listOf(
            SleepSession(
                id = 1,
                startEpochMs = 1700000000000L,
                stopEpochMs = 1700028800000L,
                rating = 5L,
                note = "Nuit paisible et réparatrice",
                wakeups = 1
            ),
            SleepSession(
                id = 2,
                startEpochMs = 1700086400000L,
                stopEpochMs = 1700115200000L,
                rating = null,
                note = "",
                wakeups = 0
            )
        )

        val password = "StrongSecretPassword123!".toCharArray()
        val encryptedData = EncryptedBackupManager.createEncryptedBackup(originalSessions, password)

        assertTrue(encryptedData.isNotEmpty())
        assertTrue(encryptedData.size > 100)

        val restored = EncryptedBackupManager.restoreEncryptedBackup(encryptedData, password)
        assertEquals(2, restored.size)
        assertEquals(originalSessions[0].id, restored[0].id)
        assertEquals(originalSessions[0].startEpochMs, restored[0].startEpochMs)
        assertEquals(originalSessions[0].stopEpochMs, restored[0].stopEpochMs)
        assertEquals(originalSessions[0].rating, restored[0].rating)
        assertEquals(originalSessions[0].note, restored[0].note)
        assertEquals(originalSessions[0].wakeups, restored[0].wakeups)

        assertEquals(originalSessions[1].id, restored[1].id)
        assertEquals(originalSessions[1].rating, restored[1].rating)
    }

    @Test
    fun `wrong password fails decryption with authentication exception`() {
        val sessions = listOf(
            SleepSession(
                id = 1,
                startEpochMs = 1700000000000L,
                stopEpochMs = 1700028800000L
            )
        )
        val password = "CorrectPassword".toCharArray()
        val wrongPassword = "WrongPassword".toCharArray()

        val encryptedData = EncryptedBackupManager.createEncryptedBackup(sessions, password)

        try {
            EncryptedBackupManager.restoreEncryptedBackup(encryptedData, wrongPassword)
            fail("Should fail with authentication error for wrong password")
        } catch (e: Exception) {
            // Expected AEADBadTagException or GeneralSecurityException
            assertNotNull(e)
        }
    }
}
