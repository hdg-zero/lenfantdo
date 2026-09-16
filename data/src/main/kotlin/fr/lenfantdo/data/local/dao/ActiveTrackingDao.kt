/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.lenfantdo.data.local.entity.ActiveTrackingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveTrackingDao {
    @Query("SELECT * FROM active_tracking WHERE slot = 1 LIMIT 1")
    fun observeActive(): Flow<ActiveTrackingEntity?>

    @Query("SELECT * FROM active_tracking WHERE slot = 1 LIMIT 1")
    suspend fun getActive(): ActiveTrackingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setActive(entity: ActiveTrackingEntity)

    @Query("DELETE FROM active_tracking WHERE slot = 1")
    suspend fun clearActive()
}
