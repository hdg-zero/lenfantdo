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
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdgdev.lenfantdo.ui.component.glassBorderBrush
import com.hdgdev.lenfantdo.ui.component.glassGradient
import com.hdgdev.lenfantdo.ui.theme.WarmAmberTertiary
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ModernSleepBarChart(
    bars: List<DailyChartBar>,
    meanDurationHours: Double,
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) return

    var selectedIndex by remember(bars) {
        val lastTrackedIndex = bars.indexOfLast { it.isTracked }
        mutableStateOf(if (lastTrackedIndex >= 0) lastTrackedIndex else bars.lastIndex)
    }

    val animProgress = remember(bars) { Animatable(0f) }
    LaunchedEffect(bars) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val selectedBar = bars.getOrNull(selectedIndex)
    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(glassGradient())
            .border(1.dp, glassBorderBrush(), cardShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: concise title + 8h target glass pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sommeil par nuit",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Compact glass pill for 8h target
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(WarmAmberTertiary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Obj. 8h",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Glassmorphic Day Inspection Pill
            AnimatedContent(
                targetState = selectedBar,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
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

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Canvas
            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            val amberColor = WarmAmberTertiary
            val maxHours = 12f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .semantics {
                        contentDescription = "Graphique des durées de sommeil. " +
                                (selectedBar?.let { "${it.date} : ${it.formattedDuration}" } ?: "")
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
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    drawLine(
                        color = amberColor.copy(alpha = 0.45f),
                        start = Offset(0f, y8h),
                        end = Offset(canvasWidth, y8h),
                        strokeWidth = 1.2f,
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
                                        primaryColor.copy(alpha = 0.60f)
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
                                    color = Color.White.copy(alpha = 0.85f),
                                    radius = currentBarWidth * 0.35f,
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
    val fullDateFormatter = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH) }
    val formattedDate = remember(bar.date) {
        bar.date.format(fullDateFormatter).replaceFirstChar { it.uppercase() }
    }
    val pillShape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(pillShape)
            .background(glassGradient(alphaTop = 0.85f, alphaBottom = 0.60f))
            .border(1.dp, glassBorderBrush(alphaStart = 0.30f, alphaEnd = 0.08f), pillShape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${bar.startTimeFormatted} → ${bar.stopTimeFormatted}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (bar.rating != null && bar.rating > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = WarmAmberTertiary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "${bar.rating}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (!bar.isTracked) {
                    Text(
                        text = "Pas de données",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                        text = "$sign$deltaMin min",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = deltaColor
                    )
                }
            }
        }
    }
}
