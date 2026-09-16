/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.feature.home

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import fr.lenfantdo.domain.model.SleepSession
import fr.lenfantdo.ui.component.SleepCard
import fr.lenfantdo.ui.component.TrackingButton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onNavigateToReview: (SleepSession) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToManualAdd: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "L'enfant do",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Journal de sommeil privé et hors ligne",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Assistance warning (e.g. tracking > 14h)
        AnimatedVisibility(visible = state.activeWarning != null) {
            state.activeWarning?.let { warning ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = warning.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Active tracking or idle tracking card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.activeTracking != null) {
                    val active = state.activeTracking!!
                    val startTimeFormatted = remember(active.startEpochMs) {
                        Instant.ofEpochMilli(active.startEpochMs)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                    }

                    Text(
                        text = "Sommeil en cours",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val hours = state.elapsedSeconds / 3600
                    val minutes = (state.elapsedSeconds % 3600) / 60
                    val seconds = state.elapsedSeconds % 60
                    val timerText = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                    Text(
                        text = timerText,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Commencé à $startTimeFormatted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TrackingButton(
                        isTracking = true,
                        onClick = {
                            viewModel.stopTracking(onStopped = onNavigateToReview)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { showCancelDialog = true }
                    ) {
                        Text(
                            text = "Annuler ce suivi",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(
                        text = "Prêt pour la nuit ?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Touchez le bouton ci-dessous au moment de dormir. Votre téléphone peut être verrouillé en toute sérénité.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TrackingButton(
                        isTracking = false,
                        onClick = { viewModel.startTracking() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual record button
        OutlinedButton(
            onClick = onNavigateToManualAdd,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ajouter une nuit manuellement")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Last session section
        if (state.lastSession != null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Dernière nuit enregistrée",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SleepCard(
                    session = state.lastSession!!,
                    onClick = { onNavigateToDetail(state.lastSession!!.id) }
                )
            }
        }
    }

    // Cancel tracking confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Annuler le suivi en cours ?") },
            text = { Text("La session en cours sera abandonnée sans être enregistrée dans l'historique.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelTracking()
                        showCancelDialog = false
                    }
                ) {
                    Text("Oui, annuler", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Poursuivre le suivi")
                }
            }
        )
    }
}
