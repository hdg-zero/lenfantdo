/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.tracking

import android.content.Context
import fr.lenfantdo.data.local.AppDatabase
import fr.lenfantdo.data.repository.SleepRepositoryImpl
import fr.lenfantdo.domain.model.ActiveTracking
import fr.lenfantdo.domain.model.SleepSession
import fr.lenfantdo.domain.repository.SleepRepository
import fr.lenfantdo.domain.usecase.CancelTrackingUseCase
import fr.lenfantdo.domain.usecase.StartTrackingUseCase
import fr.lenfantdo.domain.usecase.StopTrackingUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TrackingManager private constructor(
    private val context: Context,
    private val sleepRepository: SleepRepository,
    private val notificationManager: TrackingNotificationManager
) {
    private val startTrackingUseCase = StartTrackingUseCase(sleepRepository)
    private val stopTrackingUseCase = StopTrackingUseCase(sleepRepository)
    private val cancelTrackingUseCase = CancelTrackingUseCase(sleepRepository)

    fun observeActiveTracking(): Flow<ActiveTracking?> =
        sleepRepository.observeActiveTracking()

    suspend fun getActiveTracking(): ActiveTracking? =
        sleepRepository.getActiveTracking()

    suspend fun start(startEpochMs: Long = System.currentTimeMillis()): Result<Unit> {
        val result = startTrackingUseCase(startEpochMs)
        if (result.isSuccess) {
            notificationManager.showTrackingNotification(startEpochMs)
        }
        return result
    }

    suspend fun stop(
        stopEpochMs: Long = System.currentTimeMillis(),
        rating: Long? = null,
        note: String = "",
        wakeups: Int = 0
    ): Result<SleepSession> {
        val result = stopTrackingUseCase(stopEpochMs, rating, note, wakeups)
        if (result.isSuccess) {
            notificationManager.cancelNotification()
        }
        return result
    }

    suspend fun cancel(): Result<Unit> {
        val result = cancelTrackingUseCase()
        if (result.isSuccess) {
            notificationManager.cancelNotification()
        }
        return result
    }

    fun restoreNotificationIfActive() {
        CoroutineScope(Dispatchers.IO).launch {
            val active = getActiveTracking()
            if (active != null) {
                notificationManager.showTrackingNotification(active.startEpochMs)
            } else {
                notificationManager.cancelNotification()
            }
        }
    }

    companion object {
        @Volatile
        private var instance: TrackingManager? = null

        fun getInstance(context: Context): TrackingManager {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val appContext = context.applicationContext
                    val db = AppDatabase.build(appContext)
                    val repo = SleepRepositoryImpl(db)
                    val notif = TrackingNotificationManager(appContext)
                    TrackingManager(appContext, repo, notif).also { instance = it }
                }
            }
        }
    }
}
