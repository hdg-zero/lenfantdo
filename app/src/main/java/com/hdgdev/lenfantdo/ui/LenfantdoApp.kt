/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hdgdev.lenfantdo.feature.detail.SessionDetailScreen
import com.hdgdev.lenfantdo.feature.home.HomeScreen
import com.hdgdev.lenfantdo.feature.insights.InsightsScreen
import com.hdgdev.lenfantdo.feature.journal.JournalScreen
import com.hdgdev.lenfantdo.feature.privacy.PrivacyScreen
import com.hdgdev.lenfantdo.feature.review.SessionReviewScreen
import com.hdgdev.lenfantdo.feature.review.SessionReviewViewModel
import com.hdgdev.lenfantdo.feature.settings.SettingsScreen
import com.hdgdev.lenfantdo.feature.settings.SettingsViewModel
import com.hdgdev.lenfantdo.ui.navigation.BottomNavItems
import com.hdgdev.lenfantdo.ui.navigation.Screen
import com.hdgdev.lenfantdo.ui.theme.LenfantdoTheme

@Composable
fun LenfantdoApp() {
    val settingsViewModel: SettingsViewModel = viewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val isDarkTheme = when (settingsState.preferences.appTheme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    LenfantdoTheme(darkTheme = isDarkTheme) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Shared ReviewViewModel instance across transitions
        val reviewViewModel: SessionReviewViewModel = viewModel()

        val showBottomBar = BottomNavItems.any { it.route == currentRoute }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        BottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    val icon = if (isSelected) screen.selectedIcon else screen.unselectedIcon
                                    icon?.let { Icon(it, contentDescription = screen.title) }
                                },
                                label = { Text(screen.title) },
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToReview = { stoppedSession ->
                            reviewViewModel.initFromSession(stoppedSession)
                            navController.navigate(Screen.Review.route)
                        },
                        onNavigateToDetail = { sessionId ->
                            navController.navigate(Screen.Detail.createRoute(sessionId))
                        },
                        onNavigateToManualAdd = {
                            reviewViewModel.initForManualEntry()
                            navController.navigate(Screen.Review.route)
                        }
                    )
                }

                composable(Screen.Journal.route) {
                    JournalScreen(
                        onNavigateToDetail = { sessionId ->
                            navController.navigate(Screen.Detail.createRoute(sessionId))
                        }
                    )
                }

                composable(Screen.Insights.route) {
                    InsightsScreen()
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateToPrivacy = {
                            navController.navigate(Screen.Privacy.route)
                        }
                    )
                }

                composable(Screen.Review.route) {
                    SessionReviewScreen(
                        viewModel = reviewViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                    SessionDetailScreen(
                        sessionId = sessionId,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Privacy.route) {
                    PrivacyScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
