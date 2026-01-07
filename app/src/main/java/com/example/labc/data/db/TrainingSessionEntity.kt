package com.example.labc.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,           // tex "2024-11-16"
    val trainingScore: Double,
    val riskLevel: String       // "GREEN", "YELLOW", "RED"
)