/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hdgdev.lenfantdo.data.local.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Query("SELECT * FROM sleep ORDER BY start_date DESC")
    fun observeAll(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep ORDER BY start_date DESC")
    suspend fun getAll(): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep WHERE sid = :id LIMIT 1")
    suspend fun getById(id: Long): SleepSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SleepSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SleepSessionEntity>)

    @Update
    suspend fun update(entity: SleepSessionEntity)

    @Delete
    suspend fun delete(entity: SleepSessionEntity)

    @Query("DELETE FROM sleep WHERE sid = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sleep")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM sleep")
    suspend fun count(): Int
}
