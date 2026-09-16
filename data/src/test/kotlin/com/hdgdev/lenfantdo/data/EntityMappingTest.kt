/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.data

import com.hdgdev.lenfantdo.data.local.entity.ActiveTrackingEntity
import com.hdgdev.lenfantdo.data.local.entity.SleepSessionEntity
import com.hdgdev.lenfantdo.domain.model.ActiveTracking
import com.hdgdev.lenfantdo.domain.model.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntityMappingTest {

    @Test
    fun sleepSessionEntityToDomainAndBack() {
        val entity = SleepSessionEntity(
            id = 42L,
            startDate = 1000000L,
            stopDate = 2000000L,
            rating = 4L,
            comment = "Nuit réparatrice",
            wakes = 2,
            healthConnectId = "hc-123",
            healthConnectVersion = 3L,
            healthConnectSyncedVersion = 3L
        )

        val domain = entity.toDomain()
        assertEquals(42L, domain.id)
        assertEquals(1000000L, domain.startEpochMs)
        assertEquals(2000000L, domain.stopEpochMs)
        assertEquals(4L, domain.rating)
        assertEquals("Nuit réparatrice", domain.note)
        assertEquals(2, domain.wakeups)

        val backToEntity = SleepSessionEntity.fromDomain(domain)
        assertEquals(42L, backToEntity.id)
        assertEquals(1000000L, backToEntity.startDate)
        assertEquals(2000000L, backToEntity.stopDate)
        assertEquals(4L, backToEntity.rating)
        assertEquals("Nuit réparatrice", backToEntity.comment)
        assertEquals(2, backToEntity.wakes)
    }

    @Test
    fun invalidOrZeroRatingMapsToNull() {
        val entityZero = SleepSessionEntity(
            id = 1L,
            startDate = 1000L,
            stopDate = 2000L,
            rating = 0L
        )
        assertNull(entityZero.toDomain().rating)

        val entityOutOfRange = SleepSessionEntity(
            id = 2L,
            startDate = 1000L,
            stopDate = 2000L,
            rating = 99L
        )
        assertNull(entityOutOfRange.toDomain().rating)
    }

    @Test
    fun activeTrackingEntityToDomainAndBack() {
        val entity = ActiveTrackingEntity(
            slot = 1,
            startEpochMs = 1700000000000L
        )
        val domain = entity.toDomain()
        assertEquals(1700000000000L, domain.startEpochMs)

        val backToEntity = ActiveTrackingEntity.fromDomain(domain)
        assertEquals(1, backToEntity.slot)
        assertEquals(1700000000000L, backToEntity.startEpochMs)
    }
}
