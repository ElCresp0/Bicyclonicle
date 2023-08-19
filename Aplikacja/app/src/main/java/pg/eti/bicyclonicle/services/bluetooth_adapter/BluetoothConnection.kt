package pg.eti.bicyclonicle.services.bluetooth_adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import kotlin.streams.toList

const val LOG_TAG = "BT"

class BluetoothConnection(context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as
            BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    @SuppressLint("MissingPermission")
    fun enableBluetooth(): BluetoothStatus {
        if (!isBluetoothSupported()) {
            return BluetoothStatus.NO_BT_SUPPORT
        }

        if (!areBluetoothEnablePermissionsGranted()) {
            return BluetoothStatus.NO_BT_PERMISSIONS
        }

        if (!isBluetoothEnabled()) {
            // It will e checked in onResult because app don't wait for user input.
            return BluetoothStatus.BT_DISABLED
        }

        return BluetoothStatus.BT_ENABLED_NOT_CONNECTED
    }

    // TODO: connection
    fun isConnectedToArduino(): Boolean {
        return true
    }

    private fun isBluetoothEnabled(): Boolean {
        return when (bluetoothAdapter?.isEnabled) {
            false -> {
                Log.e(LOG_TAG, "Bluetooth is DISABLED.")
                false
            }

            true -> {
                Log.i(LOG_TAG, "Bluetooth is ENABLED.")
                true
            }

            else -> {
                Log.e(LOG_TAG, "Something went wrong with checking if bluetooth is enabled.")
                false
            }
        }
    }

    private fun isBluetoothSupported(): Boolean {
        return if (bluetoothAdapter == null) {
            Log.e(LOG_TAG, "Bluetooth is NOT supported.")
            false
        } else {
            Log.i(LOG_TAG, "Bluetooth is supported.")
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
        Log.i(LOG_TAG, "Permissions for enabling bluetooth ARE granted.")
        return true
    }

    @SuppressLint("MissingPermission")
    fun getBondedDevices(): List<String> {
        return if (bluetoothAdapter == null) {
            ArrayList()
        } else {
            bluetoothAdapter.bondedDevices.stream()
                .map { it.name }
                .toList()
        }
    }
}