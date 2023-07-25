package pg.eti.bicyclonicle.ui.recording_settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecordingSettingsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is recording settings fragment"
    }
    val text: LiveData<String> = _text
}