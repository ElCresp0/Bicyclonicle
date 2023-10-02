package pg.eti.bicyclonicle.arduino_connection.services

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import pg.eti.bicyclonicle.arduino_connection.enums.ConnectionStatus
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

const val CONN_THREAD_TAG = "CONN_RUNNABLE"

class ConnectedThread(
    private val mmSocket: BluetoothSocket,
    private val connectionHandler: Handler
) : Runnable {

    private val mmInStream: InputStream?
    private val mmOutStream: OutputStream?

    init {
        var tmpIn: InputStream? = null
        var tmpOut: OutputStream? = null

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = mmSocket.inputStream
            tmpOut = mmSocket.outputStream
        } catch (e: IOException) {
            Log.e(CONN_THREAD_TAG, "Something went wrong with assigning sockets.", e)
        }

        mmInStream = tmpIn
        mmOutStream = tmpOut
    }

    @Synchronized
    override fun run() {
        val buffer = ByteArray(1024) // buffer store for the stream
        var bytes = 0 // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                /*
                Read from the InputStream from Arduino until termination character is reached.
                Then send the whole String message to GUI Handler.
                 */
                buffer[bytes] = mmInStream!!.read().toByte()
                var readMessage: String
                if (buffer[bytes] == 'q'.code.toByte()) {
                    readMessage = String(buffer, 0, bytes)
                    connectionHandler.obtainMessage(
                        ConnectionStatus.MESSAGE_READ.ordinal,
                        readMessage
                    )
                        .sendToTarget()

                    bytes = 0
                } else {
                    bytes++
                }
            } catch (e: IOException) {
                e.printStackTrace()
                break
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    fun write(input: String) {
        val bytes = input.toByteArray() //converts entered String into bytes
        try {
            mmOutStream!!.write(bytes)
        } catch (e: IOException) {
            Log.e(CONN_THREAD_TAG, "Unable to send message", e)
        }
    }

    /* Call this from the main activity to shutdown the connection */
    fun cancel() {
        try {
            mmSocket.close()
        } catch (e: IOException) {
            Log.e(CONN_THREAD_TAG, "Connection haven't been closed.", e)
        }
    }
}