package com.example.labc.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * En enkel BLE-manager för att läsa Heart Rate Service (HRS) från t.ex. Polar Verity Sense.
 *
 * Används så här:
 *  - Skapa en instans i ViewModel: val bleManager = BleHeartRateManager(appContext)
 *  - startScan() när permissions är godkända
 *  - lyssna på heartRate / connectionState i UI via StateFlow
 */

enum class BleConnectionState {
    Idle,
    Scanning,
    Connecting,
    Connected,
    Error
}

class BleHeartRateManager(
    private val context: Context
) {

    private val tag = "BleHR"

    // UUIDs för Heart Rate Service + Measurement + CCCD
    private val HEART_RATE_SERVICE_UUID: UUID =
        UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
    private val HEART_RATE_MEAS_CHAR_UUID: UUID =
        UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val scanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private var currentGatt: BluetoothGatt? = null

    // --- StateFlows som UI/ViewModel observerar ---

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()

    private val _connectionState = MutableStateFlow(BleConnectionState.Idle)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _connectionInfo = MutableStateFlow("Inte ansluten")
    val connectionInfo: StateFlow<String> = _connectionInfo.asStateFlow()

    private var isScanning = false

    // ---------------------- Publika API: start / stop ----------------------

    /**
     * Starta scanning efter enheter med Heart Rate Service.
     * Får anropas först när rätt BLE-permissioner är godkända.
     */
    @SuppressLint("MissingPermission")
    fun startScan() {
        Log.d(tag, "startScan() called")

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(tag, "Bluetooth är inte påslaget")
            _connectionState.value = BleConnectionState.Error
            _connectionInfo.value = "Bluetooth är inte aktiverat"
            return
        }

        if (isScanning) {
            Log.d(tag, "Redan i scanning, ignorerar startScan()")
            return
        }

        // OBS: ingen filter på service-UUID längre – vi loggar allt vi ser
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        _connectionState.value = BleConnectionState.Scanning
        _connectionInfo.value = "Söker efter pulssensor..."
        isScanning = true

        scanner?.startScan(null, settings, scanCallback)
        Log.d(tag, "Scanning started (utan filter)")
    }

    /**
     * Koppla ner från aktuell GATT + stoppa scanning.
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        Log.d(tag, "disconnect() called")
        stopScanInternal()

        currentGatt?.close()
        currentGatt = null

        _heartRate.value = null
        _connectionState.value = BleConnectionState.Idle
        _connectionInfo.value = "Frånkopplad"
    }

    // ---------------------- ScanCallback ----------------------

    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device ?: return

            val name = device.name ?: "okänd"
            Log.d(tag, "onScanResult: name=$name addr=${device.address}")

            // Filtrera på namn istället – justera om din sensor heter något annat
            if (!name.contains("Polar", ignoreCase = true)) {
                // Ignorera enheter som inte ser ut som en Polar-sensor
                return
            }

            Log.d(tag, "Väljer enhet: $name – försöker ansluta")
            stopScanInternal()

            _connectionState.value = BleConnectionState.Connecting
            _connectionInfo.value = "Hittade: $name – ansluter..."

            currentGatt = device.connectGatt(
                context,
                false,
                gattCallback
            )
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(tag, "onScanFailed: errorCode=$errorCode")
            _connectionState.value = BleConnectionState.Error
            _connectionInfo.value = "Scanning misslyckades (kod $errorCode)"
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        if (!isScanning) return
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(tag, "stopScanInternal: ${e.message}")
        }
        isScanning = false
        Log.d(tag, "Scanning stopped")
    }

    // ---------------------- GATT-callback ----------------------

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.d(tag, "onConnectionStateChange: status=$status newState=$newState")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.Error
                _connectionInfo.value = "Fel vid anslutning (status $status)"
                gatt.close()
                currentGatt = null
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = BleConnectionState.Connected
                    _connectionInfo.value =
                        "Ansluten till ${gatt.device.name ?: "enhet"}"
                    Log.d(tag, "GATT connected, discovering services...")
                    gatt.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = BleConnectionState.Idle
                    _connectionInfo.value = "Frånkopplad"
                    Log.d(tag, "GATT disconnected")
                    gatt.close()
                    currentGatt = null
                }
            }
        }

        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {
            super.onServicesDiscovered(gatt, status)
            Log.d(tag, "onServicesDiscovered: status=$status")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.Error
                _connectionInfo.value = "Kunde inte hitta tjänster (status $status)"
                return
            }

            val hrService = gatt.getService(HEART_RATE_SERVICE_UUID)
            if (hrService == null) {
                Log.e(tag, "Heart Rate Service hittades inte")
                _connectionState.value = BleConnectionState.Error
                _connectionInfo.value = "Heart Rate Service hittades inte"
                return
            }

            val hrChar = hrService.getCharacteristic(HEART_RATE_MEAS_CHAR_UUID)
            if (hrChar == null) {
                Log.e(tag, "Heart Rate Measurement-karaktäristik saknas")
                _connectionState.value = BleConnectionState.Error
                _connectionInfo.value = "Heart Rate Measurement saknas"
                return
            }

            // Slå på notiser för heart rate
            val ok = gatt.setCharacteristicNotification(hrChar, true)
            Log.d(tag, "setCharacteristicNotification=$ok")

            val cccd = hrChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            if (cccd != null) {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(cccd)
                Log.d(tag, "Skrev CCCD för notiser")
            } else {
                Log.w(tag, "Ingen CCCD-descriptor, vissa enheter kräver detta")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)

            if (characteristic.uuid == HEART_RATE_MEAS_CHAR_UUID) {
                val hr = parseHeartRate(characteristic)
                Log.d(tag, "onCharacteristicChanged: hr=$hr")
                if (hr != null && hr > 0) {
                    _heartRate.value = hr
                }
            }
        }
    }

    // ---------------------- Hjälpmetod för att tolka HR ----------------------

    private fun parseHeartRate(ch: BluetoothGattCharacteristic): Int? {
        val value = ch.value ?: return null
        if (value.isEmpty()) return null

        // Första byte = flags, bit0 säger om HR är 8- eller 16-bitars
        val flags = value[0].toInt()
        val hrFormat16Bit = (flags and 0x01) != 0

        return if (!hrFormat16Bit) {
            // 8-bit HR ligger i byte 1
            value[1].toInt() and 0xFF
        } else {
            // 16-bit HR ligger i byte 1 & 2 (little endian)
            val lower = value[1].toInt() and 0xFF
            val upper = value[2].toInt() and 0xFF
            (upper shl 8) or lower
        }
    }
}
