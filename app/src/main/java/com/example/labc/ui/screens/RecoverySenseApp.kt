package com.example.labc.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.example.labc.ui.TrainingViewModel
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Menu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverySenseApp(viewModel: TrainingViewModel) {

    // Default screen = Home
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // Load data once
    LaunchedEffect(Unit) { viewModel.loadData() }

    val state by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun navigate(screen: Screen) {
        currentScreen = screen
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Recovery Sense",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Hem") },
                    selected = currentScreen is Screen.Home,
                    onClick = { navigate(Screen.Home) }
                )
                NavigationDrawerItem(
                    label = { Text("Import") },
                    selected = currentScreen is Screen.Import,
                    onClick = { navigate(Screen.Import) }
                )
                NavigationDrawerItem(
                    label = { Text("Grafer") },
                    selected = currentScreen is Screen.Graph,
                    onClick = { navigate(Screen.Graph) }
                )
                NavigationDrawerItem(
                    label = { Text("Rekommend") },
                    selected = currentScreen is Screen.Recommendation,
                    onClick = { navigate(Screen.Recommendation) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (currentScreen) {
                                is Screen.Home -> "Hem"
                                is Screen.Import -> "Import"
                                is Screen.Graph -> "Grafer"
                                is Screen.Recommendation -> "Rekommend"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Menu,
                                contentDescription = "Meny"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    is Screen.Home -> HomeScreen(
                        state = state,
                        onGoToImport = { currentScreen = Screen.Import },
                        onGoToGraph = { currentScreen = Screen.Graph },
                        onGoToRecommendation = { currentScreen = Screen.Recommendation }
                    )

                    is Screen.Import -> ImportScreen(viewModel = viewModel, state = state)
                    is Screen.Graph -> GraphScreen(trainingDays = state.trainingDays)
                    is Screen.Recommendation -> RecommendationScreen(trainingDays = state.trainingDays)
                }
            }
        }
    }
}
