package com.example.labc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
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

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Enkel top-navigering
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { currentScreen = Screen.Home }) {
                    Text("Hem")
                }
                Button(onClick = { currentScreen = Screen.Graph }) {
                    Text("Grafer")
                }
                Button(onClick = { currentScreen = Screen.Recommendation }) {
                    Text("Rekommendation")
                }
            }

            // Själva innehållet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                when (currentScreen) {
                    is Screen.Home -> HomeScreen(
                        viewModel = viewModel,
                        state = state
                    )
                    is Screen.Graph -> GraphScreen(
                        trainingDays = state.trainingDays
                    )
                    is Screen.Recommendation -> RecommendationScreen(
                        trainingDays = state.trainingDays
                    )
                }
            }
        }
    }
}
