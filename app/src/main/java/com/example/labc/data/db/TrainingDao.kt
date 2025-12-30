package com.example.labc.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TrainingDao {

    @Transaction
    @Query("SELECT * FROM training_sessions")
    suspend fun getAllSessionsWithSamples(): List<TrainingDayWithSamples>

    @Insert
    suspend fun insertSession(session: TrainingSessionEntity): Long

    @Insert
    suspend fun insertSamples(samples: List<HeartRateSampleEntity>)

    // Hjälpfunktion: spara pass + samples i ett svep
    @Transaction
    suspend fun insertSessionWithSamples(
        session: TrainingSessionEntity,
        samples: List<HeartRateSampleEntity>
    ) {
        val sessionId = insertSession(session)
        val samplesWithSessionId = samples.map { it.copy(sessionId = sessionId) }
        insertSamples(samplesWithSessionId)
    }
}
