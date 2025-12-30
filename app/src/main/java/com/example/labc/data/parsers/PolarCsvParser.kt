package com.example.labc.data.parsers

import com.example.labc.data.model.HeartRateSample

//TODO: Skriv om filen så den passar polars CSV. Just nu är ett exempel

object PolarCsvParser {

    fun parse(lines: List<String>): List<HeartRateSample> {
        if (lines.isEmpty()) return emptyList()

        val result = mutableListOf<HeartRateSample>()

        for ((index, line) in lines.withIndex()) {
            if (index == 0) continue  // hoppa header
            if (line.isBlank()) continue

            val parts = line.split(",", ";")
            if (parts.size < 2) continue

            val timeStr = parts[0].trim()
            val hrStr = parts[1].trim()

            val hr = hrStr.toIntOrNull() ?: continue
            val timestamp = parseTimeToMillis(timeStr)

            result.add(HeartRateSample(timestamp = timestamp, heartRate = hr))
        }

        return result
    }

    private fun parseTimeToMillis(timeStr: String): Long {
        // TODO: anpassa efter hur tiden ser ut i din riktiga Polar-CSV
        // T.ex. om det är sekunder från start:
        return timeStr.toLongOrNull()?.times(1000L) ?: 0L
    }
}

