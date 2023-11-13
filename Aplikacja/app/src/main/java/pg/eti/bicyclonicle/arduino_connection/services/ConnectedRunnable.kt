package pg.eti.bicyclonicle.arduino_connection.services

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.util.Log
import pg.eti.bicyclonicle.arduino_connection.enums.ConnectionStatus
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Thread.sleep
import kotlin.math.min

const val CONN_THREAD_TAG = "CONN_RUNNABLE"
const val BUFF_SIZE = 1024

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
        val buffer = ByteArray(BUFF_SIZE + 1) // buffer store for the stream
        var bytes = 0 // bytes returned from read()
        var byte: Byte
        var char: Char
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                /*
                Read from the InputStream from Arduino until termination character is reached.
                Then send the whole String message to GUI Handler.
                 */
                // read 1 byte, expected messages are:
                //  > sending;
                //  > failed;
                //  > executed;
                 
                byte = mmInStream!!.read().toByte()
                char = byte.toUInt().toInt().toChar()
                if (! (char.isLetterOrDigit() || char in "_-/:;,.")) continue
                buffer[bytes] = byte
                var readMessage: String
// nie ja tego nie przezyje.......
                if (buffer[bytes] == ';'.code.toByte()) {
                    readMessage = String(buffer, 0, bytes)
                    Log.i("BT", "received: $readMessage")
                    if (readMessage == "executed") {
                        // release semaphor in ConnectionManager
                        connectionHandler.obtainMessage(
                            ConnectionStatus.MESSAGE_READ.ordinal,
                            readMessage
                        ).sendToTarget()
                    }
                    else if ("sending" in readMessage) {
                        connectionHandler.obtainMessage(
                            ConnectionStatus.MESSAGE_READ.ordinal,
                            readMessage
                        ).sendToTarget()
                        val params = readMessage.split(":")
                        receiveBlueToothFile(params[1], params[2].toInt())
                        
                    }
                    else if ("sdcard" in readMessage) {
                        // means message contains ls result in a form of comma separated list of paths
                        // send it to the handler
                        connectionHandler.obtainMessage(
                            ConnectionStatus.MESSAGE_READ.ordinal,
                            readMessage
                        ).sendToTarget()
                        // change paths to names
                        
                        // in handler perform the following to obtain a list of names
                        // readMessage = readMessage.replace("/[^,]*/".toRegex(), "")
                        // val files = readMessage.split(",")
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

    private fun receiveBlueToothFile(name: String, size: Int) {
        Log.i("BT", "receiving file: $name")
        var count: Int = 0
        var waitOnce = false
        var buffer = ByteArray(BUFF_SIZE)
        val bufferInputStream = mmInStream?.buffered(BUFF_SIZE)
        val fos: FileOutputStream = FileOutputStream(name.split("/").last())
        while (count < size) {
            if (bufferInputStream!!.available() > 0) {
                waitOnce = false
                // read max size or if there's less than that left in the stream, read the diff
                val tmpCount = bufferInputStream.read(buffer, 0, min(size - BUFF_SIZE, BUFF_SIZE))
                fos.write(buffer, 0, tmpCount)
                count += tmpCount
            }
            else if (waitOnce == false) {
                sleep(100)
                waitOnce = true
            }
            else {
                // rollback due to errors
                Log.e("BT", "Couldn't receive the file, rolling back")
                // close the file
                connectionHandler.obtainMessage(
                            ConnectionStatus.MESSAGE_READ.ordinal,
                            "failed"
                        ).sendToTarget()
                break   
            }
        }
        fos.close()
        // connectionHandler.obtainMessage(
        //                     ConnectionStatus.MESSAGE_READ.ordinal,
        //                     "executed"
        //                 ).sendToTarget()


        // BYTEDEQUE IDEA BELOW MIGHT NOT BE THAT STUPID
        // if (!bufferReader.containsAll("request_ok;".toByteArray().asList())) {
        //     Log.w("BT - receive file", "no response from ESP")
        //     return
        // }
        // bufferReader.clear()
        // var byteDeque: ArrayDeque<Byte> = ArrayDeque<Byte>()
        // val finishedString: ByteArray = "sending_finished".toByteArray()
        // while (byteDeque.size < finishedString.size || byteDeque.takeLast(finishedString.size).toByteArray() != finishedString) {
        //     byteDeque.addLast(bufferWriter.take())
        // }
        // byteDeque.dropLast(finishedString.size)
        // fos.write(byteDeque.toByteArray())
        // fos.close()
        Log.i("BT alert", "received file")
    }
}