package pg.eti.bicyclonicle.arduino_connection.services

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.core.util.Consumer
import pg.eti.bicyclonicle.arduino_connection.enums.BluetoothStatus
import pg.eti.bicyclonicle.arduino_connection.enums.ConnectionStatus
import pg.eti.bicyclonicle.preferences.SharedPreferencesManager
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import pg.eti.bicyclonicle.arduino_connection.enums.ArduinoResponse as ar

const val MANAGE_CONN_TAG = "MANAGE_CONN"

class ConnectionManager private constructor(
    private val context: Context,
    private val enableBluetoothByIntent: Consumer<Intent>,
) {
    // Members for connection to Arduino.
    private var mmSocket: BluetoothSocket? = null
    private var connectedThread: ConnectedThread? = null
    private var connectionHandler: Handler? = null
    private val prefsManager = SharedPreferencesManager.getInstance(context)

    // Members for bluetooth module.
    private var androidBluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE)
            as android.bluetooth.BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = androidBluetoothManager.adapter
    private var bluetoothManager: BluetoothManager = BluetoothManager.getInstance(
        bluetoothAdapter,
        "JANEK-LAPTOP",
        prefsManager
    )
    private var wasAskedToEnableBt = false

    private val responseSemaphore = Semaphore(0)

    fun connectToArduino(): ConnectionStatus {
        val arduinoConnectionStatus = getUpdatedArduinoConnectionStatus()

        // We don't want to get endless loop connecting.
        if (arduinoConnectionStatus == ConnectionStatus.CONNECTED) {
            Log.i(MANAGE_CONN_TAG, "ARDUINO CONNECTED")
            return arduinoConnectionStatus
        }

        val bluetoothStatus = bluetoothManager.getBluetoothStatus()
        // todo: the rest statuses
        if (bluetoothStatus == BluetoothStatus.BT_DISABLED) {
            if (!wasAskedToEnableBt) {
                Log.i(MANAGE_CONN_TAG, "Asking to enable bluetooth.")
                askToEnableBluetooth()
                wasAskedToEnableBt = true
            }
        } else if (bluetoothStatus == BluetoothStatus.BT_ENABLED_NOT_PAIRED) {
            Log.e(MANAGE_CONN_TAG, "You need to pair arduino!")
            Toast.makeText(context, "You need to pair arduino!", Toast.LENGTH_SHORT).show()

        } else if (bluetoothStatus == BluetoothStatus.BT_PAIRED_WITH_ARDUINO) {
            val arduinoDevice = bluetoothManager.getArduinoDevice()

            if (arduinoDevice == null) {
                Log.e(MANAGE_CONN_TAG, "Arduino device is null!")
                return ConnectionStatus.NOT_CONNECTED
            }

            mmSocket = setupSocket(arduinoDevice)
            connectionHandler = getBluetoothConnectionHandler()

            if (startConnection() == ConnectionStatus.CONNECTED) {
                // Set global var that is observed by modules to decide whether perform actions.
                prefsManager.setIsArduinoConnected(true)

                return ConnectionStatus.CONNECTED
            }
        } else if (bluetoothStatus == BluetoothStatus.NO_BT_PERMISSIONS) {
            Toast.makeText(context, "No permissions for bluetooth!", Toast.LENGTH_SHORT).show()
        }
        return ConnectionStatus.NOT_CONNECTED
    }

    @SuppressLint("MissingPermission") // in HomeFragment
    private fun setupSocket(arduinoDevice: BluetoothDevice): BluetoothSocket? {
        var tmpSocket: BluetoothSocket? = null
        val btSppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        if (prefsManager.isPermissionBluetoothConnect()) {
            try {
                tmpSocket = arduinoDevice.createRfcommSocketToServiceRecord(btSppUuid)
            } catch (e: IOException) {
                Log.e(MANAGE_CONN_TAG, "Socket's create() method failed", e)
            }
        } else {
            Log.e(MANAGE_CONN_TAG, "No permissions to create socket.")
        }

        return tmpSocket
    }

    private fun getBluetoothConnectionHandler(): Handler {
        return object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    // todo: what if there will be no connection and something in buffer
                    // todo: what if while exchanging data connection will be lost?
                    ConnectionStatus.CONNECTION_STATUS.ordinal -> when (msg.arg1) {
                        ConnectionStatus.CONNECTED.ordinal -> {
                            // todo: when connected
                        }

                        ConnectionStatus.NOT_CONNECTED.ordinal -> {
                            // todo: when not connected
                        }
                    }

                    ConnectionStatus.MESSAGE_READ.ordinal -> {
                        val arduinoMsg: String = msg.obj.toString() // Read message from Arduino
                        Log.i(MANAGE_CONN_TAG, "ARDUINO_MESSAGE: $arduinoMsg")
                        when (arduinoMsg.lowercase(Locale.getDefault())) {
                            ar.EXECUTED.response -> {
                                responseSemaphore.release()
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission") // in HomeFragment
    private fun startConnection(): ConnectionStatus {
        // Cancel discovery because it otherwise slows down the connection.
        if (bluetoothAdapter == null) {
            Log.e(MANAGE_CONN_TAG, "BluetoothAdapter is null!")
            return ConnectionStatus.NOT_CONNECTED
        }

        if (prefsManager.isPermissionBluetoothScan()) {
            bluetoothAdapter.cancelDiscovery()

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket!!.connect()
                Log.i(MANAGE_CONN_TAG, "Device connected")

            } catch (connectException: IOException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket!!.close()
                    Log.e(MANAGE_CONN_TAG, "Cannot connect to device")

                } catch (closeException: IOException) {
                    Log.e(MANAGE_CONN_TAG, "Could not close the client socket", closeException)
                }

                prefsManager.setIsArduinoConnected(false)

                return ConnectionStatus.NOT_CONNECTED
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = ConnectedThread(mmSocket!!, connectionHandler!!)

            Executors.newSingleThreadExecutor().submit(connectedThread)

            return ConnectionStatus.CONNECTED
        }

        return ConnectionStatus.NOT_CONNECTED
    }

    fun getUpdatedArduinoConnectionStatus(): ConnectionStatus {
        val bluetoothStatus = bluetoothManager.getBluetoothStatus()

        return if (bluetoothStatus == BluetoothStatus.BT_PAIRED_WITH_ARDUINO
            && prefsManager.isArduinoConnected()
            && ifStillConnected()
        ) {
            ConnectionStatus.CONNECTED
        } else {
            prefsManager.setIsArduinoConnected(false)
            ConnectionStatus.NOT_CONNECTED
        }
    }

    private fun ifStillConnected(): Boolean {
        return try {
            mmSocket!!.outputStream.write(0)
            true
        } catch (e: IOException) {
            false
        }
    }

    /**
     * @return True if commands has been executed.
     */
    fun sendAndWaitForResponse(commandsString: String, afterWait: BiConsumer<Boolean, String>) {
        val outputStream = mmSocket!!.outputStream

        var message = ""
        try {
            outputStream.write(commandsString.encodeToByteArray())
        } catch (e: IOException) {
            message = "Arduino is NOT connected."
        }

        Log.i(MANAGE_CONN_TAG, "WAITING FOR RESPONSE")
        // Now, wait for a response or timeout
        afterWait.accept(
            responseSemaphore.tryAcquire(1, 10, TimeUnit.SECONDS),
            message
        )
    }

    private fun askToEnableBluetooth() {
        // HomeFragment observe this val so it will start activity that asks for turning bt on.
        // After enabling BT HomeFragment will call connectToArduino in view model, that will call
        // connectToArduino here.
        enableBluetoothByIntent.accept(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: ConnectionManager? = null

        fun getInstance(
            context: Context,
            enableBluetoothByIntent: Consumer<Intent>
        ): ConnectionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConnectionManager(
                    context,
                    enableBluetoothByIntent
                ).also {
                    INSTANCE = it
                }
            }
        }

        // ConnectionManager must be initialized first in HomeViewModel.
        // I can get the same connection manager because all things here are the same everywhere.
        fun getExistInstance(): ConnectionManager {
            return INSTANCE!!
        }
    }
}



