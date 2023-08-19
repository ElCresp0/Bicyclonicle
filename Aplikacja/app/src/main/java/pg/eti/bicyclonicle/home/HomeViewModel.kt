package pg.eti.bicyclonicle.home

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pg.eti.bicyclonicle.R
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
    private val getStringResource: (Int) -> String? = { stringResId ->
        context.value?.getString(stringResId)
    }

    private val _bondedDevices = MutableLiveData<List<String>>()
    val bondedDevices: LiveData<List<String>> = _bondedDevices

    fun setupArduinoConnection() {
        // Context will be not null here for sure.
        _bluetoothConnection.value = BluetoothConnection(context.value!!)

        val bluetoothStatus = _bluetoothConnection.value?.enableBluetooth()
        // Take one try to enable bluetooth if it's not
        if (bluetoothStatus != null && bluetoothStatus == BluetoothStatus.BT_DISABLED) {
            askToEnableBluetooth()
        }

        manageBluetoothStatus()
    }

    private fun askToEnableBluetooth() {
        // HomeFragment observe this val so it will start activity that asks for turning bt on.
        _enableBluetoothIntent.value = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    }

    fun manageBluetoothStatus() {
        val bluetoothStatus = _bluetoothConnection.value?.enableBluetooth()
        if (bluetoothStatus != null && bluetoothStatus != BluetoothStatus.BT_ENABLED) {
            _bluetoothStatusText.value = getStringResource(bluetoothStatus.stringResId)
            _bondedDevices.value = ArrayList()

        } else if (bluetoothStatus != null && bluetoothStatus == BluetoothStatus.BT_ENABLED) {
            _bluetoothStatusText.value = getStringResource(bluetoothStatus.stringResId)
            _bondedDevices.value = _bluetoothConnection.value?.getBondedDevices()

        } else {
            _bluetoothStatusText.value = getStringResource(R.string.bt_error)
            _bondedDevices.value = ArrayList()
        }
    }
}