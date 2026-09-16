/*
 * Copyright 2023 Miklos Vajna
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package hu.vmiklos.plees_tracker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.util.Log
import androidx.annotation.RequiresApi
import fr.lenfantdo.tracking.TrackingManager
import kotlinx.coroutines.runBlocking

/**
 * Quick settings tile to toggle sleep tracking directly from system panel.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class TileService : android.service.quicksettings.TileService() {

    override fun onStartListening() {
        refreshTile()
    }

    override fun onTileAdded() {
        refreshTile()
    }

    private fun refreshTile() {
        try {
            val trackingManager = TrackingManager.getInstance(applicationContext)
            runBlocking {
                val active = trackingManager.getActiveTracking() != null
                qsTile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                qsTile.updateTile()
            }
        } catch (e: Exception) {
            Log.e(TAG, "refreshTile error", e)
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        try {
            val trackingManager = TrackingManager.getInstance(applicationContext)
            runBlocking {
                val active = trackingManager.getActiveTracking()
                if (active != null) {
                    trackingManager.stop()
                } else {
                    trackingManager.start()
                }
            }
            refreshTile()

            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val flags = PendingIntent.FLAG_IMMUTABLE
                val activity = PendingIntent.getActivity(this, 0, intent, flags)
                startActivityAndCollapse(activity)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onClick error", e)
        }
    }

    companion object {
        private const val TAG = "TileService"
    }
}
