package com.example.labc.data.model

data class HeartRateSample(
    val timestamp: Long,   // t.ex. millisekunder sedan epoch
    val heartRate: Int
)
