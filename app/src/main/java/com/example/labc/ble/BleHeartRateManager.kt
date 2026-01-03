package com.example.labc.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

@SuppressLint("MissingPermission")   // Vi hanterar tillstånd själva tar bort felmeddelande
class BleHeartRateManager(
    private val context: Context
) {

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private val HEART_RATE_SERVICE_UUID =
        UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
    private val HEART_RATE_MEASUREMENT_UUID =
        UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate

    private var currentGatt: BluetoothGatt? = null
    private var scanning = false

    private fun hasScanPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

    fun startScan() {
        if (scanning) return
        if (!hasScanPermission()) return  // säkerhetscheck

        val scanner = scanner ?: return

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
        scanning = true
    }

    fun stopScan() {
        if (!scanning) return
        scanner?.stopScan(scanCallback)
        scanning = false
    }

    fun disconnect() {
        stopScan()
        currentGatt?.close()
        currentGatt = null
        _heartRate.value = null
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // När vi hittar en HRS-enhet kopplar vi upp
            stopScan()
            val device = result.device
            device.connectGatt(context, false, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS &&
                newState == BluetoothProfile.STATE_CONNECTED
            ) {
                currentGatt = gatt
                gatt.discoverServices()
            } else {
                gatt.close()
                if (currentGatt == gatt) currentGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val service = gatt.getService(HEART_RATE_SERVICE_UUID) ?: return
            val characteristic =
                service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID) ?: return

            gatt.setCharacteristicNotification(characteristic, true)

            val descriptor = characteristic.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
            )
            descriptor?.let {
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(it)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                val hr = parseHeartRate(characteristic.value)
                _heartRate.value = hr
            }
        }
    }

    private fun parseHeartRate(data: ByteArray): Int? {
        if (data.isEmpty()) return null
        val flag = data[0].toInt()
        val isUint16 = flag and 0x01 != 0

        return if (isUint16) {
            if (data.size >= 3) {
                ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            } else null
        } else {
            if (data.size >= 2) data[1].toInt() and 0xFF else null
        }
    }
}
