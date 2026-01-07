package com.example.labc.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.labc.ble.BleHeartRateManager
import com.example.labc.data.TrainingRepository
import com.example.labc.data.model.HeartRateSample
import com.example.labc.data.model.TrainingDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrainingUiState(
    val isLoading: Boolean = false,
    val trainingDays: List<TrainingDay> = emptyList(),
    val errorMessage: String? = null
)

class TrainingViewModel(
    private val repository: TrainingRepository,
    private val appContext: Context
) : ViewModel() {

    private val bleManager = BleHeartRateManager(appContext)

    // BLE-status till UI
    val bleConnectionState = bleManager.connectionState
    val bleConnectionInfo = bleManager.connectionInfo

    // Livepuls till UI
    val liveHeartRate: StateFlow<Int?> = bleManager.heartRate

    // UI-state för alla pass
    private val _uiState = MutableStateFlow(TrainingUiState(isLoading = true))
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    // --- Livepass-inspelning ---
    private val liveSessionSamples = mutableListOf<HeartRateSample>()
    private var liveSessionStartTime: Long? = null
    private var isRecordingLiveSession: Boolean = false

    init {
        // Spara samples när vi spelar in
        viewModelScope.launch {
            liveHeartRate.collect { hr ->
                if (hr != null && isRecordingLiveSession) {
                    val timestamp = System.currentTimeMillis()
                    liveSessionSamples.add(
                        HeartRateSample(
                            timestamp = timestamp,
                            heartRate = hr
                        )
                    )
                }
            }
        }
    }

    // --- Ladda alla pass ---
    fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = TrainingUiState(isLoading = true)
                val days = repository.getAllTrainingDays()
                _uiState.value = TrainingUiState(
                    isLoading = false,
                    trainingDays = days
                )
            } catch (e: Exception) {
                _uiState.value = TrainingUiState(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    // --- Import från fil ---
    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                repository.importFromUri(uri, appContext)
                val days = repository.getAllTrainingDays()
                _uiState.value = _uiState.value.copy(
                    trainingDays = days,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Kunde inte importera filen: ${e.message}"
                )
            }
        }
    }

    // --- Livepass: starta (med ev. MAC) ---
    fun startLiveSession(targetMac: String?) {
        android.util.Log.d(
            "BleHR",
            "ViewModel.startLiveSession(targetMac=$targetMac) called"
        )

        liveSessionSamples.clear()
        liveSessionStartTime = System.currentTimeMillis()
        isRecordingLiveSession = true

        // Om MAC är tom/null -> vanlig scan
        if (targetMac.isNullOrBlank()) {
            bleManager.startScan()
        } else {
            // Trimma mellanslag bara för säkerhets skull
            bleManager.connectDirect(targetMac.trim())
        }
    }

    // --- Livepass: stoppa & spara ---
    fun stopLiveSessionAndSave() {
        isRecordingLiveSession = false
        bleManager.disconnect()

        val samplesCopy = liveSessionSamples.toList()
        if (samplesCopy.isEmpty()) {
            liveSessionStartTime = null
            liveSessionSamples.clear()
            return
        }

        val startTime = liveSessionStartTime ?: System.currentTimeMillis()

        viewModelScope.launch {
            try {
                repository.saveLiveSession(
                    samples = samplesCopy,
                    startTimeMillis = startTime
                )

                val days = repository.getAllTrainingDays()
                _uiState.value = _uiState.value.copy(
                    trainingDays = days,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Kunde inte spara livepasset: ${e.message}"
                )
            } finally {
                liveSessionSamples.clear()
                liveSessionStartTime = null
            }
        }
    }

    // Används från UI
    fun startLiveHeartRate(targetMac: String?) {
        android.util.Log.d("BleHR", "ViewModel.startLiveHeartRate(mac=$targetMac) alias called")
        startLiveSession(targetMac)
    }

    fun stopLiveHeartRate() = stopLiveSessionAndSave()

    override fun onCleared() {
        super.onCleared()
        isRecordingLiveSession = false
        bleManager.disconnect()
    }
}
