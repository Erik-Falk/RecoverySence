package com.example.labc.data.json

import com.example.labc.data.model.HeartRateSample
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class PolarWorkout(
    val date: String,
    val samples: List<HeartRateSample>
)

object PolarJsonParser {

    fun parse(json: String): PolarWorkout {
        val root = JSONObject(json)

        val startTimeStr = root.getString("startTime")
        val date = startTimeStr.substring(0, 10)
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

    private fun parseHeartRateSamples(
        exercise: JSONObject,
        startTimeMillis: Long
    ): List<HeartRateSample> {

        val out = mutableListOf<HeartRateSample>()

        val samplesObj = exercise.optJSONObject("samples") ?: return emptyList()

        if (samplesObj.has("heartRate")) {
            val node = samplesObj.get("heartRate")
            if (node is JSONArray) {
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
                val hrSamples = node.getJSONArray("samples")
                out.addAll(parseTimestampMsArray(hrSamples, startTimeMillis))
                return out
            }
        }

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

    private fun parseDateTimeToMillis(dateTimeStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
            sdf.timeZone = TimeZone.getDefault()
            val date = sdf.parse(dateTimeStr)
            date?.time ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
}
