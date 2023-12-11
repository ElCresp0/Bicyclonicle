package pg.eti.bicyclonicle.arduino_connection.services

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.util.Log
import pg.eti.bicyclonicle.arduino_connection.enums.ConnectionStatus
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Thread.sleep
import java.time.Instant
import java.util.Date
import kotlin.math.min

const val CONN_THREAD_TAG = "CONN_RUNNABLE"
const val BUFF_SIZE = 1024
const val FILE_TRANSFER_TIMEOUT = 400
const val FILE_TRANSFER_WAIT_TIME = 20

class ConnectedThread(
    private val mmSocket: BluetoothSocket,
    private val connectionHandler: Handler
) : Runnable {

    private val mmInStream: InputStream?
    private val mmOutStream: OutputStream?
    private var receiveFileFlag: Boolean = false

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
        val buffer = ByteArray(BUFF_SIZE + 1) // buffer store for the stream
        var bytes = 0 // bytes returned from read()
        var byte: Byte
        var char: Char
        // Keep listening to the InputStream until an exception occurs
        while (true) if (!receiveFileFlag) {
            try {
                /*
                Read from the InputStream from Arduino until termination character is reached.
                Then send the whole String message to GUI Handler.
                 */
                // read 1 byte, expected messages are:
                //  > sending:<file_path>:<file_size>;
                //  > <file_path1>,<file_path2>,[...];
                //  > failed;
                //  > executed;
                 
                byte = mmInStream!!.read().toByte()
                char = byte.toUInt().toInt().toChar()
                if (! (char.isLetterOrDigit() || char in "_-/:;,.")) continue
                buffer[bytes] = byte
                var readMessage: String
                if (buffer[bytes] == ';'.code.toByte()) {
                    readMessage = String(buffer, 0, bytes)
                    Log.i("BT", "received: $readMessage")
                    if (readMessage == "executed") {
                        // release semaphor in ConnectionManager
                        if (!receiveFileFlag) {
                            connectionHandler.obtainMessage(
                                ConnectionStatus.MESSAGE_READ.ordinal,
                                readMessage
                            ).sendToTarget()
                        }
                        receiveFileFlag = false;
                    }
                    else if ("sending" in readMessage) {
                        connectionHandler.obtainMessage(
                            ConnectionStatus.MESSAGE_READ.ordinal,
                            readMessage
                        ).sendToTarget()
                        receiveFileFlag = true
                        // it's receiver's responsibility to receive the file and unset the flag

                        // val params = readMessage.split(":")
                        // receiveBlueToothFile(params[1], params[2].toInt())
                        
                    }
                    else if ("avi" in readMessage || "mp4" in readMessage) {
                        // means message contains ls result in a form of comma separated list of paths
                        // send it to the handler for further processing
                        connectionHandler.obtainMessage(
                            ConnectionStatus.MESSAGE_READ.ordinal,
                            readMessage
                        ).sendToTarget()
                    }

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

    public fun receiveBlueToothFile(name: String, size: Int, context: Context): String {
        Log.i("BT", "receiving file: $name")
        var count: Int = 0
        var available: Int
        var buffer = ByteArray(BUFF_SIZE + 1)
        val startDate: Date = Date.from(Instant.now())
        var lastCheck = Date.from(Instant.now())
        val f = File(context.filesDir.absolutePath + "/" + name.split("/").last())
        var fos: FileOutputStream? = null
        try {
//            TODO: register key event when user wants to stop the transfer
            Log.i(
                CONN_THREAD_TAG,
                "File: ${f.name}, exists: ${f.exists()}, expectedSize: $size, absPath: ${f.absolutePath}"
            )
            fos = context.openFileOutput(f.name, Context.MODE_APPEND)
            Log.i(CONN_THREAD_TAG, "opened FileOutputStream")
            while (count < size) {
                available = mmInStream!!.available()
                if (available > 0) {
                    val tmpCount = mmInStream.read(buffer, 0, min(BUFF_SIZE, size - count))
                    fos.write(buffer, 0, tmpCount)
                    count += tmpCount
                    lastCheck = Date.from(Instant.now())
                } else {
                    if (Date.from(Instant.now()).time - lastCheck.time > FILE_TRANSFER_TIMEOUT) {
                        throw IOException("Error downloading a file")
                    }
                    sleep(FILE_TRANSFER_WAIT_TIME.toLong())
                }
            }
            fos.close()
            receiveFileFlag = false
            Log.i("BT alert", "received file")
            Log.i(CONN_THREAD_TAG, "it took: ${(lastCheck.time - startDate.time) / 1000.0} seconds")
        }
        catch(e: Exception) {
            write("error;")
            fos?.close()
            Log.e(
                CONN_THREAD_TAG,
                "not getting bytes from ESP -> reverting receiveFile"
            )
            e.printStackTrace()
            Log.e(CONN_THREAD_TAG, "error message: ${e.message}")
            context.deleteFile(f.name)
            fos = context.openFileOutput(f.name, Context.MODE_PRIVATE)
            fos.close()
            Log.d(CONN_THREAD_TAG, "after clearing file")
            return "BT - failed to download the file"
        }
        return "BT - downloaded file successfully"
    }

    public fun unsetReceiveFileFlag() {
        receiveFileFlag = false
    }
}