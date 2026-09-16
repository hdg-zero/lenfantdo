/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hdgdev.lenfantdo.ui.component.EmptyState
import com.hdgdev.lenfantdo.ui.component.GlassCard
import com.hdgdev.lenfantdo.ui.component.GlassPill
import com.hdgdev.lenfantdo.ui.component.MetricCard
import com.hdgdev.lenfantdo.ui.component.PeriodSelector
import com.hdgdev.lenfantdo.ui.theme.TrackingStartGreen
import com.hdgdev.lenfantdo.ui.theme.WarmAmberTertiary
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Ambient Lighting Background Canvas
    val glowIndigo = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val glowAmber = WarmAmberTertiary.copy(alpha = 0.12f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Atmospheric light orbs creating the frosted depth
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-left nocturnal celestial orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowIndigo, Color.Transparent),
                    center = Offset(w * 0.2f, h * 0.15f),
                    radius = w * 0.65f
                )
            )

            // Mid-right warm dream orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowAmber, Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.50f),
                    radius = w * 0.55f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            // Crisp, modern glass header without wordy descriptions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tendances",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (state.totalRecordedSessions > 0) {
                    GlassPill {
                        Text(
                            text = "${state.totalRecordedSessions} nuits",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Glassmorphic period selector
            PeriodSelector(
                selectedPeriod = state.selectedPeriod,
                onPeriodSelected = { viewModel.selectPeriod(it) }
            )

            // Smooth animated content transition
            AnimatedContent(
                targetState = state.selectedPeriod,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "insights_period_transition"
            ) { _ ->
                if (!state.hasEnoughData || state.analytics == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            title = "Données insuffisantes",
                            description = "Enregistrez vos premières nuits pour observer vos tendances.",
                            icon = Icons.Default.BarChart
                        )
                    }
                } else {
                    val analytics = state.analytics!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        // 1. Hero Sleep Regularity Card (Frosted Glass)
                        HeroRegularityGlassCard(
                            regularityScore = state.regularityScore,
                            regularityLabel = state.regularityLabel,
                            totalHoursSlept = state.totalHoursSlept,
                            trackedDays = analytics.coverage.trackedDaysCount,
                            totalCalendarDays = analytics.coverage.totalCalendarDays
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Interactive Sleep Bar Chart
                        val meanDurationMs = analytics.durationSummaryMs.mean
                        val meanDurationHours = meanDurationMs / (1000.0 * 3600.0)

                        ModernSleepBarChart(
                            bars = state.chartBars,
                            meanDurationHours = meanDurationHours
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. 2x2 Glass Metric Cards (Concise, punchy copy)
                        val meanHours = (meanDurationMs / (1000 * 3600)).toLong()
                        val meanMins = ((meanDurationMs % (1000 * 3600)) / (1000 * 60)).toLong()
                        val durationText = "${meanHours}h ${meanMins}min"

                        val stdDevMinutes = (analytics.durationSummaryMs.sampleStandardDeviation / (1000 * 60)).roundToInt()
                        val stdDevText = "± $stdDevMinutes min"

                        val bedtimeTime = analytics.bedtimeCircular.meanTime
                        val bedtimeText = bedtimeTime?.let {
                            String.format(Locale.FRENCH, "%02dh%02d", it.hour, it.minute)
                        } ?: "--"

                        val wakeTime = analytics.wakeTimeCircular.meanTime
                        val wakeTimeText = wakeTime?.let {
                            String.format(Locale.FRENCH, "%02dh%02d", it.hour, it.minute)
                        } ?: "--"

                        val covRatioPct = (analytics.coverage.coverageRatio * 100).roundToInt()
                        val covText = "$covRatioPct %"
                        val covSubtitle = "${analytics.coverage.trackedDaysCount} / ${analytics.coverage.totalCalendarDays} nuits"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard(
                                title = "Durée",
                                value = durationText,
                                subtitle = stdDevText,
                                icon = Icons.Default.AccessTime,
                                iconTint = MaterialTheme.colorScheme.primary,
                                iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Suivi",
                                value = covText,
                                subtitle = covSubtitle,
                                icon = Icons.Default.CheckCircleOutline,
                                iconTint = TrackingStartGreen,
                                iconBackground = TrackingStartGreen.copy(alpha = 0.2f),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard(
                                title = "Coucher",
                                value = bedtimeText,
                                subtitle = "Régulier",
                                icon = Icons.Default.Bedtime,
                                iconTint = WarmAmberTertiary,
                                iconBackground = WarmAmberTertiary.copy(alpha = 0.2f),
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Réveil",
                                value = wakeTimeText,
                                subtitle = "Stable",
                                icon = Icons.Default.WbSunny,
                                iconTint = Color(0xFFFBBF24),
                                iconBackground = Color(0xFFFBBF24).copy(alpha = 0.2f),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // 4. Weekday vs Weekend Comparison
                        if (state.weekdayMeanHours != null && state.weekendMeanHours != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            WeekdayWeekendGlassCard(
                                weekdayHours = state.weekdayMeanHours!!,
                                weekendHours = state.weekendMeanHours!!,
                                deltaMinutes = state.weekdayWeekendDeltaMinutes ?: 0
                            )
                        }

                        // 5. Ratings and nocturnal wakeups if tracked
                        if (analytics.averageRating != null || analytics.averageWakeups > 0.0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            SleepQualityGlassCard(
                                averageRating = analytics.averageRating,
                                averageWakeups = analytics.averageWakeups
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 6. Detailed Log Table in Glass Card
                        DetailedHistoryGlassCard(bars = state.chartBars)

                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroRegularityGlassCard(
    regularityScore: Int?,
    regularityLabel: String?,
    totalHoursSlept: Double,
    trackedDays: Long,
    totalCalendarDays: Long
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Régularité",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (regularityScore != null && regularityLabel != null) {
                    GlassPill {
                        Text(
                            text = "$regularityScore % • $regularityLabel",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (regularityScore != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val progress = (regularityScore / 100f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val totalH = totalHoursSlept.toInt()
            val totalM = ((totalHoursSlept - totalH) * 60).roundToInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cumul : ${totalH}h ${totalM}min",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$trackedDays / $totalCalendarDays nuits",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeekdayWeekendGlassCard(
    weekdayHours: Double,
    weekendHours: Double,
    deltaMinutes: Int
) {
    val weekdayH = weekdayHours.toInt()
    val weekdayM = ((weekdayHours - weekdayH) * 60).roundToInt()
    val weekendH = weekendHours.toInt()
    val weekendM = ((weekendHours - weekendH) * 60).roundToInt()

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Semaine vs Week-end",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val deltaText = when {
                    deltaMinutes > 15 -> "+$deltaMinutes min"
                    deltaMinutes < -15 -> "$deltaMinutes min"
                    else -> "Stable"
                }
                GlassPill {
                    Text(
                        text = deltaText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Semaine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${weekdayH}h ${weekdayM}min",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Week-end",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${weekendH}h ${weekendM}min",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepQualityGlassCard(
    averageRating: Double?,
    averageWakeups: Double
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (averageRating != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(WarmAmberTertiary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = WarmAmberTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ressenti",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.FRENCH, "%.1f / 5", averageRating),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (averageWakeups > 0.0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Réveils",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.FRENCH, "%.1f / nuit", averageWakeups),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedHistoryGlassCard(
    bars: List<DailyChartBar>
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Historique des nuits",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassPill {
                        Text(
                            text = "${bars.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Réduire" else "Développer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(280)) + fadeIn(animationSpec = tween(280)),
                exit = shrinkVertically(animationSpec = tween(240)) + fadeOut(animationSpec = tween(240))
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH) }

                    bars.reversed().forEach { bar ->
                        val formattedDate = remember(bar.date) {
                            bar.date.format(dateFormatter).replaceFirstChar { it.uppercase() }
                        }
                        val rowA11y = "$formattedDate : ${bar.formattedDuration}"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .semantics { contentDescription = rowA11y },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (bar.isTracked && bar.startTimeFormatted != null && bar.stopTimeFormatted != null) {
                                    Text(
                                        text = "${bar.startTimeFormatted} → ${bar.stopTimeFormatted}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (!bar.isTracked) {
                                    Text(
                                        text = "Pas de données",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = bar.formattedDuration,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (bar.isTracked) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (bar.isTracked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (bar.rating != null && bar.rating > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = WarmAmberTertiary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${bar.rating}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                    }
                }
            }
        }
    }
}
