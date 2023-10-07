package pg.eti.bicyclonicle.home

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pg.eti.bicyclonicle.LoadingScreen
import pg.eti.bicyclonicle.arduino_connection.enums.ConnectionStatus
import pg.eti.bicyclonicle.arduino_connection.services.ConnectionManager
import pg.eti.bicyclonicle.databinding.FragmentHomeBinding


const val HOME_VM_TAG = "HOME_VM_TAG"

class HomeViewModel : ViewModel() {
    // LiveData
    private val _bluetoothStatusText = MutableLiveData<String>()
    val bluetoothStatusText: LiveData<String> = _bluetoothStatusText

    private val _enableBluetoothIntent = MutableLiveData<Intent>()
    val enableBluetoothIntent: LiveData<Intent> = _enableBluetoothIntent
    val context = MutableLiveData<Context>()

    val loadingScreen = MutableLiveData<LoadingScreen>()

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

    fun checkArduinoConnection(binding: FragmentHomeBinding): Boolean {
        var status: ConnectionStatus = ConnectionStatus.CONNECTION_STATUS
        viewModelScope.launch(Dispatchers.Main) {
            status = connectionManager.getUpdatedArduinoConnectionStatus()
            _bluetoothStatusText.value = getStringResource(status.stringResId)
            binding.btnConnectToArduino.isEnabled = status != ConnectionStatus.CONNECTED
        }
        return status.stringResId == ConnectionStatus.CONNECTED.stringResId
    }

    /**
     * Results are put in LiveData.
     */
    fun connectToArduino(binding: FragmentHomeBinding) {
        val alertDialog = loadingScreen.value!!.getShowedLoadingScreen()
        viewModelScope.launch(Dispatchers.IO) {
            connectionManager.connectToArduino()
            alertDialog.dismiss()
            checkArduinoConnection(binding)
        }
    }
}