package pg.eti.bicyclonicle.ui.recordings_library

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecordingsLibraryViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is recordings library fragment"
    }
    val text: LiveData<String> = _text
}