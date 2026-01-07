package com.example.labc.data.json

import com.example.labc.data.model.HeartRateSample
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class PolarWorkout(
    val date: String,              // t.ex. "2024-11-14"
    val samples: List<HeartRateSample>
)

object PolarJsonParser {

    fun parse(json: String): PolarWorkout {
        val root = JSONObject(json)

        // Ex: "2024-11-14T15:29:00.000"
        val startTimeStr = root.getString("startTime")
        val date = startTimeStr.substring(0, 10)              // "YYYY-MM-DD"
        val startTimeMillis = parseDateTimeToMillis(startTimeStr)

        val exercises = root.optJSONArray("exercises")
        if (exercises == null || exercises.length() == 0) {
            return PolarWorkout(date = date, samples = emptyList())
        }

        val firstExercise = exercises.getJSONObject(0)

        val samples = parseHeartRateSamples(firstExercise, startTimeMillis)

        return PolarWorkout(
            date = date,
            samples = samples
        )
    }

    /**
     * Försöker tolka heart-rate samples i ett par olika Polar-format:
     *
     * 1) samples.heartRate = [ { "dateTime": "...", "value": 80 }, ... ]
     * 2) samples.heartRate (eller heart_rate) = {
     *        "samples": [
     *           { "timestamp_ms": "12345", "value": 80 }, ...
     *        ]
     *    }
     */
    private fun parseHeartRateSamples(
        exercise: JSONObject,
        startTimeMillis: Long
    ): List<HeartRateSample> {

        val out = mutableListOf<HeartRateSample>()

        val samplesObj = exercise.optJSONObject("samples") ?: return emptyList()

        // --- Format 1: som i din JSON, samples.heartRate är en array med dateTime + value ---
        if (samplesObj.has("heartRate")) {
            val node = samplesObj.get("heartRate")
            if (node is JSONArray) {
                // Exakt som i JSON-utdraget du skickade
                for (i in 0 until node.length()) {
                    val obj = node.getJSONObject(i)
                    if (!obj.has("value")) continue

                    val dateTimeStr = obj.getString("dateTime")
                    val value = obj.getInt("value")
                    val ts = parseDateTimeToMillis(dateTimeStr)

                    out.add(
                        HeartRateSample(
                            timestamp = ts,
                            heartRate = value
                        )
                    )
                }
                return out
            } else if (node is JSONObject && node.has("samples")) {
                // Format 2-variant: samples.heartRate.samples[ {timestamp_ms, value}, ... ]
                val hrSamples = node.getJSONArray("samples")
                out.addAll(parseTimestampMsArray(hrSamples, startTimeMillis))
                return out
            }
        }

        // --- Alternativ nyckel: "heart_rate" (snake case) ---
        if (samplesObj.has("heart_rate")) {
            val node = samplesObj.get("heart_rate")
            if (node is JSONArray) {
                // ovanligt, men hanteras för säkerhets skull
                for (i in 0 until node.length()) {
                    val obj = node.getJSONObject(i)
                    if (!obj.has("value")) continue
                    val dateTimeStr = obj.getString("dateTime")
                    val value = obj.getInt("value")
                    val ts = parseDateTimeToMillis(dateTimeStr)
                    out.add(HeartRateSample(timestamp = ts, heartRate = value))
                }
            } else if (node is JSONObject && node.has("samples")) {
                val hrSamples = node.getJSONArray("samples")
                out.addAll(parseTimestampMsArray(hrSamples, startTimeMillis))
            }
        }

        return out
    }

    /**
     * Hjälpmetod för formatet där varje sample ser ut ungefär så här:
     * { "timestamp_ms": "12345", "value": 80 }
     *
     * timestamp_ms kan vara:
     *  - offset från start (ms)
     *  - eller ett absolut epoch-värde
     */
    private fun parseTimestampMsArray(
        arr: JSONArray,
        startTimeMillis: Long
    ): List<HeartRateSample> {
        val result = mutableListOf<HeartRateSample>()

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (!obj.has("value")) continue

            val v = obj.getInt("value")

            val tsAny = obj.get("timestamp_ms")
            val raw = when (tsAny) {
                is Number -> tsAny.toLong()
                is String -> tsAny.toLongOrNull()
                else -> null
            } ?: continue

            // Gissning: om värdet är "litet" är det troligen offset från startTime
            val ts = if (raw < 1_000_000_000_000L) {
                startTimeMillis + raw
            } else {
                raw
            }

            result.add(
                HeartRateSample(
                    timestamp = ts,
                    heartRate = v
                )
            )
        }

        return result
    }

    /**
     * Konverterar "2024-11-14T15:29:01.000" till epoch millis.
     */
    private fun parseDateTimeToMillis(dateTimeStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
            // Polar-exporterna du visat är “lokal tid” (inte 'Z'), så vi använder device-tidszon:
            sdf.timeZone = TimeZone.getDefault()
            val date = sdf.parse(dateTimeStr)
            date?.time ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
}
