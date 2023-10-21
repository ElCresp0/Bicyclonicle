package pg.eti.bicyclonicle.arduino_connection.services

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import pg.eti.bicyclonicle.arduino_connection.enums.BluetoothStatus
import pg.eti.bicyclonicle.preferences.SharedPreferencesManager
import kotlin.streams.toList

private const val BT_CONN_TAG = "BT_CONN"

class BluetoothManager private constructor (
    private val bluetoothAdapter: BluetoothAdapter?,
    private val arduinoName: String,
    private val spm: SharedPreferencesManager
) {
    fun getBluetoothStatus(): BluetoothStatus {
        if (!isBluetoothSupported()) {
            return BluetoothStatus.NO_BT_SUPPORT
        }

        if (!isBluetoothEnabled()) {
            return BluetoothStatus.BT_DISABLED
        }

        if (getArduinoDevice() == null) {
            return BluetoothStatus.BT_ENABLED_NOT_PAIRED
        }

        return BluetoothStatus.BT_PAIRED_WITH_ARDUINO
    }

    private fun isBluetoothEnabled(): Boolean {
        return when (bluetoothAdapter?.isEnabled) {
            false -> {
                Log.e(BT_CONN_TAG, "Bluetooth is DISABLED.")
                false
            }

            true -> {
                Log.i(BT_CONN_TAG, "Bluetooth is ENABLED.")
                true
            }

            else -> {
                Log.e(BT_CONN_TAG, "Something went wrong with checking if bluetooth is enabled.")
                false
            }
        }
    }

    private fun isBluetoothSupported(): Boolean {
        return if (bluetoothAdapter == null) {
            Log.e(BT_CONN_TAG, "Bluetooth is NOT supported.")
            false
        } else {
            Log.i(BT_CONN_TAG, "Bluetooth is supported.")
            true
        }
    }

    @SuppressLint("MissingPermission") // in HomeFragment
    fun getArduinoDevice(): BluetoothDevice? {
        if (bluetoothAdapter == null) {
            Log.e(BT_CONN_TAG, "BluetoothAdapter is null!")
            return null
        }

        if (spm.isPermissionBluetoothConnect()) {
            val arduinoDevice = bluetoothAdapter.bondedDevices.stream()
                .toList()
                .find { it.name == arduinoName }

            if (arduinoDevice == null) {
                Log.e(BT_CONN_TAG, "Can't find Arduino device.")
                return null
            }

            return bluetoothAdapter.getRemoteDevice(arduinoDevice.address)
        }
        return null
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: BluetoothManager? = null

        fun getInstance(
            bluetoothAdapter: BluetoothAdapter?,
            arduinoName: String,
            spm: SharedPreferencesManager
        ): BluetoothManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothManager(
                    bluetoothAdapter,
                    arduinoName,
                    spm
                ).also { INSTANCE = it }
            }
        }
    }
}