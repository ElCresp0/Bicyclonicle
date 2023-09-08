package pg.eti.bicyclonicle

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import pg.eti.bicyclonicle.databinding.ActivityMainBinding
import java.io.IOException
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingPermission")
    private fun socketWriter(socket: BluetoothSocket) {
        while (true) {
            if (buffer.isNotEmpty()) {
                try {
                    if (!socket.isConnected)
                        socket.connect()

                    var c = buffer.take()
                    socket.outputStream.write(c.encodeToByteArray())
                    Log.i("BT", "success: $c")
                }
                catch (e: IOException) {
                    Log.e("BT", "BT err:", e)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice, bluetoothAdapter: BluetoothAdapter) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
        }
        private val bluetoothAdapter = bluetoothAdapter

        public override fun run() {
            Log.i("BT", "starting bt connect thread")
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socketWriter(socket)
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("BT", "Could not close the client socket", e)
            }
            threadInitialized = false
        }
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectThread: ConnectThread
    private var threadInitialized: Boolean = false
    private var buffer: BlockingQueue<String> = LinkedBlockingQueue()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_recording_library, R.id
                    .navigation_recording_settings, R.id.navigation_app_settings
            )
        )

        val sendConfigButton: Button = findViewById<Button>(R.id.send_config_button)
        sendConfigButton.setOnClickListener {

            if (!threadInitialized) {
                val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
                val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter!!
                val alert: AlertDialog = AlertDialog.Builder(this).create()
                alert.setTitle("BT alert")
                alert.setMessage("BT Connection failed ;<")
                if (bluetoothAdapter == null) {
                    alert.show()
                    Log.i("BT", "Device doesn't support Bluetooth")
                }
                if (!bluetoothAdapter.isEnabled) {
                    alert.show()
                    Log.w("BT", "bt adapter not enabled")
                    return@setOnClickListener
                }
                val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
                var espBluetoothDevice: BluetoothDevice? = null
                pairedDevices.forEach {
                    if ( it.name == "ESP32test" ) {
                        espBluetoothDevice = it
                    }
                }
                if (espBluetoothDevice == null) {
                    alert.show()
                    Log.w("BT", "BT device = null")
                    return@setOnClickListener
                }
                val connectThread: ConnectThread = ConnectThread(espBluetoothDevice!!, bluetoothAdapter)
                connectThread.start()
                threadInitialized = true
            }
            var prefs = getPreferences(0).all
            prefs = prefs.plus(Pair("key1", 25))
            prefs = prefs.plus(Pair("key2", "lol"))
            prefs = prefs.plus(Pair("key3", 3.14))
            Log.i("BT", "sending $prefs")
            for (pref in prefs) {
                Log.i("BT", "sending: ${pref.key}:${pref.value};")
                buffer.put("${pref.key}:${pref.value};")
            }
        }

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Try to make connection with recorder.
        // Whole class that will handle connection and update all data.
        // Settings should be put in shared preferences.
    }

    override fun onDestroy() {
        super.onDestroy()
        if (threadInitialized) {
            connectThread.cancel()
            connectThread.join()
            Log.i("BT", "connect thread joined")
        }
    }
}