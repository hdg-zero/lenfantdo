/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.privacy

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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confidentialité & Métriques") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
                .padding(16.dp)
        ) {
            // 100% Offline Commitment
            InfoCard(
                title = "Engagement 100% Hors Ligne",
                icon = Icons.Default.WifiOff,
                body = "L'application ne dispose d'aucune permission réseau (la permission INTERNET est retirée du manifeste). Aucune donnée, télémétrie ou identifiant n'est jamais transmis à qui que ce soit. Vos nuits restent strictement confidentielles sur votre appareil."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // No Cloud Backup
            InfoCard(
                title = "Protection contre l'aspiration cloud",
                icon = Icons.Default.Lock,
                body = "La sauvegarde automatique vers les serveurs cloud de Google est expressément désactivée (allowBackup=\"false\"). Seuls les exports locaux que vous déclenchez vous-même permettent de transférer ou sauvegarder vos données."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Math Definitions: Circular Mean
            InfoCard(
                title = "Moyenne circulaire (Heures de sommeil)",
                icon = Icons.Default.Calculate,
                body = "Une moyenne arithmétique ordinaire sur les heures produit une aberration mathématique connue sous le nom de « piège de minuit » (ex : coucher à 23h50 et 00h10 donnerait une moyenne naïve à 12h00 de midi). L'enfant do projette les heures sous forme d'angles sur le cercle unité trigonométrique (24 heures = 360°) pour calculer une moyenne directionnelle exacte (00h00)."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Math Definitions: Coverage & Missing Days
            InfoCard(
                title = "Régularité et jours non renseignés",
                icon = Icons.Default.Info,
                body = "Contrairement aux applications qui attribuent 0 heure de sommeil aux jours oubliés (ce qui fausse dramatiquement les moyennes), L'enfant do calcule la durée moyenne uniquement sur les nuits observées et indique séparément le taux de couverture et le nombre de jours d'absence de données."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deterministic Assistance
            InfoCard(
                title = "Assistance déterministe et explicable",
                icon = Icons.Default.Info,
                body = "L'enfant do n'utilise aucun modèle d'intelligence artificielle opaque, réseau neuronal ou traitement distant. L'assistance repose sur des règles de cohérence explicites (détection des suivis actifs de plus de 14h ou des sessions de moins de 30 minutes) pour vous aider à corriger simplement les oublis."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: ImageVector,
    body: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
