package pg.eti.bicyclonicle

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
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
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingPermission")
    private fun socketReaderWriter(socket: BluetoothSocket) {
        while (true) {
            if (bufferWriter.isNotEmpty()) {
                try {
                    if (!socket.isConnected)
                        socket.connect()

                    var c = bufferWriter.take()
                    socket.outputStream.write(byteArrayOf(c))
//                    Log.i("BT", "sent: ${c.toInt().toChar()}")
                }
                catch (e: IOException) {
                    Log.e("BT", "BT err:", e)
                }
            }
            if (socket.isConnected && socket.inputStream.available() != 0) {
//                val c = socket.inputStream.read().toChar()
//                if (c.isLetter() || specialChars.contains(c)) {
//                    bufferReader.put(c.toString())
//                    Log.i("BT", "received char: ${bufferReader.last()}")
//                }
                bufferReader.put(socket.inputStream.read().toByte())
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
                socketReaderWriter(socket)
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

    private inner class SendBlueToothFile(fileStream: InputStream) : Thread() {
        private val fileStream = fileStream

        public override fun run() {
            sendBlueToothMessage("sending_file;".toByteArray())
            sleep(100)
            if (!bufferReader.containsAll("sending_ok;".toByteArray().asList())) {
                Log.w("BT - send file", "no response from ESP")
                return
            }
            bufferReader.clear()
            while (fileStream.available() != 0) {
                sendBlueToothMessage(byteArrayOf(fileStream.read().toByte()))
            }
            sendBlueToothMessage("sending_finished;".toByteArray())
            Log.i("BT - file transfer", "sending finished")
        }
    }

    private inner class ReceiveBlueToothFile() : Thread() {
        public override fun run() {
            sleep(100)
            if (!bufferReader.containsAll("request_ok;".toByteArray().asList())) {
                Log.w("BT - receive file", "no response from ESP")
                return
            }
            bufferReader.clear()
            val fos: FileOutputStream = openFileOutput("test.mp4", Context.MODE_PRIVATE)
            var byteDeque: ArrayDeque<Byte> = ArrayDeque<Byte>()
            val finishedString: ByteArray = "sending_finished".toByteArray()
            while (byteDeque.size < finishedString.size || byteDeque.takeLast(finishedString.size).toByteArray() != finishedString) {
                byteDeque.addLast(bufferWriter.take())
            }
            byteDeque.dropLast(finishedString.size)
            fos.write(byteDeque.toByteArray())
            fos.close()
            Log.i("BT alert", "received file")
        }
    }




    @SuppressLint("MissingPermission")
    private fun sendBlueToothMessage(msg: ByteArray): Int {
        if (!threadInitialized) {
            val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter!!
//            val alert: AlertDialog = AlertDialog.Builder(this).create()
//            alert.setTitle("BT alert")
//            alert.setMessage("BT Connection failed ;<")
            if (bluetoothAdapter == null) {
                Log.e("BT", "Bluetooth connection failed")
                Log.i("BT", "Device doesn't support Bluetooth")
            }
            if (!bluetoothAdapter.isEnabled) {
                Log.e("BT", "Bluetooth connection failed")
                Log.w("BT", "bt adapter not enabled")
                return -1
            }
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            var espBluetoothDevice: BluetoothDevice? = null
            pairedDevices.forEach {
                if ( it.name == "ESP32test" ) {
                    espBluetoothDevice = it
                }
            }
            if (espBluetoothDevice == null) {
                Log.e("BT", "Bluetooth connection failed")
                Log.w("BT", "BT device = null")
                return -1
            }
            val connectThread: ConnectThread = ConnectThread(espBluetoothDevice!!, bluetoothAdapter)
            connectThread.start()
            threadInitialized = true
        }
        Log.i("BT", "sending: $msg")
        for (byte in msg) {
            bufferWriter.put(byte)
        }
        return 1
    }

//    private fun receiveBlueToothMessage(): String {
//
//
//        bufferReader.
//        return "1"
//    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectThread: ConnectThread
    private var threadInitialized: Boolean = false
    private var bufferWriter: BlockingQueue<Byte> = LinkedBlockingQueue()
    private var bufferReader: BlockingQueue<Byte> = LinkedBlockingQueue()
    private val specialChars: CharArray = charArrayOf(';', ':')

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
            var prefs = getPreferences(0).all
            prefs = prefs.plus(Pair("key1", 25))
            prefs = prefs.plus(Pair("key2", "lol"))
            prefs = prefs.plus(Pair("key3", 3.14))
            Log.i("BT", "sending $prefs")
            for (pref in prefs) {
                sendBlueToothMessage("${pref.key}:${pref.value};".toByteArray())
            }
        }

        val sendVideoButton: Button = findViewById<Button>(R.id.send_video)
        sendVideoButton.setOnClickListener {
            val vid: InputStream = resources.openRawResource(R.raw.test_png)
            val sendBlueToothFile: SendBlueToothFile = SendBlueToothFile(vid)
            sendBlueToothFile.start()
            Log.i("BT", "sending file")
            val alert: AlertDialog = AlertDialog.Builder(this).create()
            alert.setTitle("BT alert")
            alert.setMessage("sending file")
            alert.show()
        }

        val receiveVideoButton: Button = findViewById<Button>(R.id.receive_video)
        receiveVideoButton.setOnClickListener {
            sendBlueToothMessage("requesting_f;".toByteArray())
            val receiveBlueToothFile: ReceiveBlueToothFile = ReceiveBlueToothFile()
            receiveBlueToothFile.start()

        }






        Log.i("FS", this.filesDir.absolutePath)

        val fos: FileOutputStream = openFileOutput("test_ktory_nikogo_nie_obraza3.txt", Context.MODE_PRIVATE)
        fos.write("test string hello".toByteArray())
        fos.close()
        Log.i("BT alert", "wrote to file")

        Log.i("FS", getFileStreamPath("test_ktory_nikogo_nie_obraza3.txt").absolutePath)

//        val fos2: FileOutputStream = FileOutputStream(File(this.filesDir.absolutePath + "test_ktory_nikogo_nie_obraza2.txt"))
//        fos2.write("test string".toByteArray())
//        fos2.close()
//        Log.i("BT alert", "wrote to file2")





        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Try to make connection with recorder. TODO
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