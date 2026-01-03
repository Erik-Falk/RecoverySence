package com.example.labc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiveHeartRateScreen(
    liveHeartRate: Int?,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        Text(
            text = "Livepuls",
            style = MaterialTheme.typography.titleLarge
        )

        // Stor pulsvy
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = colors.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = liveHeartRate?.let { "$it" } ?: "--",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                Text(
                    text = "bpm",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (liveHeartRate == null)
                        "Tryck på Starta för att koppla upp mot sensorn."
                    else
                        "Pulsvärde från ansluten sensor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        // Start / Stop-knappar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartLive,
                modifier = Modifier.weight(1f)
            ) {
                Text("Starta")
            }
            OutlinedButton(
                onClick = onStopLive,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stoppa")
            }
        }
    }
}
