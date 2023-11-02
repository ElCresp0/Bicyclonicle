package pg.eti.bicyclonicle.recording_settings

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pg.eti.bicyclonicle.LoadingScreen
import pg.eti.bicyclonicle.arduino_connection.enums.ConnectionStatus
import pg.eti.bicyclonicle.arduino_connection.services.ConnectionManager
import pg.eti.bicyclonicle.arduino_connection.services.MANAGE_CONN_TAG
import pg.eti.bicyclonicle.preferences.SharedPreferencesManager
import pg.eti.bicyclonicle.recording_settings.RecordingSingleSetting.Properties as settings


const val REC_SETT_TAG = "MANAGE_CONN"

class RecordingsSettingsViewModel(
    val context: Context,
    private val preferenceScreen: PreferenceScreen,
    private val loadingScreen: LoadingScreen
) : ViewModel() {
    private val spm = SharedPreferencesManager.getInstance(context)
    private var previousSettings = listOf<RecordingSingleSetting>()
    private lateinit var connectionManager: ConnectionManager
    private var changedSettings = mutableMapOf<String, Boolean>()

    init {
        if (!::connectionManager.isInitialized) {
            connectionManager = ConnectionManager.getExistInstance()
        }

        if (spm.isAppFirstStart()) {
            spm.setIsAppFirstStart(false)
        } else {
            getPreferencesFromSharedPrefs()
        }
        previousSettings = getCurrentSettings()
        setOnPreferenceChangeListeners()
        setMapForChangedPrefsCounting()

        setIsEnableSynchronizeButton(checkArduinoConnection())
    }

    public fun getSpm(): SharedPreferencesManager {
        return spm
    }

    private fun getPreferencesFromSharedPrefs() {
        settings.entries.forEach { entry ->
            // Here must be all types of preferences but synchronize.
            when (entry.preferenceClass) {
                ListPreference::class.java -> {
                    preferenceScreen
                        .findPreference<ListPreference>(entry.key)!!
                        .value = spm.getSetting(entry.key)
                }

                CheckBoxPreference::class.java -> {
                    preferenceScreen.findPreference<CheckBoxPreference>(entry.key)!!
                        .isChecked.toString()
                    preferenceScreen
                        .findPreference<CheckBoxPreference>(entry.key)!!
                        .isChecked = spm.getSetting(entry.key).toBoolean()
                }

                else -> {}
            }
        }
    }

    fun checkArduinoConnection(): Boolean {
        return connectionManager.getUpdatedArduinoConnectionStatus().stringResId == ConnectionStatus.CONNECTED.stringResId
    }

    fun synchronizeSettings() {
        val currentSettings = getCurrentSettings()

        val commands = StringBuilder()
        currentSettings.forEach { setting -> commands.append(setting.getAsCommand()) }

        val rsvm = this

        viewModelScope.launch(Dispatchers.IO) {
            // Non UI, long lasting operations should be made on other thread.
//            var alertDialog: AlertDialog? = null
//            viewModelScope.launch(Dispatchers.Main) {
//                // UI on main.
//                alertDialog = loadingScreen.getShowedLoadingScreen()
//            }

            // robie obiekt dziedziczacy po thread z dwoma polami do ustawienia, przekazuje go tam, tam ustawiam i odpalam

            connectionManager.sendAndWaitForResponse(
                commands.toString(),
                ExecuteAfterWait(rsvm, currentSettings)
            )
        }
    }

    fun enableSettings() {
        viewModelScope.launch(Dispatchers.Main) {
            // UI on main.
            for (i in 0 until preferenceScreen.preferenceCount) {
                val preference = preferenceScreen.getPreference(i)

                if (!preference.isEnabled) {
                    preference.isEnabled = true
                }
            }
        }
    }

    fun disableSettings() {
        viewModelScope.launch(Dispatchers.Main) {
            // UI on main.
            for (i in 0 until preferenceScreen.preferenceCount) {
                val preference = preferenceScreen.getPreference(i)

                if (preference.isEnabled
                    && !preference.key.equals(settings.SYNCHRONIZE.key)
                ) {
                    preference.isEnabled = false
                }
            }
        }
    }

    fun rollbackPreferences() {
        viewModelScope.launch(Dispatchers.Main) {
            previousSettings.forEach { setting ->
                // Here must be all types of preferences but synchronize.
                when (setting.getProperties().preferenceClass) {
                    ListPreference::class.java -> preferenceScreen
                        .findPreference<ListPreference>(setting.getProperties().key)!!
                        .value = setting.getValue()

                    CheckBoxPreference::class.java -> preferenceScreen
                        .findPreference<CheckBoxPreference>(setting.getProperties().key)!!
                        .isChecked = setting.getValue().toBoolean()

                    else -> {}
                }
            }
        }
    }

    private fun setOnPreferenceChangeListeners() {
        settings.entries.forEach { entry ->
            preferenceScreen.findPreference<Preference>(entry.key)!!
                .setOnPreferenceChangeListener { preference, newValue ->
                    checkEnableSynchronization(preference, newValue.toString())
                    true
                }
        }
    }

    private fun setMapForChangedPrefsCounting() {
        settings.entries.forEach { entry ->
            changedSettings[entry.key] = false
        }
    }


    private fun getCurrentSettings(): List<RecordingSingleSetting> {
        val currentSettings = mutableListOf<RecordingSingleSetting>()

        settings.entries.forEach { entry ->
            // Here must be all types of preferences but synchronize.
            when (entry.preferenceClass) {
                ListPreference::class.java -> {
                    currentSettings.add(
                        RecordingSingleSetting(
                            entry,
                            preferenceScreen.findPreference<ListPreference>(entry.key)!!.value
                        )
                    )
                }

                CheckBoxPreference::class.java -> {
                    currentSettings.add(
                        RecordingSingleSetting(
                            entry,
                            preferenceScreen.findPreference<CheckBoxPreference>(entry.key)!!
                                .isChecked.toString()
                        )
                    )
                }

                else -> {}
            }
        }

        return currentSettings
    }

    private fun checkEnableSynchronization(preference: Preference, newValue: String) {
        previousSettings.forEach { setting ->
            if (setting.getProperties().key == preference.key) {
                // Here must be all types of preferences but synchronize.
                when (setting.getProperties().preferenceClass) {
                    ListPreference::class.java -> {
                        changedSettings[preference.key] = setting.getValue() != newValue
                    }

                    CheckBoxPreference::class.java -> {
                        changedSettings[preference.key] = setting.getValue() != newValue
                    }

                    else -> {}
                }
                checkIsEnableSynchronizeButton()
            }
        }
    }

    private fun checkIsEnableSynchronizeButton() {
        Log.i(REC_SETT_TAG, changedSettings.toString())
        changedSettings.entries.forEach { entry ->
            if (entry.value) {
                setIsEnableSynchronizeButton(true)
                return
            }
        }

        setIsEnableSynchronizeButton(false)
    }

    fun setIsEnableSynchronizeButton(isEnable: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            preferenceScreen.findPreference<Preference>(
                RecordingSingleSetting.Properties
                    .SYNCHRONIZE.key
            )!!.isEnabled = isEnable
        }
    }
}