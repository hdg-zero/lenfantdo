/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hdgdev.lenfantdo.domain.model.ActiveTracking

@Entity(
    tableName = "active_tracking"
)
data class ActiveTrackingEntity(
    @PrimaryKey
    @ColumnInfo(name = "slot")
    val slot: Int = 1,

    @ColumnInfo(name = "start_epoch_ms")
    val startEpochMs: Long
) {
    fun toDomain(): ActiveTracking = ActiveTracking(startEpochMs = startEpochMs)

    companion object {
        fun fromDomain(active: ActiveTracking): ActiveTrackingEntity = ActiveTrackingEntity(
            slot = 1,
            startEpochMs = active.startEpochMs
        )
    }
}
