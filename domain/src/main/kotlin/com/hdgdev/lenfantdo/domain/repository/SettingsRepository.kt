/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.repository

import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val appTheme: String = "auto",
    val compactView: Boolean = false,
    val bedtimeHour: Int = 22,
    val bedtimeMinute: Int = 0,
    val wakeupHour: Int = 7,
    val wakeupMinute: Int = 0,
    val reminderBedtimeEnabled: Boolean = false,
    val reminderWakeupEnabled: Boolean = false,
    val autoBackupEnabled: Boolean = false,
    val autoBackupPath: String? = null
)

interface SettingsRepository {
    fun observeSettings(): Flow<UserPreferences>
    suspend fun getSettings(): UserPreferences
    suspend fun updateTheme(theme: String)
    suspend fun updateCompactView(compact: Boolean)
    suspend fun updateBedtime(hour: Int, minute: Int)
    suspend fun updateWakeup(hour: Int, minute: Int)
    suspend fun updateReminders(bedtime: Boolean, wakeup: Boolean)
    suspend fun updateAutoBackup(enabled: Boolean, path: String?)
}
