package pg.eti.bicyclonicle.home

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pg.eti.bicyclonicle.R
import pg.eti.bicyclonicle.SharedPreferencesManager
import pg.eti.bicyclonicle.services.bluetooth_adapter.BluetoothConnection
import pg.eti.bicyclonicle.services.bluetooth_adapter.BluetoothStatus

class HomeViewModel : ViewModel() {
    private val _bluetoothStatusText = MutableLiveData<String>()
    val bluetoothStatusText: LiveData<String> = _bluetoothStatusText

    private val _enableBluetoothIntent = MutableLiveData<Intent>()
    val enableBluetoothIntent: LiveData<Intent> = _enableBluetoothIntent

    val context = MutableLiveData<Context>()

    private val _bluetoothConnection = MutableLiveData<BluetoothConnection>()

    // Lambda that use application got from HomeFragment to get access to getString.
    private val _getStringResource: (Int) -> String? = { stringResId ->
        context.value?.getString(stringResId)
    }

    private val _bondedDevices = MutableLiveData<List<String>>()
    val bondedDevices: LiveData<List<String>> = _bondedDevices

    private val _prefs = MutableLiveData<SharedPreferencesManager>()

    fun initViewModel() {
        // Context will be not null here for sure.
        _prefs.value = SharedPreferencesManager(context.value!!)
        _bluetoothConnection.value = BluetoothConnection(context.value!!)
    }

    fun setupArduinoConnection() {
        val bluetoothStatus = _bluetoothConnection.value?.enableBluetooth()
        // Take one try to enable bluetooth if it's not
        if (bluetoothStatus != null && bluetoothStatus == BluetoothStatus.BT_DISABLED) {
            askToEnableBluetooth()
        }

        updateBluetoothStatus()
    }

    private fun askToEnableBluetooth() {
        // HomeFragment observe this val so it will start activity that asks for turning bt on.
        _enableBluetoothIntent.value = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    }

    // TODO: when device not connected to arduino then recordings library should check it
    //  and show msg (and sent the status here) <- is it accomplished by the class
    //  BluetoothConnection?
    fun updateBluetoothStatus() {
        var bluetoothStatus = _bluetoothConnection.value?.enableBluetooth()
        _prefs.value!!.putPref(
            SharedPreferencesManager.Prefs.IS_ARDUINO_CONNECTED.name,
            false
        )

        if (bluetoothStatus != null && bluetoothStatus != BluetoothStatus.BT_ENABLED_NOT_CONNECTED) {
            _bluetoothStatusText.value = _getStringResource(bluetoothStatus.stringResId)
            _bondedDevices.value = ArrayList()

        } else if (bluetoothStatus != null && bluetoothStatus == BluetoothStatus.BT_ENABLED_NOT_CONNECTED) {
            if (_bluetoothConnection.value!!.isConnectedToArduino()) {
                bluetoothStatus = BluetoothStatus.BT_CONNECTED_TO_ARDUINO
                _bondedDevices.value = ArrayList()
                _prefs.value!!.putPref(
                    SharedPreferencesManager.Prefs.IS_ARDUINO_CONNECTED.name,
                    true
                )
            } else {
                bluetoothStatus = BluetoothStatus.BT_ENABLED_NOT_CONNECTED
                _bondedDevices.value = _bluetoothConnection.value?.getBondedDevices()
            }
            _bluetoothStatusText.value = _getStringResource(bluetoothStatus.stringResId)

        } else {
            _bluetoothStatusText.value = _getStringResource(R.string.bt_error)
            _bondedDevices.value = ArrayList()
        }
    }
}