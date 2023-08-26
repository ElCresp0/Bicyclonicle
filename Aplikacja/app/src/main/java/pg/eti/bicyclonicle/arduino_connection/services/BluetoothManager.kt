package pg.eti.bicyclonicle.arduino_connection.services

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import pg.eti.bicyclonicle.arduino_connection.enums.BluetoothStatus
import kotlin.streams.toList

class BluetoothManager private constructor (
    private val bluetoothAdapter: BluetoothAdapter?,
    private val arduinoName: String
) {
    // todo: change tags in all modules
    private val BT_CONN_TAG = "BT_CONN"

    @SuppressLint("MissingPermission")
    fun getBluetoothStatus(): BluetoothStatus {
        if (!isBluetoothSupported()) {
            return BluetoothStatus.NO_BT_SUPPORT
        }

        if (!areBluetoothEnablePermissionsGranted()) {
            return BluetoothStatus.NO_BT_PERMISSIONS
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

    private fun areBluetoothEnablePermissionsGranted(): Boolean {
        // TODO: Request Bluetooth permission if not enabled.
        //  Maybe with permission manager?
//        if (!bluetoothAdapter!!.isEnabled) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
//        }
        // TODO: log wrong
        Log.i(BT_CONN_TAG, "Permissions for enabling bluetooth ARE granted.")
        return true
    }

    // todo: permissions
    @SuppressLint("MissingPermission")
    fun getArduinoDevice(): BluetoothDevice? {
        if (bluetoothAdapter == null) {
            Log.e(BT_CONN_TAG, "BluetoothAdapter is null!")
            return null
        }

        val arduinoDevice = bluetoothAdapter.bondedDevices.stream()
            .toList()
            .find { it.name == arduinoName }

        if (arduinoDevice == null) {
            Log.e(BT_CONN_TAG, "Can't find Arduino device.")
            return null
        }

        return bluetoothAdapter.getRemoteDevice(arduinoDevice.address)
    }

    companion object {
        @Volatile
        private var INSTANCE: BluetoothManager? = null

        fun getInstance(bluetoothAdapter: BluetoothAdapter?, arduinoName: String): BluetoothManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothManager(bluetoothAdapter, arduinoName).also { INSTANCE = it }
            }
        }
    }
}