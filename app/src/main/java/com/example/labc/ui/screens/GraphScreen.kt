package com.example.labc.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.labc.data.model.TrainingDay
import androidx.compose.ui.text.style.TextAlign

@Composable
fun GraphScreen(
    trainingDays: List<TrainingDay>
) {
    if (trainingDays.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = "Inga träningspass importerade ännu.",
                modifier = Modifier
                    .padding(16.dp)
            )
        }
        return
    }

    // Sortera på datum (string) – funkar ok om formatet är YYYY-MM-DD
    val sortedDays = trainingDays.sortedBy { it.date }
    val scores = sortedDays.map { it.trainingScore ?: 0.0 }

    val maxScore = scores.maxOrNull() ?: 0.0
    val minScore = 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Träningsscore per pass",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (scores.size < 2 || maxScore <= minScore) return@Canvas

                val width = size.width
                val height = size.height

                val xStep = width / (scores.size - 1).coerceAtLeast(1)
                val scoreRange = (maxScore - minScore).takeIf { it > 0 } ?: 1.0

                val path = Path()

                scores.forEachIndexed { index, score ->
                    val x = xStep * index
                    val normalized = ((score - minScore) / scoreRange).toFloat()
                    val y = height - normalized * height

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }

                    // 🔹 Rita en liten punkt för varje pass
                    drawCircle(
                        color = Color.Black,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                }

                // 🔹 Rita själva linjen
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 4f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // En enkel lista med datum + score under grafen
            Column {
                sortedDays.forEach { day ->
                    Text(
                        text = "${day.date}: ${"%.1f".format(day.trainingScore ?: 0.0)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
