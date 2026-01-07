package com.example.labc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.labc.ui.TrainingUiState

@Composable
fun HomeScreen(
    state: TrainingUiState,
    onGoToImport: () -> Unit,
    onGoToGraph: () -> Unit,
    onGoToRecommendation: () -> Unit,
    onGoToLive: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Hem", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }

        state.errorMessage?.let {
            Text("Fel: $it", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }

        HomeActionButton(
            title = "Livepuls",
            subtitle = "Se puls i realtid från din sensor",
            icon = Icons.Default.Favorite,
            onClick = onGoToLive
        )

        Spacer(Modifier.height(12.dp))

        HomeActionButton(
            title = "Import",
            subtitle = "Importera Polar-pass (JSON) och se alla dagar",
            icon = Icons.Default.UploadFile,
            onClick = onGoToImport
        )

        Spacer(Modifier.height(12.dp))

        HomeActionButton(
            title = "Grafer",
            subtitle = "Se utveckling över tid (score/risk)",
            icon = Icons.Default.ShowChart,
            onClick = onGoToGraph
        )

        Spacer(Modifier.height(12.dp))

        HomeActionButton(
            title = "Rekommendation",
            subtitle = "Få träningsförslag baserat på belastning",
            icon = Icons.Default.Lightbulb,
            onClick = onGoToRecommendation
        )
    }
}

@Composable
fun HomeActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
