package pg.eti.bicyclonicle.recordings_library

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

const val REC_LIB_VM_TAG = "REC_LIB_VM_TAG"

class RecordingsLibraryViewModel : ViewModel() {
    // LiveData
    private val _isArduinoConnectedText = MutableLiveData<String>()
    val isArduinoConnectedText: LiveData<String> = _isArduinoConnectedText

    private var showVideoFlag = false
    private lateinit var videoUri: Uri

    fun ifShowVideo(): Boolean {
        return showVideoFlag
    }

    fun resetShowVideoFlag() {
        showVideoFlag = false
    }

    fun getVideoUri(): Uri {
        return videoUri
    }

    fun setupSingleVideo(
        uri: Uri
    ) {
        videoUri = uri
        showVideoFlag = true
    }
}