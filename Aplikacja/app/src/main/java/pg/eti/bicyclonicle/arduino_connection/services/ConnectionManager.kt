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
import androidx.core.util.Consumer
import pg.eti.bicyclonicle.arduino_connection.enums.ArduinoResponse as ar
import pg.eti.bicyclonicle.preferences.SharedPreferencesManager
import pg.eti.bicyclonicle.arduino_connection.enums.BluetoothStatus
import pg.eti.bicyclonicle.arduino_connection.enums.ConnectionStatus

import java.io.IOException
// import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer

const val MANAGE_CONN_TAG = "MANAGE_CONN"

// todo: permissions
// todo: what if arduino will be disconnected after connection
//  then all modules want to connect
@SuppressLint("MissingPermission")
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
        "Bicyclonicle"
    )
    private var wasAskedToEnableBt = false

    private val responseSemaphore = Semaphore(0)
    private var receivedMessage: String = ""

    // todo: permissions
    @SuppressLint("MissingPermission")
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
            //todo: msg
            Log.e(MANAGE_CONN_TAG, "You need to pair arduino!")
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
        }
        return ConnectionStatus.NOT_CONNECTED
    }

    private fun setupSocket(arduinoDevice: BluetoothDevice): BluetoothSocket? {
        var tmpSocket: BluetoothSocket? = null
        val btSppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        try {
            tmpSocket = arduinoDevice.createRfcommSocketToServiceRecord(btSppUuid)
        } catch (e: IOException) {
            Log.e(MANAGE_CONN_TAG, "Socket's create() method failed", e)
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
                        receivedMessage = msg.obj.toString()
                        Log.i(MANAGE_CONN_TAG, "ARDUINO_MESSAGE: $receivedMessage")
                        if (receivedMessage == "executed")
                            responseSemaphore.release()
                        else if (receivedMessage == "failed")
                            Log.e("BT", "received message: \"failed\"")
                        else if ("sending" in receivedMessage)
                            responseSemaphore.release()
                        else if ("avi" in receivedMessage || "mp4" in receivedMessage)
                            responseSemaphore.release()
                    }
                }
            }
        }
    }

    private fun startConnection(): ConnectionStatus {
        // Cancel discovery because it otherwise slows down the connection.
        if (bluetoothAdapter == null) {
            Log.e(MANAGE_CONN_TAG, "BluetoothAdapter is null!")
            return ConnectionStatus.NOT_CONNECTED
        }

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

    // TODO: all tests for connectivity:
    //  connection lost in middle of operation etc.
    /**
     * @return True if commands has been executed.
     */
    fun sendAndWaitForResponse(commandsString: String, afterWait: BiConsumer<Boolean, String>) {
        val outputStream = mmSocket!!.outputStream

        // var message = ""
        receivedMessage = ""
        try {
            outputStream.write(commandsString.encodeToByteArray())
        } catch (e: IOException) {
            // message = "Arduino is NOT connected."
            receivedMessage = "Arduino is NOT connected."
        }

        Log.i(MANAGE_CONN_TAG, "WAITING FOR RESPONSE")
        // Now, wait for a response or timeout
        // 100$ question: is the received message up to date?
        afterWait.accept(
            responseSemaphore.tryAcquire(1, 10, TimeUnit.SECONDS),
            // message
            receivedMessage
        )
    }

    public fun receiveFileInConnectedThread(name: String, size: Int, context: Context): String? {
        return connectedThread?.receiveBlueToothFile(name, size, context)
    }

    private fun askToEnableBluetooth() {
        // HomeFragment observe this val so it will start activity that asks for turning bt on.
        // After enabling BT HomeFragment will call connectToArduino in view model, that will call
        // connectToArduino here.
        enableBluetoothByIntent.accept(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    // Closes the client socket and causes the thread to finish.
    fun cancel() {
        try {
            mmSocket?.close()
            connectedThread?.cancel()
            prefsManager.setIsArduinoConnected(false)
        } catch (e: IOException) {
            Log.e(MANAGE_CONN_TAG, "Could not close the client socket", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ConnectionManager? = null

        fun getInstance(
            context: Context,
            enableBluetoothByIntent: Consumer<Intent>,
        ): ConnectionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConnectionManager(context, enableBluetoothByIntent).also {
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



