package com.example.labc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.labc.ui.screens.HomeScreen
import com.example.labc.ui.screens.GraphScreen
import com.example.labc.ui.screens.RecommendationScreen
import com.example.labc.ui.TrainingViewModel
import androidx.compose.runtime.collectAsState

@Composable
fun RecoverySenseApp(viewModel: TrainingViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // top-nav
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavButton(
                    text = "Hem",
                    selected = currentScreen is Screen.Home,
                    onClick = { currentScreen = Screen.Home }
                )
                NavButton(
                    text = "Grafer",
                    selected = currentScreen is Screen.Graph,
                    onClick = { currentScreen = Screen.Graph }
                )
                NavButton(
                    text = "Rekommendation",
                    selected = currentScreen is Screen.Recommendation,
                    onClick = { currentScreen = Screen.Recommendation }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                when (currentScreen) {
                    is Screen.Home -> HomeScreen(viewModel = viewModel, state = state)
                    is Screen.Graph -> GraphScreen(trainingDays = state.trainingDays)
                    is Screen.Recommendation -> RecommendationScreen(trainingDays = state.trainingDays)
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val background = if (selected) colors.primary else colors.surfaceVariant
    val contentColor = if (selected) colors.onPrimary else colors.onSurfaceVariant

    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)      // funkar nu – vi är i RowScope
            .height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = contentColor
        )
    ) {
        Text(text)
    }
}
