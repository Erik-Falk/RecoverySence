package com.example.labc.data.model

data class TrainingDay(
    val date: String,                // tex "2025-12-10"
    val samples: List<HeartRateSample>,
    val trainingScore: Double? = null,
    val riskLevel: RiskLevel? = null
)

enum class RiskLevel {
    GREEN, YELLOW, RED
}