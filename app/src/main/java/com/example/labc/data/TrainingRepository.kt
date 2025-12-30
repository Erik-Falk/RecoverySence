package com.example.labc.data

import android.content.Context
import com.example.labc.data.csv.PolarCsvParser
import com.example.labc.data.model.TrainingDay
import com.example.labc.data.model.HeartRateSample
import com.example.labc.data.model.RiskLevel


class TrainingRepository(
    private val appContext: Context
) {

    /**
     * Laddar ett exempelpass från en CSV-fil i assets.
     * Namnge filen t.ex. "polar_example.csv" och lägg den i /app/src/main/assets
     */
    fun loadTrainingDaysFromAssets(): List<TrainingDay> {
        return try {
            val assetManager = appContext.assets
            val inputStream = assetManager.open("polar_example.csv")
            val lines = inputStream.bufferedReader().use { it.readLines() }

            val samples = PolarCsvParser.parse(lines)

            val score = calculateTrainingScore(samples)
            val risk = calculateRiskLevel(score)

            listOf(
                TrainingDay(
                    date = "2025-12-01",
                    samples = samples,
                    trainingScore = score,
                    riskLevel = risk
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()

            // Fallback-data så appen inte kraschar om något går snett
            val fallbackSamples = listOf(
                HeartRateSample(timestamp = 0L, heartRate = 80),
                HeartRateSample(timestamp = 1000L, heartRate = 85),
                HeartRateSample(timestamp = 2000L, heartRate = 90)
            )

            val score = calculateTrainingScore(fallbackSamples)
            val risk = calculateRiskLevel(score)

            listOf(
                TrainingDay(
                    date = "fallback",
                    samples = fallbackSamples,
                    trainingScore = score,
                    riskLevel = risk
                )
            )
        }
    }

    private fun calculateTrainingScore(samples: List<HeartRateSample>): Double {
        if (samples.isEmpty()) return 0.0

        // 1. Medelpuls
        val avgHr = samples.map { it.heartRate }.average()

        // 2. Duration i minuter (utifrån första/sista timestamp)
        val minTime = samples.minOf { it.timestamp }
        val maxTime = samples.maxOf { it.timestamp }
        val durationMillis = maxTime - minTime
        val durationMinutes = durationMillis / 1000.0 / 60.0

        if (durationMinutes <= 0) return 0.0

        // 3. Enkelt tränings-score
        return durationMinutes * (avgHr / 100.0)
    }

    private fun calculateRiskLevel(score: Double): RiskLevel {
        return when {
            score < 40.0 -> RiskLevel.GREEN
            score < 80.0 -> RiskLevel.YELLOW
            else -> RiskLevel.RED
        }
    }
}