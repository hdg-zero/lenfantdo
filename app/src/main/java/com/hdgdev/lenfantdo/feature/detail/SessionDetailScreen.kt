/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hdgdev.lenfantdo.ui.component.RatingStars
import com.hdgdev.lenfantdo.ui.theme.WarmAmberTertiary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: SessionDetailViewModel = viewModel()
) {
    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    val state by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val zoneId = remember { ZoneId.systemDefault() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH) }

    val activeStartMs = if (state.isEditing) state.editStartMs else state.session?.startEpochMs ?: 0L
    val activeStopMs = if (state.isEditing) state.editStopMs else state.session?.stopEpochMs ?: 0L

    val startZDT = remember(activeStartMs) {
        if (activeStartMs > 0) Instant.ofEpochMilli(activeStartMs).atZone(zoneId) else null
    }
    val stopZDT = remember(activeStopMs) {
        if (activeStopMs > 0) Instant.ofEpochMilli(activeStopMs).atZone(zoneId) else null
    }

    val durationHours = state.durationMs / (1000 * 60 * 60)
    val durationMinutes = (state.durationMs / (1000 * 60)) % 60
    val formattedDuration = if (durationHours > 0) "${durationHours} h ${durationMinutes} min" else "${durationMinutes} min"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Modifier la nuit" else "Détail de la nuit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = { viewModel.cancelEditing() }) {
                            Icon(Icons.Default.Close, contentDescription = "Annuler les modifications")
                        }
                        IconButton(onClick = { viewModel.saveChanges() }) {
                            Icon(Icons.Default.Check, contentDescription = "Enregistrer")
                        }
                    } else {
                        IconButton(onClick = { viewModel.startEditing() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Duration highlight card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Durée totale",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (stopZDT != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stopZDT.format(dateFormatter).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Warnings in edit mode
            AnimatedVisibility(visible = state.isEditing && state.warnings.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    state.warnings.forEach { warning ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(warning.message, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timings card
            Card(
                shape = RoundedCornerShape(16.dp),
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
                        text = "Horaires",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Coucher", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = startZDT?.format(timeFormatter) ?: "--:--",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (state.isEditing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        viewModel.updateTimes(state.editStartMs - 15 * 60 * 1000L, state.editStopMs)
                                    }
                                ) {
                                    Text("-15m", style = MaterialTheme.typography.labelMedium)
                                }
                                IconButton(
                                    onClick = {
                                        val newStart = state.editStartMs + 15 * 60 * 1000L
                                        if (newStart < state.editStopMs) {
                                            viewModel.updateTimes(newStart, state.editStopMs)
                                        }
                                    }
                                ) {
                                    Text("+15m", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Réveil", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = stopZDT?.format(timeFormatter) ?: "--:--",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (state.isEditing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val newStop = state.editStopMs - 15 * 60 * 1000L
                                        if (newStop > state.editStartMs) {
                                            viewModel.updateTimes(state.editStartMs, newStop)
                                        }
                                    }
                                ) {
                                    Text("-15m", style = MaterialTheme.typography.labelMedium)
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.updateTimes(state.editStartMs, state.editStopMs + 15 * 60 * 1000L)
                                    }
                                ) {
                                    Text("+15m", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rating & Wakeups
            Card(
                shape = RoundedCornerShape(16.dp),
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
                        text = "Qualité ressentie",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.isEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (i in 1..5) {
                                IconButton(
                                    onClick = {
                                        val newRating = if (state.editRating == i.toLong()) null else i.toLong()
                                        viewModel.updateRating(newRating)
                                    }
                                ) {
                                    val isSelected = state.editRating != null && i <= state.editRating!!
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = "Noter $i étoiles",
                                        tint = if (isSelected) WarmAmberTertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        val rating = state.session?.rating
                        if (rating != null && rating in 1..5) {
                            RatingStars(rating = rating.toInt())
                        } else {
                            Text("Aucune note attribuée", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Réveils nocturnes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val wakeupsCount = if (state.isEditing) state.editWakeups else state.session?.wakeups ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$wakeupsCount réveil${if (wakeupsCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        if (state.isEditing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.updateWakeups(state.editWakeups - 1) },
                                    enabled = state.editWakeups > 0
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Moins de réveils")
                                }
                                IconButton(
                                    onClick = { viewModel.updateWakeups(state.editWakeups + 1) }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus de réveils")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes
            if (state.isEditing) {
                OutlinedTextField(
                    value = state.editNote,
                    onValueChange = { viewModel.updateNote(it) },
                    label = { Text("Commentaire ou note") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.saveChanges() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enregistrer les modifications", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
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
                            text = "Notes & observations",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val note = state.session?.note
                        if (!note.isNullOrBlank()) {
                            Text(text = note, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("Aucun commentaire", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer cette nuit ?") },
            text = { Text("Cette action retirera définitivement cette session de votre historique.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteSession(onDeleted = onNavigateBack)
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
