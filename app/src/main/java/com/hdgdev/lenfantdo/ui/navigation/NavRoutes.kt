/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    object Home : Screen("home", "Accueil", Icons.Filled.Bedtime, Icons.Outlined.Bedtime)
    object Journal : Screen("journal", "Journal", Icons.AutoMirrored.Filled.ListAlt, Icons.AutoMirrored.Outlined.ListAlt)
    object Insights : Screen("insights", "Tendances", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    object Settings : Screen("settings", "Paramètres", Icons.Filled.Settings, Icons.Outlined.Settings)
    object Review : Screen("review", "Bilan du réveil")
    object Privacy : Screen("privacy", "Confidentialité & Métriques")
    object Detail : Screen("detail/{sessionId}", "Détail de session") {
        fun createRoute(sessionId: Long) = "detail/$sessionId"
    }
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Journal,
    Screen.Insights,
    Screen.Settings
)
