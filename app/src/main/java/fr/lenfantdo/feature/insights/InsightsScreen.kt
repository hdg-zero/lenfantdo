/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.feature.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.lenfantdo.ui.component.EmptyState
import fr.lenfantdo.ui.component.MetricCard
import fr.lenfantdo.ui.component.PeriodSelector
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
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Tendances",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Statistiques civiles et moyennes circulaires",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Period filter chips
        PeriodSelector(
            selectedPeriod = state.selectedPeriod,
            onPeriodSelected = { viewModel.selectPeriod(it) }
        )

        if (!state.hasEnoughData || state.analytics == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "Données insuffisantes",
                    description = "Enregistrez vos premières nuits pour calculer vos moyennes et observer vos tendances.",
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
                // Key metrics cards
                val meanDurationMs = analytics.durationSummaryMs.mean
                val meanHours = (meanDurationMs / (1000 * 3600)).toLong()
                val meanMins = ((meanDurationMs % (1000 * 3600)) / (1000 * 60)).toLong()
                val durationText = "${meanHours} h ${meanMins} min"

                val stdDevMinutes = (analytics.durationSummaryMs.sampleStandardDeviation / (1000 * 60)).roundToInt()
                val stdDevText = "Écart-type : ± $stdDevMinutes min"

                val bedtimeTime = analytics.bedtimeCircular.meanTime
                val bedtimeText = bedtimeTime?.let {
                    String.format(Locale.FRENCH, "%02d h %02d", it.hour, it.minute)
                } ?: "--"

                val wakeTime = analytics.wakeTimeCircular.meanTime
                val wakeTimeText = wakeTime?.let {
                    String.format(Locale.FRENCH, "%02d h %02d", it.hour, it.minute)
                } ?: "--"

                val covRatioPct = (analytics.coverage.coverageRatio * 100).roundToInt()
                val covText = "$covRatioPct % (${analytics.coverage.trackedDaysCount}/${analytics.coverage.totalCalendarDays} nuits)"
                val covSubtitle = if (analytics.coverage.missingDaysCount > 0) {
                    "${analytics.coverage.missingDaysCount} nuit(s) non renseignée(s)"
                } else "Couverture complète"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Durée moyenne",
                        value = durationText,
                        subtitle = stdDevText,
                        icon = Icons.Default.AccessTime,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Couverture",
                        value = covText,
                        subtitle = covSubtitle,
                        icon = Icons.Default.CheckCircleOutline,
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
                        subtitle = "Moyenne circulaire sans biais",
                        icon = Icons.Default.Bedtime,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Réveil moyen",
                        value = wakeTimeText,
                        subtitle = "Moyenne circulaire civile",
                        icon = Icons.Default.WbSunny,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bar chart of daily sleep durations
                Card(
                    shape = RoundedCornerShape(18.dp),
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
                        Text(
                            text = "Durée quotidienne du sommeil",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Axe vertical : 0 à 12 heures",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val primaryColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        val chartBars = state.chartBars

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .semantics {
                                    contentDescription = "Graphique en bâtons des durées quotidiennes de sommeil sur la période sélectionnée."
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val maxHours = 12f

                                val barCount = chartBars.size.coerceAtLeast(1)
                                val availableWidthPerBar = canvasWidth / barCount
                                val barWidth = (availableWidthPerBar * 0.6f).coerceAtLeast(6f).coerceAtMost(32f)

                                // Draw baseline and 8h reference line
                                val y8h = canvasHeight * (1f - (8f / maxHours))
                                drawLine(
                                    color = trackColor,
                                    start = Offset(0f, y8h),
                                    end = Offset(canvasWidth, y8h),
                                    strokeWidth = 2f
                                )

                                chartBars.forEachIndexed { index, bar ->
                                    val centerX = (index + 0.5f) * availableWidthPerBar
                                    val clampedHours = bar.durationHours.toFloat().coerceIn(0f, maxHours)
                                    val barHeight = (clampedHours / maxHours) * canvasHeight

                                    val left = centerX - (barWidth / 2f)
                                    val top = canvasHeight - barHeight

                                    if (bar.isTracked && barHeight > 2f) {
                                        drawRoundRect(
                                            color = primaryColor,
                                            topLeft = Offset(left, top),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                                        )
                                    } else {
                                        // Missing / untracked day stub
                                        drawRoundRect(
                                            color = trackColor,
                                            topLeft = Offset(left, canvasHeight - 4f),
                                            size = Size(barWidth, 4f),
                                            cornerRadius = CornerRadius(2f, 2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Accessible TalkBack Data Table
                Card(
                    shape = RoundedCornerShape(18.dp),
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
                        Text(
                            text = "Relevé détaillé de la période",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tableau textuel accessible récapitulant chaque date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH) }

                        state.chartBars.reversed().forEach { bar ->
                            val formattedDate = bar.date.format(dateFormatter).replaceFirstChar { it.uppercase() }
                            val rowA11y = "$formattedDate : ${bar.formattedDuration}"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .semantics { contentDescription = rowA11y },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = bar.formattedDuration,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (bar.isTracked) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (bar.isTracked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
