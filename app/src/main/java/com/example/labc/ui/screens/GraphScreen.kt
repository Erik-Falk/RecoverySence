package com.example.labc.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.labc.data.model.TrainingDay
import kotlin.math.ceil
import kotlin.math.min

@Composable
fun GraphScreen(
    trainingDays: List<TrainingDay>
) {
    val colors = MaterialTheme.colorScheme

    if (trainingDays.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Inga träningspass importerade ännu.",
                color = colors.onBackground,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val sortedDays = trainingDays.sortedBy { it.date }
    val windowSize = 7

    var startIndex by remember { mutableStateOf(0) }

    // hoppa alltid till senaste 7 när mängden data förändras
    LaunchedEffect(sortedDays.size) {
        startIndex = (sortedDays.size - windowSize).coerceAtLeast(0)
    }

    val clampedStart = startIndex.coerceIn(0, (sortedDays.size - 1).coerceAtLeast(0))
    val endIndexExclusive = min(clampedStart + windowSize, sortedDays.size)

    val windowDays = sortedDays.subList(clampedStart, endIndexExclusive)
    val scores = windowDays.map { it.trainingScore ?: 0.0 }
    val maxScore = scores.maxOrNull() ?: 0.0

    // gör skalan "rund": 0–10–20–30 osv
    val tickStep = when {
        maxScore <= 50 -> 10.0
        maxScore <= 100 -> 20.0
        maxScore <= 200 -> 50.0
        else -> 100.0
    }
    val topTick = if (maxScore <= 0.0) tickStep else tickStep * ceil(maxScore / tickStep)
    val maxValueForScale = topTick.coerceAtLeast(tickStep) // undvik 0

    var selectedDay by remember { mutableStateOf<TrainingDay?>(null) }

    // Paint för y-axelns text
    val yLabelColorInt = colors.onBackground.copy(alpha = 0.7f).toArgb()
    val yLabelPaint = remember(yLabelColorInt) {
        Paint().apply {
            color = yLabelColorInt
            textSize = 26f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Träningsscore per pass",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Visar ${windowDays.size} pass (${clampedStart + 1}–$endIndexExclusive av ${sortedDays.size}).",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // knappar för att bläddra mellan fönster
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                enabled = clampedStart > 0,
                onClick = {
                    startIndex = (clampedStart - windowSize).coerceAtLeast(0)
                }
            ) {
                Text("Äldre")
            }

            TextButton(
                enabled = endIndexExclusive < sortedDays.size,
                onClick = {
                    startIndex = (clampedStart + windowSize)
                        .coerceAtMost((sortedDays.size - 1).coerceAtLeast(0))
                }
            ) {
                Text("Nyare")
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surfaceVariant
            )
        ) {
            var pointPositions by remember { mutableStateOf<List<Offset>>(emptyList()) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .pointerInput(scores, pointPositions) {
                        detectTapGestures { tapOffset ->
                            val points = pointPositions
                            if (points.isNotEmpty()) {
                                val index = points.indices.minByOrNull { i ->
                                    val p = points[i]
                                    val dx = p.x - tapOffset.x
                                    val dy = p.y - tapOffset.y
                                    dx * dx + dy * dy
                                }
                                index?.let { i ->
                                    selectedDay = windowDays[i]
                                }
                            }
                        }
                    }
            ) {
                if (scores.isEmpty() || maxValueForScale <= 0.0) return@Canvas

                val fullWidth = size.width
                val fullHeight = size.height

                // lämna plats för y-axel till vänster + lite padding
                val leftPadding = 48f
                val topPadding = 8f
                val bottomPadding = 24f

                val chartWidth = fullWidth - leftPadding
                val chartHeight = fullHeight - topPadding - bottomPadding

                val gridColor = Color.White.copy(alpha = 0.25f)
                val lineColor = Color(0xFF4CAF50) // grön linje

                // --- Y-AXEL + horisontella gridlinjer + etiketter ---
                val maxTickInt = topTick.toInt()
                val stepInt = tickStep.toInt().coerceAtLeast(1)

                for (value in 0..maxTickInt step stepInt) {
                    val ratio = value / maxValueForScale.toFloat() // 0–1
                    val y = topPadding + chartHeight * (1f - ratio)

                    // gridlinje
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, y),
                        end = Offset(leftPadding + chartWidth, y),
                        strokeWidth = 1f
                    )

                    // etikett (ritas lite till vänster om y-axeln)
                    drawContext.canvas.nativeCanvas.drawText(
                        value.toString(),
                        leftPadding - 8f,
                        y + (yLabelPaint.textSize / 3f),
                        yLabelPaint
                    )
                }

                // y-axel
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, topPadding),
                    end = Offset(leftPadding, topPadding + chartHeight),
                    strokeWidth = 2f
                )

                // --- Vertikala gridlinjer (en per pass i fönstret) ---
                val xStep = if (scores.size == 1) 0f else chartWidth / (scores.size - 1)
                for (i in scores.indices) {
                    val x = if (scores.size == 1)
                        leftPadding + chartWidth / 2f
                    else
                        leftPadding + xStep * i

                    drawLine(
                        color = gridColor,
                        start = Offset(x, topPadding),
                        end = Offset(x, topPadding + chartHeight),
                        strokeWidth = 1f
                    )
                }

                // --- Punktpositioner ---
                val positions = mutableListOf<Offset>()
                scores.forEachIndexed { index, score ->
                    val x = if (scores.size == 1)
                        leftPadding + chartWidth / 2f
                    else
                        leftPadding + xStep * index

                    val ratio = (score / maxValueForScale).toFloat().coerceIn(0f, 1f)
                    val y = topPadding + chartHeight * (1f - ratio)

                    positions.add(Offset(x, y))
                }
                pointPositions = positions

                // --- grön linje ---
                for (i in 0 until positions.size - 1) {
                    drawLine(
                        color = lineColor,
                        start = positions[i],
                        end = positions[i + 1],
                        strokeWidth = 4f
                    )
                }

                // --- punkter ---
                positions.forEachIndexed { index, offset ->
                    val day = windowDays[index]
                    val radius = if (day == selectedDay) 10f else 6f
                    drawCircle(
                        color = lineColor,
                        radius = radius,
                        center = offset
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // info om vald punkt
        val selectedText = selectedDay?.let { day ->
            val score = day.trainingScore ?: 0.0
            "Valt pass: ${day.date}, score ${"%.1f".format(score)}"
        } ?: "Ingen punkt vald ännu."

        Text(
            text = selectedText,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Tidslinje under grafen – etiketter för de pass som finns i fönstret
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            windowDays.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.date, // korta ev. till day.date.takeLast(5)
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
