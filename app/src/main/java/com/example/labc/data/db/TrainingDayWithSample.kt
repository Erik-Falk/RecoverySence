package com.example.labc.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class TrainingDayWithSamples(
    @Embedded val session: TrainingSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val samples: List<HeartRateSampleEntity>
)