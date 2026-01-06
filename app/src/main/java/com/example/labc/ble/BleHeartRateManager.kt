package com.example.labc.ble

import android.content.Context
import android.util.Log
import com.polar.androidcommunications.api.ble.model.DisInfo
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApi.PolarBleSdkFeature
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHealthThermometerData
import com.polar.sdk.api.model.PolarHrData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class BleConnectionState {
    Idle,
    Scanning,
    Connecting,
    Connected,
    Error
}

// (Valfri) hårdkodad device-id om du vill använda connectToDevice istället för autoConnect
private const val POLAR_DEVICE_ID = "C4458C23"

class BleHeartRateManager(
    context: Context
) {

    private val tag = "BleHR"

    // --- Polar SDK API ---
    private val api: PolarBleApi =
        PolarBleApiDefaultImpl.defaultImplementation(
            context.applicationContext,
            setOf(
                PolarBleSdkFeature.FEATURE_HR // vi behöver bara puls för nu
            )
        )

    private val disposables = CompositeDisposable()
    private var currentDeviceId: String? = null

    // --- StateFlows till UI / ViewModel ---

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()

    private val _connectionState = MutableStateFlow(BleConnectionState.Idle)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _connectionInfo = MutableStateFlow("Inte ansluten")
    val connectionInfo: StateFlow<String> = _connectionInfo.asStateFlow()

    init {
        api.setApiCallback(object : PolarBleApiCallback() {

            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(tag, "BLE power: $powered")
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(tag, "CONNECTING: ${polarDeviceInfo.deviceId} ${polarDeviceInfo.name}")
                _connectionState.value = BleConnectionState.Connecting
                _connectionInfo.value = "Ansluter: ${polarDeviceInfo.name}"
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(tag, "CONNECTED: ${polarDeviceInfo.deviceId} ${polarDeviceInfo.name}")
                currentDeviceId = polarDeviceInfo.deviceId
                _connectionState.value = BleConnectionState.Connected
                _connectionInfo.value = "Ansluten: ${polarDeviceInfo.name}"

                // Starta HR-stream direkt när vi är anslutna
                startHrStreamingInternal(polarDeviceInfo.deviceId)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(tag, "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                if (currentDeviceId == polarDeviceInfo.deviceId) {
                    currentDeviceId = null
                }
                _connectionState.value = BleConnectionState.Idle
                _connectionInfo.value = "Frånkopplad"
                _heartRate.value = null
            }

            override fun bleSdkFeatureReady(
                identifier: String,
                feature: PolarBleSdkFeature
            ) {
                Log.d(tag, "Feature ready: $feature for $identifier")
            }

            // Måste finnas med i den nya versionen av SDK:n, men vi använder dem inte
            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(tag, "disInformationReceived(uuid): $identifier, $uuid, $value")
            }

            override fun disInformationReceived(identifier: String, disInfo: DisInfo) {
                Log.d(tag, "disInformationReceived(disInfo): $identifier, $disInfo")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(tag, "batteryLevelReceived: $identifier level=$level")
            }

            override fun htsNotificationReceived(
                identifier: String,
                data: PolarHealthThermometerData
            ) {
                Log.d(tag, "htsNotificationReceived: $identifier, $data")
            }
        })
    }

    /**
     * Starta automatisk anslutning till närmsta Polar-enhet.
     * Polar SDK sköter scanning + connect.
     */
    fun startScan() {
        Log.d(tag, "startScan() / autoConnect called")

        if (_connectionState.value == BleConnectionState.Connected ||
            _connectionState.value == BleConnectionState.Connecting
        ) {
            Log.d(tag, "Redan ansluten/ansluter – ignorerar startScan()")
            return
        }

        _connectionState.value = BleConnectionState.Scanning
        _connectionInfo.value = "Söker efter Polar-sensor..."

        val d = api.autoConnectToDevice(
            -60,   // RSSI-gräns (negativ dBm), -60 ≈ ganska nära
            null,  // inget specifikt deviceId-filter
            null   // alla Polar-typer
        )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d(tag, "autoConnect completed")
            }, { error ->
                Log.e(tag, "autoConnect error: ${error.message}", error)
                _connectionState.value = BleConnectionState.Error
                _connectionInfo.value = "autoConnect-fel: ${error.message}"
            })

        disposables.add(d)
    }

    /**
     * Stoppa HR-ström och koppla ned från aktuell enhet.
     */
    fun disconnect() {
        Log.d(tag, "disconnect() called")
        val id = currentDeviceId
        if (id != null) {
            try {
                api.disconnectFromDevice(id)
            } catch (e: Exception) {
                Log.w(tag, "disconnectFromDevice error: ${e.message}")
            }
        }
        currentDeviceId = null
        _heartRate.value = null
        _connectionState.value = BleConnectionState.Idle
        _connectionInfo.value = "Frånkopplad"
    }

    /**
     * Intern: startar HR-streaming via Polar-SDK.
     */
    private fun startHrStreamingInternal(deviceId: String) {
        Log.d(tag, "startHrStreamingInternal($deviceId)")

        val d = api.startHrStreaming(deviceId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ hrData: PolarHrData ->
                if (hrData.samples.isNotEmpty()) {
                    val hr = hrData.samples.last().hr
                    Log.d(tag, "HR=$hr")
                    if (hr > 0) {
                        _heartRate.value = hr
                    }
                }
            }, { error ->
                Log.e(tag, "HR stream error: ${error.message}", error)
                _connectionState.value = BleConnectionState.Error
                _connectionInfo.value = "HR-stream fel: ${error.message}"
            })

        disposables.add(d)
    }

    /**
     * Anropa från t.ex. ViewModel.onCleared()
     */
    fun shutdown() {
        Log.d(tag, "shutdown()")
        disconnect()
        disposables.clear()
        api.shutDown()
    }
}
