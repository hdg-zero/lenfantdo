/*
 * Copyright 2023 Miklos Vajna
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.platform

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.hdgdev.lenfantdo.MainActivity
import com.hdgdev.lenfantdo.R

/**
 * Provides a home screen widget that opens MainActivity and toggles between started/stopped
 * sleep tracking.
 */
class ToggleWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        Log.d(TAG, "ToggleWidget.onUpdate")
        if (context == null || appWidgetManager == null || appWidgetIds == null) {
            return
        }

        for (appWidgetId in appWidgetIds) {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("startStop", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, flags
            )
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout_toggle)
            remoteViews.setOnClickPendingIntent(R.id.widget_toggle, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    companion object {
        private const val TAG = "ToggleWidget"
    }
}
