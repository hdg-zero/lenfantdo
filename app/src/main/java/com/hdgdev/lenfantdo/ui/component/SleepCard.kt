/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.ui.theme.WarmAmberTertiary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SleepCard(
    session: SleepSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault(),
    isCompact: Boolean = false
) {
    val startZDT = remember(session.startEpochMs, zoneId) {
        Instant.ofEpochMilli(session.startEpochMs).atZone(zoneId)
    }
    val stopZDT = remember(session.stopEpochMs, zoneId) {
        Instant.ofEpochMilli(session.stopEpochMs).atZone(zoneId)
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH) }

    val formattedDate = remember(stopZDT) {
        stopZDT.format(dateFormatter).replaceFirstChar { it.uppercase() }
    }
    val formattedStart = remember(startZDT) { startZDT.format(timeFormatter) }
    val formattedStop = remember(stopZDT) { stopZDT.format(timeFormatter) }

    val durationHours = session.durationMs / (1000 * 60 * 60)
    val durationMinutes = (session.durationMs / (1000 * 60)) % 60
    val formattedDuration = remember(durationHours, durationMinutes) {
        if (durationHours > 0) "${durationHours} h ${durationMinutes} min" else "${durationMinutes} min"
    }

    val a11ySummary = buildString {
        append("Nuit du $formattedDate. ")
        append("De $formattedStart à $formattedStop, ")
        append("durée $formattedDuration. ")
        session.rating?.let { append("Note : $it étoiles sur 5. ") }
        if (session.wakeups > 0) append("${session.wakeups} réveil${if (session.wakeups > 1) "s" else ""}. ")
        if (session.note.isNotBlank()) append("Note : ${session.note}")
    }

    val cardShape = RoundedCornerShape(if (isCompact) 14.dp else 20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(glassGradient())
            .border(1.dp, glassBorderBrush(), cardShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = a11ySummary }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 10.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formattedDuration,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$formattedStart  →  $formattedStop",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (session.rating != null && session.rating in 1L..5L) {
                    RatingStars(rating = session.rating!!.toInt())
                }
            }

            if (session.wakeups > 0 || session.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (session.wakeups > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${session.wakeups} réveil${if (session.wakeups > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (session.note.isNotBlank()) {
                        Text(
                            text = session.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingStars(
    rating: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (i <= rating) WarmAmberTertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
