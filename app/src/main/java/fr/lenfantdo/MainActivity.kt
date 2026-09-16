/*
 * Copyright 2023 Miklos Vajna
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import fr.lenfantdo.tracking.TrackingManager
import fr.lenfantdo.ui.LenfantdoApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleStartStopIntent(intent)

        // Ensure active tracking notification is restored if tracking was ongoing
        TrackingManager.getInstance(applicationContext).restoreNotificationIfActive()

        // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            LenfantdoApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleStartStopIntent(intent)
    }

    private fun handleStartStopIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("startStop", false) == true) {
            intent.removeExtra("startStop")
            lifecycleScope.launch {
                val trackingManager = TrackingManager.getInstance(applicationContext)
                val active = trackingManager.getActiveTracking()
                if (active != null) {
                    trackingManager.stop()
                } else {
                    trackingManager.start()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        TrackingManager.getInstance(applicationContext).restoreNotificationIfActive()
    }
}
