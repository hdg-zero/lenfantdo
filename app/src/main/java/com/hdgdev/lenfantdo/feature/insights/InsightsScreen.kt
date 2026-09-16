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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hdgdev.lenfantdo.ui.component.EmptyState
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // Poetic and informative header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tendances",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Régularité civile et moyennes sans biais",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.totalRecordedSessions > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${state.totalRecordedSessions} nuits",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Modern animated period selector
        PeriodSelector(
            selectedPeriod = state.selectedPeriod,
            onPeriodSelected = { viewModel.selectPeriod(it) }
        )

        // Smooth transition when switching periods
        AnimatedContent(
            targetState = state.selectedPeriod,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
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
                        description = "Enregistrez vos premières nuits pour observer vos tendances et régularités de sommeil.",
                        icon = Icons.Default.BarChart
                    )
                }
            } else {
                val analytics = state.analytics!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 1. Hero Sleep Health & Regularity Card
                    HeroRegularityCard(
                        regularityScore = state.regularityScore,
                        regularityLabel = state.regularityLabel,
                        totalHoursSlept = state.totalHoursSlept,
                        trackedDays = analytics.coverage.trackedDaysCount,
                        totalCalendarDays = analytics.coverage.totalCalendarDays
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Interactive and Animated Sleep Bar Chart
                    val meanDurationMs = analytics.durationSummaryMs.mean
                    val meanDurationHours = meanDurationMs / (1000.0 * 3600.0)

                    ModernSleepBarChart(
                        bars = state.chartBars,
                        meanDurationHours = meanDurationHours
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. 2x2 Metric Cards Grid
                    val meanHours = (meanDurationMs / (1000 * 3600)).toLong()
                    val meanMins = ((meanDurationMs % (1000 * 3600)) / (1000 * 60)).toLong()
                    val durationText = "${meanHours}h ${meanMins}min"

                    val stdDevMinutes = (analytics.durationSummaryMs.sampleStandardDeviation / (1000 * 60)).roundToInt()
                    val stdDevText = "Écart-type : ± $stdDevMinutes min"

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
                    val covSubtitle = "${analytics.coverage.trackedDaysCount}/${analytics.coverage.totalCalendarDays} nuits suivies"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Durée moyenne",
                            value = durationText,
                            subtitle = stdDevText,
                            icon = Icons.Default.AccessTime,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconBackground = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Couverture",
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
                            title = "Coucher moyen",
                            value = bedtimeText,
                            subtitle = "Moyenne circulaire",
                            icon = Icons.Default.Bedtime,
                            iconTint = WarmAmberTertiary,
                            iconBackground = WarmAmberTertiary.copy(alpha = 0.2f),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Réveil moyen",
                            value = wakeTimeText,
                            subtitle = "Moyenne civile",
                            icon = Icons.Default.WbSunny,
                            iconTint = Color(0xFFFBBF24),
                            iconBackground = Color(0xFFFBBF24).copy(alpha = 0.2f),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 4. Weekday vs Weekend Comparison
                    if (state.weekdayMeanHours != null && state.weekendMeanHours != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        WeekdayWeekendCard(
                            weekdayHours = state.weekdayMeanHours!!,
                            weekendHours = state.weekendMeanHours!!,
                            deltaMinutes = state.weekdayWeekendDeltaMinutes ?: 0
                        )
                    }

                    // 5. Ratings and nocturnal wakeups if tracked
                    if (analytics.averageRating != null || analytics.averageWakeups > 0.0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        SleepQualityCard(
                            averageRating = analytics.averageRating,
                            averageWakeups = analytics.averageWakeups
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 6. Detailed Accessible History Log
                    DetailedHistoryCard(bars = state.chartBars)

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun HeroRegularityCard(
    regularityScore: Int?,
    regularityLabel: String?,
    totalHoursSlept: Double,
    trackedDays: Long,
    totalCalendarDays: Long
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Régularité du rythme",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Cohérence horaire sur la période",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (regularityScore != null && regularityLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "$regularityScore % • $regularityLabel",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar for regularity
            if (regularityScore != null) {
                val progress = (regularityScore / 100f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quick summary footer
            val totalH = totalHoursSlept.toInt()
            val totalM = ((totalHoursSlept - totalH) * 60).roundToInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total cumulé : ${totalH}h ${totalM}min",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$trackedDays nuits sur $totalCalendarDays",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeekdayWeekendCard(
    weekdayHours: Double,
    weekendHours: Double,
    deltaMinutes: Int
) {
    val weekdayH = weekdayHours.toInt()
    val weekdayM = ((weekdayHours - weekdayH) * 60).roundToInt()
    val weekendH = weekendHours.toInt()
    val weekendM = ((weekendHours - weekendH) * 60).roundToInt()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rythme Semaine vs Week-end",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val deltaText = when {
                    deltaMinutes > 15 -> "+$deltaMinutes min en week-end"
                    deltaMinutes < -15 -> "$deltaMinutes min en week-end"
                    else -> "Rythme stable (± 15 min)"
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = deltaText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Weekday
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "En semaine (Lun - Ven)",
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

                // Weekend
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "En week-end (Sam - Dim)",
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
private fun SleepQualityCard(
    averageRating: Double?,
    averageWakeups: Double
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
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
                            .size(34.dp)
                            .background(WarmAmberTertiary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = WarmAmberTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ressenti moyen",
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
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Réveils nocturnes",
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
private fun DetailedHistoryCard(
    bars: List<DailyChartBar>
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Relevé détaillé nuit par nuit",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${bars.size} nuits récapitulées • Accessible TalkBack",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Réduire le tableau" else "Développer le tableau",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH) }

                    bars.reversed().forEach { bar ->
                        val formattedDate = remember(bar.date) {
                            bar.date.format(dateFormatter).replaceFirstChar { it.uppercase() }
                        }
                        val rowA11y = "$formattedDate : ${bar.formattedDuration}" +
                                if (bar.startTimeFormatted != null && bar.stopTimeFormatted != null) {
                                    ", couché ${bar.startTimeFormatted}, levé ${bar.stopTimeFormatted}"
                                } else ""

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (!bar.isTracked) {
                                    Text(
                                        text = "Non renseigné",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${bar.rating}/5",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}
