package pg.eti.bicyclonicle.ui.app_settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AppSettingsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is app settings fragment"
    }
    val text: LiveData<String> = _text
}