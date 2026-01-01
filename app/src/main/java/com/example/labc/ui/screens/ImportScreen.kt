package com.example.labc.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.labc.data.model.RiskLevel
import com.example.labc.data.model.TrainingDay
import com.example.labc.ui.TrainingUiState
import com.example.labc.ui.TrainingViewModel

@Composable
fun ImportScreen(
    viewModel: TrainingViewModel,
    state: TrainingUiState
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importFromUri(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = {
                    filePickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Importera Polar-pass (JSON)")
            }
        }

        when {
            state.isLoading -> {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) { CircularProgressIndicator() }
                }
            }

            state.errorMessage != null -> {
                item { Text("Fel: ${state.errorMessage}") }
            }

            else -> {
                items(state.trainingDays) { day ->
                    TrainingDayCard(day)
                }
            }
        }
    }
}

@Composable
private fun TrainingDayCard(day: TrainingDay) {
    val riskText = when (day.riskLevel) {
        RiskLevel.GREEN -> "Grön (stabil belastning)"
        RiskLevel.YELLOW -> "Gul (ökad belastning)"
        RiskLevel.RED -> "Röd (hög belastning)"
        null -> "Okänd"
    }

    val riskColor = when (day.riskLevel) {
        RiskLevel.GREEN -> Color(0xFF2E7D32)
        RiskLevel.YELLOW -> Color(0xFFF9A825)
        RiskLevel.RED -> Color(0xFFC62828)
        null -> Color.Unspecified
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Datum: ${day.date}", style = MaterialTheme.typography.titleMedium)
            Text("Antal mätpunkter: ${day.samples.size}")
            Text("Träningsscore: ${"%.1f".format(day.trainingScore ?: 0.0)}")
            Text("Risknivå: $riskText", color = riskColor)
        }
    }
}
