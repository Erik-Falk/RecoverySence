package com.example.labc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.labc.ble.BleConnectionState

@Composable
fun LiveHeartRateScreen(
    liveHeartRate: Int?,
    connectionState: BleConnectionState,
    connectionInfo: String,
    onStartLive: (String?) -> Unit,
    onStopLive: () -> Unit
) {
    var macText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection-status
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "Anslutning",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Status: ${connectionState.name}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = connectionInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Livepuls
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Livepuls",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = liveHeartRate?.let { "$it bpm" } ?: "--",
                    style = MaterialTheme.typography.displaySmall
                )
            }
        }

        // Fält för MAC-adress
        OutlinedTextField(
            value = macText,
            onValueChange = { macText = it },
            label = { Text("MAC-adress (valfritt)") },
            placeholder = { Text("t.ex. A0:9E:1A:C4:45:8C") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Lämna tomt för att söka efter närmaste Polar-sensor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    android.util.Log.d("BleHR", "Start-knapp tryckt i LiveHeartRateScreen")
                    val macOrNull = macText.trim().ifBlank { null }
                    onStartLive(macOrNull)
                }
            ) {
                Text("Starta")
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    android.util.Log.d("BleHR", "Stop-knapp tryckt i LiveHeartRateScreen")
                    onStopLive()
                }
            ) {
                Text("Stoppa")
            }
        }
    }
}
