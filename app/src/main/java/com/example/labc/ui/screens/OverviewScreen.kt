package com.example.labc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.labc.data.model.RiskLevel
import com.example.labc.data.model.TrainingDay
import com.example.labc.ui.TrainingViewModel
import androidx.compose.ui.graphics.Color

@Composable
fun OverviewScreen(
    viewModel: TrainingViewModel
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
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
                RiskLevel.GREEN -> Color(0xFF2E7D32)
                RiskLevel.YELLOW -> Color(0xFFF9A825)
                RiskLevel.RED -> Color(0xFFC62828)
                null -> Color.Unspecified
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
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

