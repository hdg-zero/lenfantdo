/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hdgdev.lenfantdo.domain.model.SleepSession

@Entity(
    tableName = "sleep"
)
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "sid")
    val id: Long = 0,

    @ColumnInfo(name = "start_date")
    val startDate: Long,

    @ColumnInfo(name = "stop_date")
    val stopDate: Long,

    @ColumnInfo(name = "rating")
    val rating: Long = 0,

    @ColumnInfo(name = "comment")
    val comment: String = "",

    @ColumnInfo(name = "wakes")
    val wakes: Int = 0,

    @ColumnInfo(name = "health_connect_id", defaultValue = "''")
    val healthConnectId: String = "",

    @ColumnInfo(name = "health_connect_version", defaultValue = "0")
    val healthConnectVersion: Long = 0,

    @ColumnInfo(name = "health_connect_synced_version", defaultValue = "-1")
    val healthConnectSyncedVersion: Long = -1
) {
    fun toDomain(): SleepSession = SleepSession(
        id = id,
        startEpochMs = startDate,
        stopEpochMs = stopDate,
        rating = if (rating in 1..5) rating else null,
        note = comment,
        wakeups = wakes
    )

    companion object {
        fun fromDomain(session: SleepSession): SleepSessionEntity = SleepSessionEntity(
            id = session.id,
            startDate = session.startEpochMs,
            stopDate = session.stopEpochMs,
            rating = session.rating ?: 0L,
            comment = session.note,
            wakes = session.wakeups
        )
    }
}
