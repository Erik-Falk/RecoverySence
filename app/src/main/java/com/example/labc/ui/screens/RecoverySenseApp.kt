package com.example.labc.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.labc.ui.TrainingViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverySenseApp(viewModel: TrainingViewModel) {

    // vilken skärm som visas
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // ladda pass vid start
    LaunchedEffect(Unit) { viewModel.loadData() }

    val state by viewModel.uiState.collectAsState()
    val liveHr by viewModel.liveHeartRate.collectAsState()
    val connState by viewModel.bleConnectionState.collectAsState()
    val connInfo by viewModel.bleConnectionInfo.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun navigate(screen: Screen) {
        currentScreen = screen
        scope.launch { drawerState.close() }
    }

    // ---- BLE permissions ----
    val blePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        android.util.Log.d("BleHR", "Permission callback: $perms")
        val allGranted = perms.values.all { it }
        if (allGranted) {
            android.util.Log.d("BleHR", "Alla BLE-permissioner godkända – startar live")
            viewModel.startLiveHeartRate()
        } else {
            android.util.Log.d("BleHR", "Permission nekad, startar INTE scan")
        }
    }

    fun startLiveWithPermissions() {
        blePermissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
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
                    label = { Text("Rekommendation") },
                    selected = currentScreen is Screen.Recommendation,
                    onClick = { navigate(Screen.Recommendation) }
                )
                NavigationDrawerItem(
                    label = { Text("Livepuls") },
                    selected = currentScreen is Screen.Live,
                    onClick = { navigate(Screen.Live) }
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
                                is Screen.Recommendation -> "Rekommendation"
                                is Screen.Live -> "Livepuls"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
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
                        onGoToImport = { navigate(Screen.Import) },
                        onGoToGraph = { navigate(Screen.Graph) },
                        onGoToRecommendation = { navigate(Screen.Recommendation) },
                        onGoToLive = { navigate(Screen.Live) }
                    )

                    is Screen.Import -> ImportScreen(
                        viewModel = viewModel,
                        state = state
                    )

                    is Screen.Graph -> GraphScreen(
                        trainingDays = state.trainingDays
                    )

                    is Screen.Recommendation -> RecommendationScreen(
                        trainingDays = state.trainingDays
                    )

                    is Screen.Live -> LiveHeartRateScreen(
                        liveHeartRate = liveHr,
                        connectionState = connState,
                        connectionInfo = connInfo,
                        onStartLive = { startLiveWithPermissions() },
                        onStopLive = { viewModel.stopLiveHeartRate() }
                    )
                }
            }
        }
    }
}
