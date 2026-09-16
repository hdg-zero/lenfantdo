/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates a frosted glass background gradient tailored for dark and light modes.
 */
@Composable
fun glassGradient(
    alphaTop: Float = 0.72f,
    alphaBottom: Float = 0.45f
): Brush {
    val topColor = MaterialTheme.colorScheme.surface.copy(alpha = alphaTop)
    val bottomColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alphaBottom)
    return Brush.linearGradient(
        colors = listOf(topColor, bottomColor)
    )
}

/**
 * Creates a subtle specular glass border gradient (highlight at top-left, softer at bottom-right).
 */
@Composable
fun glassBorderBrush(
    alphaStart: Float = 0.25f,
    alphaEnd: Float = 0.06f
): Brush {
    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = alphaStart),
            Color.White.copy(alpha = alphaEnd)
        )
    )
}

/**
 * A modern, tactile frosted-glass container with delicate specular refraction border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val bgBrush = glassGradient()
    val borderBrush = glassBorderBrush()

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(width = borderWidth, brush = borderBrush, shape = shape)
    ) {
        Column(content = content)
    }
}

/**
 * A sleek frosted glass pill badge or button.
 */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    onClick: (() -> Unit)? = null,
    borderWidth: Dp = 1.dp,
    content: @Composable RowScope.() -> Unit
) {
    val bgBrush = glassGradient(alphaTop = 0.60f, alphaBottom = 0.35f)
    val borderBrush = glassBorderBrush(alphaStart = 0.22f, alphaEnd = 0.05f)

    val baseModifier = modifier
        .clip(shape)
        .background(bgBrush)
        .border(width = borderWidth, brush = borderBrush, shape = shape)

    val clickableModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else {
        baseModifier
    }

    Row(
        modifier = clickableModifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
