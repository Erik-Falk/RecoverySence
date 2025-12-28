package com.example.labc.data

import android.content.Context
import com.example.labc.data.csv.PolarCsvParser
import com.example.labc.data.model.TrainingDay


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

            // TODO: här kan du gruppera efter datum och räkna trainingScore
            listOf(
                TrainingDay(
                    date = "2025-12-01",
                    samples = samples
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}