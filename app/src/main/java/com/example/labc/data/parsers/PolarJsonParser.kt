package com.example.labc.data.json

import com.example.labc.data.model.HeartRateSample
import org.json.JSONObject


data class PolarWorkout(
    val date: String,                      // t.ex. "2024-11-16"
    val samples: List<HeartRateSample>
)

object PolarJsonParser {

    fun parse(json: String): PolarWorkout {
        val root = JSONObject(json)

        // Datum hämtar vi från startTime: "2024-11-16T08:33:12.000"
        val startTimeStr = root.getString("startTime")
        val date = startTimeStr.substring(0, 10) // "YYYY-MM-DD"

        val exercises = root.getJSONArray("exercises")
        if (exercises.length() == 0) {
            return PolarWorkout(date = date, samples = emptyList())
        }

        val firstExercise = exercises.getJSONObject(0)

        // samples -> heartRate -> array
        val samplesObject = firstExercise.getJSONObject("samples")
        val hrArray = samplesObject.getJSONArray("heartRate")

        val samples = mutableListOf<HeartRateSample>()

        for (i in 0 until hrArray.length()) {
            val sampleObj = hrArray.getJSONObject(i)

            // vissa rader saknar "value" → hoppa över dem
            if (!sampleObj.has("value")) continue

            val dateTimeStr = sampleObj.getString("dateTime") // "2024-11-16T08:33:13.000"
            val value = sampleObj.getInt("value")

            val timestamp = parseDateTimeToMillis(dateTimeStr)

            samples.add(
                HeartRateSample(
                    timestamp = timestamp,
                    heartRate = value
                )
            )
        }

        return PolarWorkout(
            date = date,
            samples = samples
        )
    }

    /**
     * Konverterar "2024-11-16T08:33:13.000" till epoch millis.
     */
    private fun parseDateTimeToMillis(dateTimeStr: String): Long {
        return try {
            // Formatet i din JSON: 2024-11-16T08:33:13.000
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US)

            // Välj tidszon – t.ex. lokal tid:
            sdf.timeZone = java.util.TimeZone.getDefault()
            // (eller TimeZone.getTimeZone("UTC") om du hellre vill ha allt som UTC)

            val date = sdf.parse(dateTimeStr)
            date?.time ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
}
