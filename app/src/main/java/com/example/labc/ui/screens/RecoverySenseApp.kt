package com.example.labc.ui.screens

import android.Manifest
import android.os.Build
import android.util.Log
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

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

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

    // MAC som användaren vill använda nästa gång vi startar
    val pendingMac = remember { mutableStateOf<String?>(null) }

    val blePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        android.util.Log.d("BleHR", "Permission callback: $perms")

        val canScan =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms[Manifest.permission.BLUETOOTH_SCAN] == true &&
                        perms[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else {
                perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
            }

        if (canScan) {
            android.util.Log.d("BleHR", "Rätt BLE-permissioner – startar scan (mac=${pendingMac.value})")
            viewModel.startLiveHeartRate(pendingMac.value)
        } else {
            android.util.Log.d("BleHR", "Permission nekad, startar INTE scan")
        }
    }

    fun startLiveWithPermissions(targetMac: String?) {
        pendingMac.value = targetMac?.trim().takeIf { !it.isNullOrEmpty() }

        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        }
        // Location hjälper scanning på många enheter, även nya
        permissions += Manifest.permission.ACCESS_FINE_LOCATION

        blePermissionLauncher.launch(permissions.toTypedArray())
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { /* som du hade */ }
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
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Meny")
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

                    is Screen.Import -> ImportScreen(viewModel = viewModel, state = state)

                    is Screen.Graph -> GraphScreen(trainingDays = state.trainingDays)

                    is Screen.Recommendation ->
                        RecommendationScreen(trainingDays = state.trainingDays)

                    is Screen.Live -> LiveHeartRateScreen(
                        liveHeartRate = liveHr,
                        connectionState = connState,
                        connectionInfo = connInfo,
                        onStartLive = { mac -> startLiveWithPermissions(mac) },
                        onStopLive = { viewModel.stopLiveHeartRate() }
                    )
                }
            }
        }
    }
}