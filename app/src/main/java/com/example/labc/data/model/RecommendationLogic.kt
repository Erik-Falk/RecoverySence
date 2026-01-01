package com.example.labc.data.model

import com.example.labc.data.model.TrainingDay
import kotlin.math.ceil
import kotlin.math.max

fun computeRecommendation(trainingDays: List<TrainingDay>): TrainingRecommendation? {
    val daysWithScore = trainingDays
        .filter { it.trainingScore != null }
        .sortedBy { it.date }

    if (daysWithScore.isEmpty()) {
        return null // ingen data
    }

    // Senaste 7 och 28 dagar
    val last7 = daysWithScore.takeLast(7)
    val last28 = daysWithScore.takeLast(28)

    fun avgScore(list: List<TrainingDay>): Double =
        list.mapNotNull { it.trainingScore }.average()

    val acuteAvg = avgScore(last7)
    val chronicAvg = if (last28.size >= 7) avgScore(last28) else acuteAvg

    val ratio = if (chronicAvg > 0.0) acuteAvg / chronicAvg else 1.0

    val hardThreshold = max(chronicAvg * 1.1, 50.0)
    val hardDays7 = last7.count { (it.trainingScore ?: 0.0) >= hardThreshold }

    val lastDay = last7.last()
    val lastDayScore = lastDay.trainingScore ?: 0.0

    // Få pass totalt → ta det lugnt och samla data
    if (daysWithScore.size < 3) {
        return TrainingRecommendation(
            intensity = SessionIntensity.EASY,
            title = "Lugnt uppstarts-pass",
            explanation = "Du har ännu inte så många pass loggade. Börja med ett lugnt pass så att appen kan lära sig din normala belastning."
        )
    }

    // Många hårda dagar + tydligt hög belastning
    if (hardDays7 >= 3 && ratio >= 1.3) {
        return TrainingRecommendation(
            intensity = SessionIntensity.REST,
            title = "Rekommenderad vila",
            explanation = "Du har haft $hardDays7 hårda pass den senaste veckan och veckans belastning är högre än din normala nivå. En vilodag minskar risken för överbelastning."
        )
    }

    // Hög belastning, men inte extrem
    if (hardDays7 >= 2 || ratio in 1.1..1.3) {
        return TrainingRecommendation(
            intensity = SessionIntensity.EASY,
            title = "Lätt återhämtningspass",
            explanation = "Belastningen den senaste veckan ligger över din normala nivå. Ett lugnt pass (promenad, lätt jogg, teknik) hjälper dig att återhämta dig utan att helt avstå från aktivitet."
        )
    }

    // Stabil belastning
    if (ratio in 0.8..1.1 && hardDays7 <= 1) {
        // Om senaste passet var väldigt hårt – styr ändå mot lätt/medium
        return if (lastDayScore >= hardThreshold) {
            TrainingRecommendation(
                intensity = SessionIntensity.EASY,
                title = "Lätt pass efter hård dag",
                explanation = "Ditt senaste pass var tufft. Även om belastningen totalt sett är stabil är det smart att följa upp med ett lättare pass."
            )
        } else {
            TrainingRecommendation(
                intensity = SessionIntensity.MODERATE,
                title = "Normalt träningspass",
                explanation = "Din belastning den senaste veckan ligger nära din normala nivå och du har inte gjort många hårda pass. Ett pass med medelintensitet är rimligt."
            )
        }
    }

    // Underbelastning: du har tränat mindre än du brukar
    if (ratio < 0.8 && chronicAvg > 0.0) {
        // Men om senaste passet var hårt, gå inte direkt på ännu ett hårt
        return if (lastDayScore >= hardThreshold) {
            TrainingRecommendation(
                intensity = SessionIntensity.MODERATE,
                title = "Medelintensivt pass",
                explanation = "Du har tränat mindre än din normala nivå, men ditt senaste pass var tufft. Ett pass med medelintensitet är en bra kompromiss."
            )
        } else {
            TrainingRecommendation(
                intensity = SessionIntensity.HARD,
                title = "Dags för ett hårdare pass",
                explanation = "Din belastning den senaste veckan har varit lägre än vad du brukar ligga på. Kroppen borde klara ett tuffare pass, förutsatt att du känner dig pigg."
            )
        }
    }

    // Fallback – om inget annat träffade
    return TrainingRecommendation(
        intensity = SessionIntensity.MODERATE,
        title = "Normalt träningspass",
        explanation = "Din belastning ser varken extremt hög eller låg ut. Ett pass med medelintensitet är ett säkert val."
    )
}
