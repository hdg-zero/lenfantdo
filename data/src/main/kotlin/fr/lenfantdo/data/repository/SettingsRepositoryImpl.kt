/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.lenfantdo.domain.repository.SettingsRepository
import fr.lenfantdo.domain.repository.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        val KEY_APP_THEME = stringPreferencesKey("app_theme")
        val KEY_COMPACT_VIEW = booleanPreferencesKey("compact_view")
        val KEY_BEDTIME_HOUR = intPreferencesKey("bedtime_hour")
        val KEY_BEDTIME_MINUTE = intPreferencesKey("bedtime_minute")
        val KEY_WAKEUP_HOUR = intPreferencesKey("wakeup_hour")
        val KEY_WAKEUP_MINUTE = intPreferencesKey("wakeup_minute")
        val KEY_REMINDER_BEDTIME = booleanPreferencesKey("reminder_bedtime")
        val KEY_REMINDER_WAKEUP = booleanPreferencesKey("reminder_wakeup")
        val KEY_AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val KEY_AUTO_BACKUP_PATH = stringPreferencesKey("auto_backup_path")
    }

    override fun observeSettings(): Flow<UserPreferences> {
        return dataStore.data.map { prefs ->
            UserPreferences(
                appTheme = prefs[KEY_APP_THEME] ?: "auto",
                compactView = prefs[KEY_COMPACT_VIEW] ?: false,
                bedtimeHour = prefs[KEY_BEDTIME_HOUR] ?: 22,
                bedtimeMinute = prefs[KEY_BEDTIME_MINUTE] ?: 0,
                wakeupHour = prefs[KEY_WAKEUP_HOUR] ?: 7,
                wakeupMinute = prefs[KEY_WAKEUP_MINUTE] ?: 0,
                reminderBedtimeEnabled = prefs[KEY_REMINDER_BEDTIME] ?: false,
                reminderWakeupEnabled = prefs[KEY_REMINDER_WAKEUP] ?: false,
                autoBackupEnabled = prefs[KEY_AUTO_BACKUP] ?: false,
                autoBackupPath = prefs[KEY_AUTO_BACKUP_PATH]
            )
        }
    }

    override suspend fun getSettings(): UserPreferences {
        return observeSettings().first()
    }

    override suspend fun updateTheme(theme: String) {
        dataStore.edit { it[KEY_APP_THEME] = theme }
    }

    override suspend fun updateCompactView(compact: Boolean) {
        dataStore.edit { it[KEY_COMPACT_VIEW] = compact }
    }

    override suspend fun updateBedtime(hour: Int, minute: Int) {
        dataStore.edit {
            it[KEY_BEDTIME_HOUR] = hour
            it[KEY_BEDTIME_MINUTE] = minute
        }
    }

    override suspend fun updateWakeup(hour: Int, minute: Int) {
        dataStore.edit {
            it[KEY_WAKEUP_HOUR] = hour
            it[KEY_WAKEUP_MINUTE] = minute
        }
    }

    override suspend fun updateReminders(bedtime: Boolean, wakeup: Boolean) {
        dataStore.edit {
            it[KEY_REMINDER_BEDTIME] = bedtime
            it[KEY_REMINDER_WAKEUP] = wakeup
        }
    }

    override suspend fun updateAutoBackup(enabled: Boolean, path: String?) {
        dataStore.edit {
            it[KEY_AUTO_BACKUP] = enabled
            if (path != null) {
                it[KEY_AUTO_BACKUP_PATH] = path
            } else {
                it.remove(KEY_AUTO_BACKUP_PATH)
            }
        }
    }
}
