package com.example.labc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.labc.data.model.TrainingDay
import com.example.labc.data.model.computeRecommendation

@Composable
fun RecommendationScreen(
    trainingDays: List<TrainingDay>
) {
    val colors = MaterialTheme.colorScheme
    val recommendation = computeRecommendation(trainingDays)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Rekommendationer",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (recommendation == null) {
            Text(
                text = "Importera några träningspass först för att få rekommendationer.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBackground
            )
            return
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = colors.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = recommendation.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // (valfritt) Visa några siffror du kan prata om:
        // t.ex. senaste veckans medel, antal hårda dagar osv
        Text(
            text = "Obs: Rekommendationen är ett stöd, inte en medicinsk bedömning. Känn efter själv också.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
