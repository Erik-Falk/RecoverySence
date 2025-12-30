package com.example.labc.data

import android.content.Context
import android.net.Uri
import com.example.labc.data.db.AppDatabase
import com.example.labc.data.db.HeartRateSampleEntity
import com.example.labc.data.db.TrainingDayWithSamples
import com.example.labc.data.db.TrainingSessionEntity
import com.example.labc.data.json.PolarJsonParser
import com.example.labc.data.json.PolarWorkout
import com.example.labc.data.model.HeartRateSample
import com.example.labc.data.model.RiskLevel
import com.example.labc.data.model.TrainingDay

class TrainingRepository(
    appContext: Context
) {

    private val db = AppDatabase.getInstance(appContext)
    private val dao = db.trainingDao()

    // Hämta allt från DB och mappa till dina domänklasser
    suspend fun getAllTrainingDays(): List<TrainingDay> {
        val fromDb: List<TrainingDayWithSamples> = dao.getAllSessionsWithSamples()
        return fromDb.map { it.toDomain() }
    }

    // Importera Polar JSON-fil -> spara i DB
    suspend fun importFromUri(uri: Uri, appContext: Context) {
        val inputStream = appContext.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Kunde inte öppna filen")

        val json = inputStream.bufferedReader().use { it.readText() }

        val workout: PolarWorkout = PolarJsonParser.parse(json)

        val score = calculateTrainingScore(workout.samples)
        val risk = calculateRiskLevel(score)

        val sessionEntity = TrainingSessionEntity(
            date = workout.date,
            trainingScore = score,
            riskLevel = risk.name // "GREEN", "YELLOW", "RED"
        )

        val sampleEntities = workout.samples.map { s ->
            HeartRateSampleEntity(
                sessionId = 0, // sätts i DAO
                timestamp = s.timestamp,
                heartRate = s.heartRate
            )
        }

        dao.insertSessionWithSamples(sessionEntity, sampleEntities)
    }

    // ---- analys-metoder som du redan använder ----

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

// ---- Mapping från DB-modeller till dina domänklasser ----

private fun TrainingDayWithSamples.toDomain(): TrainingDay {
    val risk = when (session.riskLevel) {
        "GREEN" -> RiskLevel.GREEN
        "YELLOW" -> RiskLevel.YELLOW
        "RED" -> RiskLevel.RED
        else -> null
    }

    val samplesDomain = samples.map {
        HeartRateSample(
            timestamp = it.timestamp,
            heartRate = it.heartRate
        )
    }

    return TrainingDay(
        date = session.date,
        samples = samplesDomain,
        trainingScore = session.trainingScore,
        riskLevel = risk
    )
}
