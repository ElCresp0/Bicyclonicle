package pg.eti.bicyclonicle.recordings_library

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pg.eti.bicyclonicle.arduino_connection.services.ConnectionManager

const val REC_LIB_VM_TAG = "REC_LIB_VM_TAG"

class RecordingsLibraryViewModel : ViewModel() {
    // LiveData
    private val _isArduinoConnectedText = MutableLiveData<String>()
    val isArduinoConnectedText: LiveData<String> = _isArduinoConnectedText

    val context = MutableLiveData<Context>()

    // Regular members.
    private lateinit var connectionManager: ConnectionManager
    private val _getStringResource: (Int) -> String? = { stringResId ->
        context.value?.getString(stringResId)
    }


    fun initViewModel() {
        // This method needs to be called after passing context in LiveData in Fragment.
        if (context.value != null) {
            connectionManager = ConnectionManager.getExistInstance()
        } else {
            Log.e(REC_LIB_VM_TAG, "Context has not been passed!")
        }
    }


    fun checkArduinoConnection() {
        _isArduinoConnectedText.value = _getStringResource(
            connectionManager.getUpdatedArduinoConnectionStatus().stringResId
        )
    }
}