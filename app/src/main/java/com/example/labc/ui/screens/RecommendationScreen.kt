package com.example.labc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.labc.data.model.TrainingDay

@Composable
fun RecommendationScreen(
    trainingDays: List<TrainingDay>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Rekommendationer",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Här kan du senare göra analys över senaste 7 dagar vs tidigare
        // och ge grön/gul/röd rekommendation.

        if (trainingDays.isEmpty()) {
            Text("Importera några träningspass först för att få rekommendationer.")
        } else {
            Text("Logik för rekommendationer kommer här senare 👀")
        }
    }
}
