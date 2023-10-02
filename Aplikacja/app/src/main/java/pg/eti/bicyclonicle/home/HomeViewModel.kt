package pg.eti.bicyclonicle.home

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pg.eti.bicyclonicle.arduino_connection.services.ConnectionManager


const val HOME_VM_TAG = "HOME_VM_TAG"

class HomeViewModel : ViewModel() {
    // LiveData
    private val _bluetoothStatusText = MutableLiveData<String>()
    val bluetoothStatusText: LiveData<String> = _bluetoothStatusText

    private val _enableBluetoothIntent = MutableLiveData<Intent>()
    val enableBluetoothIntent: LiveData<Intent> = _enableBluetoothIntent
    val context = MutableLiveData<Context>()

    // Regular members.
    private val enableBluetoothIntentConsumer: (Intent) -> Unit = { intent ->
        _enableBluetoothIntent.value = intent
    }
    private lateinit var connectionManager: ConnectionManager
    private val getStringResource: (Int) -> String? = { stringResId ->
        context.value?.getString(stringResId)
    }


    fun initViewModel() {
        // This method needs to be called after passing context in LiveData in Fragment.
        if (context.value != null) {
            connectionManager = ConnectionManager.getInstance(
                context.value!!,
                enableBluetoothIntentConsumer
            )
        } else {
            Log.e(HOME_VM_TAG, "Context has not been passed!")
        }
    }

    /**
     * Results are put in LiveData.
     */
    fun connectToArduino() {
        _bluetoothStatusText.value =
            getStringResource(connectionManager.connectToArduino().stringResId)
    }

    fun disconnectToArduino() {
        // Terminate Bluetooth Connection and close app
        connectionManager.cancel()
        Log.i(HOME_FR_TAG, "Stopping connection.")
    }
}