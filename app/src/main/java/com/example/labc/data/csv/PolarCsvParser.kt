package com.example.labc.data.csv

import com.example.labc.data.model.HeartRateSample

//TODO: Skriv om filen så den passar polars CSV. Just nu är ett exempel

object PolarCsvParser {
    /**
     * lines: alla rader från en CSV-fil (inkl header).
     * Returnerar en lista av HeartRateSample.
     */
    fun parse(lines: List<String>): List<HeartRateSample> {
        if (lines.isEmpty()) return emptyList()

        val result = mutableListOf<HeartRateSample>()

        // hoppa över header
        for ((index, line) in lines.withIndex()) {
            if (index == 0) continue
            if (line.isBlank()) continue

            val parts = line.split(",", ";") // stöd för både , och ;
            if (parts.size < 2) continue

            val timeStr = parts[0].trim()
            val hrStr = parts[1].trim()

            val hr = hrStr.toIntOrNull() ?: continue
            val timestamp = parseTimeToMillis(timeStr)

            result.add(HeartRateSample(timestamp, hr))
        }

        return result
    }

    private fun parseTimeToMillis(timeStr: String): Long {
        // Ex: om timeStr är "0", "1", "2" sekunder från start:
        return timeStr.toLongOrNull()?.times(1000L) ?: 0L
    }
}
