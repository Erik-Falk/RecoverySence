package com.example.labc.data

import android.content.Context
import android.net.Uri
import com.example.labc.data.json.PolarJsonParser
import com.example.labc.data.json.PolarWorkout
import com.example.labc.data.parsers.PolarCsvParser
import com.example.labc.data.model.HeartRateSample
import com.example.labc.data.model.RiskLevel
import com.example.labc.data.model.TrainingDay

class TrainingRepository(
    private val appContext: Context
) {
    // Enkel in-memory-lista för att "spara" pass under appens livslängd
    private val storedDays = mutableListOf<TrainingDay>()

    fun getAllTrainingDays(): List<TrainingDay> = storedDays.toList()

    // Anropas från ViewModel när användaren väljer en CSV-fil
    fun importFromUri(uri: Uri) {
        val inputStream = appContext.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Kunde inte öppna filen")

        val json = inputStream.bufferedReader().use { it.readText() }

        // 🔹 Använd vår nya JSON-parser
        val workout: PolarWorkout = PolarJsonParser.parse(json)

        val score = calculateTrainingScore(workout.samples)
        val risk = calculateRiskLevel(score)

        val newDay = TrainingDay(
            date = workout.date,           // t.ex. "2024-11-16"
            samples = workout.samples,
            trainingScore = score,
            riskLevel = risk
        )

        storedDays.add(newDay)
    }

    // Exempel: läs ett demo-pass från assets (frivilligt)
    fun loadFromAssetsOnce() {
        if (storedDays.isNotEmpty()) return

        try {
            val assetManager = appContext.assets
            val inputStream = assetManager.open("polar_example.csv")
            val lines = inputStream.bufferedReader().use { it.readLines() }

            val samples = PolarCsvParser.parse(lines)
            val score = calculateTrainingScore(samples)
            val risk = calculateRiskLevel(score)

            storedDays.add(
                TrainingDay(
                    date = "Demo (assets)",
                    samples = samples,
                    trainingScore = score,
                    riskLevel = risk
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateTrainingScore(samples: List<HeartRateSample>): Double {
        if (samples.isEmpty()) return 0.0

        val avgHr = samples.map { it.heartRate }.average()
        val minTime = samples.minOf { it.timestamp }
        val maxTime = samples.maxOf { it.timestamp }
        val durationMillis = maxTime - minTime
        val durationMinutes = durationMillis / 1000.0 / 60.0

        if (durationMinutes <= 0) return 0.0

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
