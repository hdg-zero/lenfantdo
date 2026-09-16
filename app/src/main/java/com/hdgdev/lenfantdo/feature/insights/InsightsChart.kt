/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdgdev.lenfantdo.ui.theme.WarmAmberTertiary
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ModernSleepBarChart(
    bars: List<DailyChartBar>,
    meanDurationHours: Double,
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) return

    // Selected bar index for interactive inspection
    var selectedIndex by remember(bars) {
        // Default to the last tracked day, or last day
        val lastTrackedIndex = bars.indexOfLast { it.isTracked }
        mutableStateOf(if (lastTrackedIndex >= 0) lastTrackedIndex else bars.lastIndex)
    }

    // Animation progress for smooth entry and period changes
    val animProgress = remember(bars) { Animatable(0f) }
    LaunchedEffect(bars) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val selectedBar = bars.getOrNull(selectedIndex)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rythme quotidien de sommeil",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Touchez une barre pour examiner les détails",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Mini legend / indicator for 8h target
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(WarmAmberTertiary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Obj. 8h",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Day Inspector Card
            AnimatedContent(
                targetState = selectedBar,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "bar_inspection_card"
            ) { bar ->
                if (bar != null) {
                    DayInspectionPill(
                        bar = bar,
                        meanDurationHours = meanDurationHours
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Canvas
            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            val selectedGlowColor = MaterialTheme.colorScheme.primary
            val surfaceColor = MaterialTheme.colorScheme.surface
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            val amberColor = WarmAmberTertiary
            val maxHours = 12f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .semantics {
                        contentDescription = "Graphique interactif des durées quotidiennes de sommeil. " +
                                (selectedBar?.let { "Jour sélectionné : ${it.date}, durée : ${it.formattedDuration}" } ?: "")
                    }
                    .pointerInput(bars) {
                        detectTapGestures { offset ->
                            val count = bars.size.coerceAtLeast(1)
                            val slotWidth = size.width / count
                            val tappedIndex = (offset.x / slotWidth).toInt().coerceIn(0, bars.lastIndex)
                            selectedIndex = tappedIndex
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barCount = bars.size.coerceAtLeast(1)
                    val availableWidthPerBar = canvasWidth / barCount
                    val barWidth = (availableWidthPerBar * 0.58f).coerceIn(6f, 26f)

                    // 8 Hours Reference line (dashed amber)
                    val y8h = canvasHeight * (1f - (8f / maxHours))
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    drawLine(
                        color = amberColor.copy(alpha = 0.5f),
                        start = Offset(0f, y8h),
                        end = Offset(canvasWidth, y8h),
                        strokeWidth = 1.5f,
                        pathEffect = dashPathEffect
                    )

                    // Mean reference line if in bounds
                    if (meanDurationHours in 1.0..11.5) {
                        val yMean = canvasHeight * (1f - (meanDurationHours.toFloat() / maxHours))
                        val meanDashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        drawLine(
                            color = primaryColor.copy(alpha = 0.35f),
                            start = Offset(0f, yMean),
                            end = Offset(canvasWidth, yMean),
                            strokeWidth = 1.2f,
                            pathEffect = meanDashEffect
                        )
                    }

                    bars.forEachIndexed { index, bar ->
                        val centerX = (index + 0.5f) * availableWidthPerBar
                        val isSelected = index == selectedIndex
                        val currentBarWidth = if (isSelected) barWidth * 1.15f else barWidth

                        val clampedHours = bar.durationHours.toFloat().coerceIn(0f, maxHours)
                        val animatedBarHeight = (clampedHours / maxHours) * canvasHeight * animProgress.value

                        val left = centerX - (currentBarWidth / 2f)
                        val top = canvasHeight - animatedBarHeight

                        if (bar.isTracked && animatedBarHeight > 2f) {
                            val barBrush = if (isSelected) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        tertiaryColor,
                                        primaryColor
                                    ),
                                    startY = top,
                                    endY = canvasHeight
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        primaryColor,
                                        primaryColor.copy(alpha = 0.65f)
                                    ),
                                    startY = top,
                                    endY = canvasHeight
                                )
                            }

                            // Active bar capsule
                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(left, top),
                                size = Size(currentBarWidth, animatedBarHeight),
                                cornerRadius = CornerRadius(currentBarWidth / 2f, currentBarWidth / 2f)
                            )

                            // Selected highlight halo
                            if (isSelected) {
                                drawCircle(
                                    color = tertiaryColor,
                                    radius = currentBarWidth * 0.45f,
                                    center = Offset(centerX, top + currentBarWidth * 0.45f)
                                )
                            }
                        } else {
                            // Missing or untracked day capsule
                            val stubHeight = 4f
                            drawRoundRect(
                                color = if (isSelected) primaryColor.copy(alpha = 0.6f) else trackColor,
                                topLeft = Offset(left, canvasHeight - stubHeight),
                                size = Size(currentBarWidth, stubHeight),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Day Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val step = when {
                    bars.size <= 7 -> 1
                    bars.size <= 14 -> 2
                    bars.size <= 21 -> 3
                    else -> 5
                }

                bars.forEachIndexed { index, bar ->
                    val isSelected = index == selectedIndex
                    val shouldShowLabel = (index % step == 0) || (index == bars.lastIndex) || isSelected

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                selectedIndex = index
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (shouldShowLabel) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = bar.dayOfWeekShort.take(1),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${bar.dayOfMonth}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayInspectionPill(
    bar: DailyChartBar,
    meanDurationHours: Double
) {
    val fullDateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH) }
    val formattedDate = remember(bar.date) {
        bar.date.format(fullDateFormatter).replaceFirstChar { it.uppercase() }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (bar.isTracked && bar.startTimeFormatted != null && bar.stopTimeFormatted != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${bar.startTimeFormatted} → ${bar.stopTimeFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (bar.rating != null && bar.rating > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = WarmAmberTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${bar.rating}/5",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (!bar.isTracked) {
                    Text(
                        text = "Aucun sommeil enregistré",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Duration and delta tag
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = bar.formattedDuration,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (bar.isTracked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (bar.isTracked && meanDurationHours > 0.1) {
                    val deltaMin = bar.deltaFromMeanMinutes
                    val sign = if (deltaMin >= 0) "+" else ""
                    val deltaColor = if (deltaMin >= 0) MaterialTheme.colorScheme.primary else WarmAmberTertiary
                    Text(
                        text = "$sign$deltaMin min vs moy.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = deltaColor
                    )
                }
            }
        }
    }
}
