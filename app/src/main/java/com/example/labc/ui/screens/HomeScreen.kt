package com.example.labc.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.labc.data.model.RiskLevel
import com.example.labc.data.model.TrainingDay
import com.example.labc.ui.TrainingUiState
import com.example.labc.ui.TrainingViewModel

@Composable
fun HomeScreen(
    viewModel: TrainingViewModel,
    state: TrainingUiState
) {
    // Filväljare för Polar JSON-filer
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFromUri(uri)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Knapp för att importera pass
        Button(
            onClick = {
                filePickerLauncher.launch(
                    arrayOf("application/json", "text/plain")
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Importera Polar-pass (JSON)")
        }

        when {
            state.isLoading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Fel: ${state.errorMessage}")
                }
            }

            else -> {
                TrainingDayList(trainingDays = state.trainingDays)
            }
        }
    }
}

@Composable
fun TrainingDayList(trainingDays: List<TrainingDay>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(trainingDays) { day ->

            // Text för risknivå
            val riskText = when (day.riskLevel) {
                RiskLevel.GREEN -> "Grön (stabil belastning)"
                RiskLevel.YELLOW -> "Gul (ökad belastning)"
                RiskLevel.RED -> "Röd (hög belastning)"
                null -> "Okänd"
            }

            // Färg för risknivå
            val riskColor = when (day.riskLevel) {
                RiskLevel.GREEN -> Color(0xFF2E7D32) // grön
                RiskLevel.YELLOW -> Color(0xFFF9A825) // gul
                RiskLevel.RED -> Color(0xFFC62828) // röd
                null -> Color.Unspecified
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Datum: ${day.date}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(text = "Antal mätpunkter: ${day.samples.size}")
                    Text(
                        text = "Träningsscore: ${
                            "%.1f".format(day.trainingScore ?: 0.0)
                        }"
                    )
                    Text(
                        text = "Risknivå: $riskText",
                        color = riskColor
                    )
                }
            }
        }
    }
}
