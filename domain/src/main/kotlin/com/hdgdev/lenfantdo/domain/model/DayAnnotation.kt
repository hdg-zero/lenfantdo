/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.domain.model

data class DayAnnotation(
    val dateIso: String,
    val tags: List<String> = emptyList(),
    val note: String = ""
)
