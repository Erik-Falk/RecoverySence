package com.example.labc.ble

import android.Manifest
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
import androidx.annotation.RequiresPermission
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

class BleHeartRateManager(private val context: Context) {

    private val tag = "BleHR"

    private val HEART_RATE_SERVICE_UUID =
        UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val HEART_RATE_MEAS_CHAR_UUID =
        UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private var currentGatt: BluetoothGatt? = null
    private var isScanning = false

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()

    private val _connectionState = MutableStateFlow(BleConnectionState.Idle)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _connectionInfo = MutableStateFlow("Inte ansluten")
    val connectionInfo: StateFlow<String> = _connectionInfo.asStateFlow()

    // -------- Public API --------

    @SuppressLint("MissingPermission")
    fun startScan() {
        Log.d(tag, "startScan() called")

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w(tag, "Bluetooth är inte påslaget")
            _connectionState.value = BleConnectionState.Error
            _connectionInfo.value = "Bluetooth är inte aktiverat"
            return
        }

        if (isScanning) return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filter = ScanFilter.Builder()
            .setDeviceAddress("A0:9E:1A:C4:45:8C") // <-- replace with YOUR Polar address
            .build()

        val leScanner = bluetoothAdapter?.bluetoothLeScanner
        leScanner?.startScan(listOf(filter), settings, scanCallback)
        Log.d(tag, "Scanning started (address filter)")
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        Log.d(tag, "disconnect()")
        stopScanInternal()

        currentGatt?.disconnect()
        currentGatt?.close()
        currentGatt = null

        _heartRate.value = null
        _connectionState.value = BleConnectionState.Idle
        _connectionInfo.value = "Frånkopplad"
    }

    // -------- ScanCallback (THIS is the only onScanResult that matters) --------

    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return

            val advName = result.scanRecord?.deviceName
            val devName = device.name
            val name = advName ?: devName ?: "okänd"

            Log.d(tag, "SCAN HIT: name=$name devName=$devName advName=$advName addr=${device.address} rssi=${result.rssi}")

            Log.d(tag, "scan: name=$name devName=$devName advName=$advName addr=${device.address} rssi=${result.rssi}")

            // Safer filter: Polar devices often advertise "Polar ..."
            if (!name.contains("polar", ignoreCase = true)) return

            Log.d(tag, "Found Polar-like device: $name, connecting...")
            stopScanInternal()

            _connectionState.value = BleConnectionState.Connecting
            _connectionInfo.value = "Hittade: $name – ansluter..."

            currentGatt = device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            Log.d(tag, "BATCH size=${results.size}")
            results.forEach { r ->
                val name = r.scanRecord?.deviceName ?: r.device.name ?: "okänd"
                Log.d(tag, "BATCH HIT: name=$name addr=${r.device.address} rssi=${r.rssi}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "onScanFailed: $errorCode")
            isScanning = false
            _connectionState.value = BleConnectionState.Error
            _connectionInfo.value = "Scanning misslyckades (kod $errorCode)"
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        if (!isScanning) return
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) { }
        isScanning = false
        Log.d(tag, "Scanning stopped")
    }

    // -------- GATT callback --------

    @SuppressLint("MissingPermission")
    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(tag, "onConnectionStateChange status=$status newState=$newState")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.Error
                _connectionInfo.value = "Fel vid anslutning (status $status)"
                gatt.close()
                currentGatt = null
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionInfo.value = "Ansluten, hämtar tjänster..."
                    Log.d(tag, "Connected -> discoverServices()")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = BleConnectionState.Idle
                    _connectionInfo.value = "Frånkopplad"
                    Log.d(tag, "Disconnected")
                    gatt.close()
                    currentGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(tag, "onServicesDiscovered status=$status")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.Error
                _connectionInfo.value = "Kunde inte hitta tjänster (status $status)"
                return
            }

            val hrChar = gatt.getService(HEART_RATE_SERVICE_UUID)
                ?.getCharacteristic(HEART_RATE_MEAS_CHAR_UUID)

            if (hrChar == null) {
                _connectionState.value = BleConnectionState.Error
                _connectionInfo.value = "Hittade inte HR 180D/2A37"
                return
            }

            val okLocal = gatt.setCharacteristicNotification(hrChar, true)
            Log.d(tag, "setCharacteristicNotification=$okLocal")

            val cccd = hrChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            if (cccd == null) {
                _connectionState.value = BleConnectionState.Error
                _connectionInfo.value = "CCCD (2902) saknas"
                return
            }

            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            val started = gatt.writeDescriptor(cccd)
            Log.d(tag, "writeDescriptor(CCCD) started=$started")
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Log.d(tag, "onDescriptorWrite uuid=${descriptor.uuid} status=$status")
            if (descriptor.uuid == CLIENT_CHARACTERISTIC_CONFIG_UUID && status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.Connected
                _connectionInfo.value = "Notiser aktiverade ✅"
            } else if (descriptor.uuid == CLIENT_CHARACTERISTIC_CONFIG_UUID) {
                _connectionState.value = BleConnectionState.Error
                _connectionInfo.value = "Kunde inte aktivera notiser (status $status)"
            }
        }

        // New overload (Android 13+ friendly)
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid != HEART_RATE_MEAS_CHAR_UUID) return
            val hr = parseHeartRate(value)
            Log.d(tag, "HR notify: hr=$hr bytes=${value.joinToString { "%02X".format(it) }}")
            if (hr != null && hr > 0) _heartRate.value = hr
        }

        // Old overload forwarder (compat)
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value ?: return
            onCharacteristicChanged(gatt, characteristic, value)
        }
    }

    private fun parseHeartRate(value: ByteArray): Int? {
        if (value.isEmpty()) return null
        val flags = value[0].toInt()
        val hr16 = (flags and 0x01) != 0

        return if (!hr16) {
            value.getOrNull(1)?.toInt()?.and(0xFF)
        } else {
            val lo = value.getOrNull(1)?.toInt()?.and(0xFF) ?: return null
            val hi = value.getOrNull(2)?.toInt()?.and(0xFF) ?: return null
            (hi shl 8) or lo
        }
    }

    @SuppressLint("MissingPermission")
    fun connectDirect(mac: String) {
        Log.d(tag, "connectDirect($mac)")

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = BleConnectionState.Error
            _connectionInfo.value = "Bluetooth är inte aktiverat"
            return
        }

        stopScanInternal()

        val device = adapter.getRemoteDevice(mac)
        _connectionState.value = BleConnectionState.Connecting
        _connectionInfo.value = "Ansluter direkt till $mac..."

        currentGatt = device.connectGatt(
            context,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
    }
}
