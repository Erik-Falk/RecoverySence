package com.example.labc.data.model

enum class SessionIntensity {
    REST,
    EASY,
    MODERATE,
    HARD
}

data class TrainingRecommendation(
    val intensity: SessionIntensity,
    val title: String,
    val explanation: String
)
