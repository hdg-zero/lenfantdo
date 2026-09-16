/*
 * Copyright 2023 Miklos Vajna
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.platform

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.util.Log
import androidx.annotation.RequiresApi
import com.hdgdev.lenfantdo.MainActivity
import com.hdgdev.lenfantdo.tracking.TrackingManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Quick settings tile to toggle sleep tracking directly from system panel.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class TileService : android.service.quicksettings.TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onStartListening() {
        refreshTile()
    }

    override fun onTileAdded() {
        refreshTile()
    }

    private fun refreshTile() {
        serviceScope.launch {
            try {
                val trackingManager = TrackingManager.getInstance(applicationContext)
                val active = withContext(Dispatchers.IO) {
                    trackingManager.getActiveTracking() != null
                }
                qsTile?.let { tile ->
                    tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    tile.updateTile()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "refreshTile error", e)
            }
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        serviceScope.launch {
            try {
                val trackingManager = TrackingManager.getInstance(applicationContext)
                withContext(Dispatchers.IO) {
                    val active = trackingManager.getActiveTracking()
                    if (active != null) {
                        trackingManager.stop()
                    } else {
                        trackingManager.start()
                    }
                }
                qsTile?.let { tile ->
                    val active = withContext(Dispatchers.IO) {
                        trackingManager.getActiveTracking() != null
                    }
                    tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    tile.updateTile()
                }

                val intent = Intent(this@TileService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        this@TileService,
                        0,
                        intent,
                        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    startActivityAndCollapse(pendingIntent)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Tile onClick error", e)
            }
        }
    }

    companion object {
        private const val TAG = "TileService"
    }
}
