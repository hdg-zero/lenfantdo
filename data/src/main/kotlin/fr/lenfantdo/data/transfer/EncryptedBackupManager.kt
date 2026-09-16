/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.data.transfer

import fr.lenfantdo.domain.model.SleepSession
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptedBackupManager {
    private val MAGIC_HEADER = "LENFANTDO_ENC01\n".toByteArray(StandardCharsets.UTF_8)
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 65536
    private const val KEY_LENGTH_BITS = 256

    /**
     * Encrypts the provided sleep sessions into an AES-256-GCM encrypted byte array using PBKDF2.
     */
    fun createEncryptedBackup(sessions: List<SleepSession>, passphrase: CharArray): ByteArray {
        require(passphrase.isNotEmpty()) { "Passphrase cannot be empty" }

        // 1. Serialize sessions to JSON
        val rootJson = JSONObject().apply {
            put("version", 1)
            put("app", "fr.lenfantdo")
            put("exportedAt", System.currentTimeMillis())

            val sessionsArray = JSONArray()
            for (session in sessions) {
                val sessionJson = JSONObject().apply {
                    put("id", session.id)
                    put("startEpochMs", session.startEpochMs)
                    put("stopEpochMs", session.stopEpochMs)
                    if (session.rating != null) {
                        put("rating", session.rating)
                    }
                    put("note", session.note)
                    put("wakeups", session.wakeups)
                }
                sessionsArray.put(sessionJson)
            }
            put("sessions", sessionsArray)
        }

        val plaintextBytes = rootJson.toString().toByteArray(StandardCharsets.UTF_8)

        // 2. Generate random Salt and IV
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

        // 3. Derive key
        val keySpec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKeyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        // 4. Encrypt with AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val ciphertext = cipher.doFinal(plaintextBytes)

        // 5. Output format: MAGIC (16 bytes) + SALT (16 bytes) + IV (12 bytes) + CIPHERTEXT
        val output = ByteArrayOutputStream()
        output.write(MAGIC_HEADER)
        output.write(salt)
        output.write(iv)
        output.write(ciphertext)
        return output.toByteArray()
    }

    /**
     * Decrypts an encrypted backup byte array, verifying authentication and extracting [SleepSession]s.
     * Throws [IllegalArgumentException] or [javax.crypto.AEADBadTagException] if password is wrong or corrupted.
     */
    fun restoreEncryptedBackup(encryptedBytes: ByteArray, passphrase: CharArray): List<SleepSession> {
        val minLength = MAGIC_HEADER.size + SALT_LENGTH + IV_LENGTH + 16
        require(encryptedBytes.size >= minLength) { "File is too small or corrupted" }

        val input = ByteArrayInputStream(encryptedBytes)

        // 1. Verify Magic
        val header = ByteArray(MAGIC_HEADER.size)
        input.read(header)
        if (!header.contentEquals(MAGIC_HEADER)) {
            throw IllegalArgumentException("Invalid backup format or unrecognised header")
        }

        // 2. Read Salt and IV
        val salt = ByteArray(SALT_LENGTH)
        input.read(salt)
        val iv = ByteArray(IV_LENGTH)
        input.read(iv)

        // 3. Read Ciphertext
        val ciphertext = input.readBytes()

        // 4. Derive key
        val keySpec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKeyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        // 5. Decrypt with AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        val decryptedBytes = cipher.doFinal(ciphertext)

        // 6. Parse JSON
        val jsonString = String(decryptedBytes, StandardCharsets.UTF_8)
        val rootJson = JSONObject(jsonString)
        val sessionsArray = rootJson.optJSONArray("sessions") ?: JSONArray()

        val result = mutableListOf<SleepSession>()
        for (i in 0 until sessionsArray.length()) {
            val s = sessionsArray.getJSONObject(i)
            val id = s.optLong("id", 0L)
            val startMs = s.getLong("startEpochMs")
            val stopMs = s.getLong("stopEpochMs")
            val rating = if (s.has("rating") && !s.isNull("rating")) s.getLong("rating") else null
            val note = s.optString("note", "")
            val wakeups = s.optInt("wakeups", 0)

            result.add(
                SleepSession(
                    id = id,
                    startEpochMs = startMs,
                    stopEpochMs = stopMs,
                    rating = rating,
                    note = note,
                    wakeups = wakeups
                )
            )
        }

        return result
    }
}
