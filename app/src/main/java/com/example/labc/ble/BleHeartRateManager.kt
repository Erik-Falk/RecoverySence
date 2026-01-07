package com.example.labc.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
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

    // senaste MAC vi letar efter (om någon)
    private var targetMacForScan: String? = null

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()

    private val _connectionState = MutableStateFlow(BleConnectionState.Idle)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _connectionInfo = MutableStateFlow("Inte ansluten")
    val connectionInfo: StateFlow<String> = _connectionInfo.asStateFlow()

    // -------- Public API --------

    /**
     * Starta scanning.
     * @param targetMac Om satt -> försök hitta JUST den MAC:en.
     *                  Om null/blank -> leta efter första Polar-enhet.
     */
    @SuppressLint("MissingPermission")
    fun startScan(targetMac: String? = null) {
        val adapter = bluetoothAdapter
        Log.d(tag, "startScan(targetMac=$targetMac), adapterEnabled=${adapter?.isEnabled}")

        if (adapter == null || !adapter.isEnabled) {
            Log.w(tag, "Bluetooth är inte påslaget")
            _connectionState.value = BleConnectionState.Error
            _connectionInfo.value = "Bluetooth är inte aktiverat"
            return
        }

        if (isScanning) {
            Log.d(tag, "Redan i scanning, ignorerar startScan")
            return
        }

        val cleanedMac = targetMac?.trim()?.takeIf { it.isNotEmpty() }
        targetMacForScan = cleanedMac?.uppercase()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // Om vi har en MAC -> använd ScanFilter på adress
        val filters: List<ScanFilter>? = cleanedMac?.let {
            listOf(
                ScanFilter.Builder()
                    .setDeviceAddress(it)
                    .build()
            )
        }

        isScanning = true
        _connectionState.value = BleConnectionState.Scanning
        _connectionInfo.value = if (targetMacForScan != null) {
            "Söker efter sensor med MAC $targetMacForScan…"
        } else {
            "Söker efter pulssensor…"
        }

        scanner?.startScan(filters, settings, scanCallback)
        Log.d(tag, "Scanning started (filters=${filters != null})")
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

    // -------- ScanCallback --------

    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return

            val advName = result.scanRecord?.deviceName
            val devName = device.name
            val name = advName ?: devName ?: "okänd"
            val addr = device.address

            Log.d(
                tag,
                "SCAN HIT: name=$name devName=$devName advName=$advName addr=$addr rssi=${result.rssi}"
            )

            val macTarget = targetMacForScan
            if (macTarget != null) {
                // Vi letar specifikt efter denna MAC
                if (!addr.equals(macTarget, ignoreCase = true)) {
                    return
                }
            } else {
                // Ingen MAC angiven -> bara ta Polar-enheter
                val anyName = name
                if (!anyName.contains("polar", ignoreCase = true)) {
                    return
                }
            }

            Log.d(tag, "Väljer enhet: $name ($addr), försöker ansluta…")
            stopScanInternal()

            _connectionState.value = BleConnectionState.Connecting
            _connectionInfo.value = "Hittade: $name – ansluter…"

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
            targetMacForScan = null
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
        targetMacForScan = null
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
                    _connectionInfo.value = "Ansluten, hämtar tjänster…"
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

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Log.d(tag, "onDescriptorWrite uuid=${descriptor.uuid} status=$status")
            if (descriptor.uuid == CLIENT_CHARACTERISTIC_CONFIG_UUID && status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.Connected
                _connectionInfo.value = "Notiser aktiverade ✅"
            } else if (descriptor.uuid == CLIENT_CHARACTERISTIC_CONFIG_UUID) {
                _connectionState.value = BleConnectionState.Error
                _connectionInfo.value = "Kunde inte aktivera notiser (status $status)"
            }
        }

        // Ny overload (Android 13+)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid != HEART_RATE_MEAS_CHAR_UUID) return
            val hr = parseHeartRate(value)
            Log.d(tag, "HR notify: hr=$hr bytes=${value.joinToString { "%02X".format(it) }}")
            if (hr != null && hr > 0) _heartRate.value = hr
        }

        // Gammal overload (kompat)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
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
}
