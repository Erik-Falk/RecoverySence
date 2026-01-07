package com.example.labc.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heart_rate_samples")
data class HeartRateSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val heartRate: Int
)