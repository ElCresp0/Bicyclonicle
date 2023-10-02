package pg.eti.bicyclonicle

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Consumer
import pg.eti.bicyclonicle.arduino_connection.enums.ArduinoResponse as ar
import pg.eti.bicyclonicle.preferences.SharedPreferencesManager
import pg.eti.bicyclonicle.arduino_connection.enums.BluetoothStatus
import pg.eti.bicyclonicle.arduino_connection.enums.ConnectionStatus

import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class LoadingScreen private constructor(
    context: Context,
    private val resources: Resources
) {
    private val alertDialog: AlertDialog

    init {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.loading_screen, null)

        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setCancelable(false) // Prevent dismissing by tapping outside

        alertDialog = alertDialogBuilder.create()
    }
    fun getShowedLoadingScreen(): AlertDialog {
        val screenWidth = resources.displayMetrics.widthPixels
        val dialogWidth = screenWidth / 3

        alertDialog.show()
        alertDialog.window?.setLayout(dialogWidth, dialogWidth)

        return alertDialog
    }

    companion object {
        @Volatile
        private var INSTANCE: LoadingScreen? = null

        fun getInstance(
            context: Context,
            resources: Resources
        ): LoadingScreen {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LoadingScreen(context, resources).also {
                    INSTANCE = it
                }
            }
        }
    }
}



