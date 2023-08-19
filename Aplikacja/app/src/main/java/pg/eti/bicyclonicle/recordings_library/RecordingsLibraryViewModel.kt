package pg.eti.bicyclonicle.recordings_library

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pg.eti.bicyclonicle.SharedPreferencesManager
import pg.eti.bicyclonicle.services.bluetooth_adapter.BluetoothStatus

class RecordingsLibraryViewModel : ViewModel() {

    private val _isArduinoConnectedText = MutableLiveData<String>()
    val isArduinoConnectedText: LiveData<String> = _isArduinoConnectedText

    val context = MutableLiveData<Context>()

    private val _prefs = MutableLiveData<SharedPreferencesManager>()

    // Lambda that use application got from HomeFragment to get access to getString.
    private val _getStringResource: (Int) -> String? = { stringResId ->
        context.value?.getString(stringResId)
    }

    fun initViewModel() {
        // Context will be not null here for sure.
        _prefs.value = SharedPreferencesManager(context.value!!)
    }

    fun checkIfArduinoConnected() {
        val isArduinoConnectedPrefResult = _prefs.value!!.getPref(
            SharedPreferencesManager.Prefs
                .IS_ARDUINO_CONNECTED.name, false
        )
        if (isArduinoConnectedPrefResult) {
            _isArduinoConnectedText.value = _getStringResource(
                BluetoothStatus
                    .BT_CONNECTED_TO_ARDUINO.stringResId
            )
        } else {
            _isArduinoConnectedText.value = _getStringResource(
                BluetoothStatus
                    .BT_NOT_CONNECTED_TO_ARDUINO.stringResId
            )
        }
    }
}