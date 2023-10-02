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
import pg.eti.bicyclonicle.arduino_connection.services.ConnectionManager
import pg.eti.bicyclonicle.arduino_connection.services.MANAGE_CONN_TAG
import pg.eti.bicyclonicle.preferences.SharedPreferencesManager
import java.lang.Exception
import pg.eti.bicyclonicle.recording_settings.RecordingSingleSetting.Properties as settings


const val REC_SETT_TAG = "MANAGE_CONN"

class RecordingsSettingsViewModel(
    private val context: Context,
    private val preferenceScreen: PreferenceScreen,
    private val loadingScreen: LoadingScreen
) : ViewModel() {
    private val spm = SharedPreferencesManager.getInstance(context)
    private var previousSettings = listOf<RecordingSingleSetting>()
    private lateinit var connectionManager: ConnectionManager
    private var changedSettings = mutableMapOf<String, Boolean>()

    init {
        if (spm.isAppFirstStart()) {
            spm.setIsAppFirstStart(false)
        } else {
            getPreferencesFromSharedPrefs()
        }
        previousSettings = getCurrentSettings()
        setOnPreferenceChangeListeners()
        setMapForChangedPrefsCounting()
    }

    private fun getPreferencesFromSharedPrefs() {
        // TODO: ??
//        if (!checkIfArduinoConnected()) {
//            throw Exception("Arduino is NOT connected!")
//        }

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

    // TODO: if go to another fragment ask if he don't want to synchronize
    //  maybe there is a way to check if something has changed?

    // todo - why
    private fun checkIfArduinoConnected(): Boolean {
        return false
    }

    fun synchronizeSettings() {
        val currentSettings = getCurrentSettings()

        val commands = StringBuilder()
        currentSettings.forEach { setting -> commands.append(setting.getAsCommand()) }

        if (!::connectionManager.isInitialized) {
            connectionManager = ConnectionManager.getExistInstance()
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Non UI, long lasting operations should be made on other thread.
            var alertDialog: AlertDialog? = null
            viewModelScope.launch(Dispatchers.Main) {
                // UI on main.
                alertDialog = loadingScreen.getShowedLoadingScreen()
            }


            val executeAfterWait: (Boolean) -> Unit = { isExecuted ->
                Log.i(MANAGE_CONN_TAG, "IS EXECUTED: $isExecuted")
                alertDialog!!.dismiss()

                if (isExecuted) {
                    Log.e(REC_SETT_TAG, "COMMANDS EXECUTED")
                    synchronizeWithSharedPreferences(currentSettings)
                    enableSettings()
                    setIsEnableSynchronizeButton(false)

                } else {
                    Log.e(REC_SETT_TAG, "COMMANDS NOT EXECUTED")
                    rollbackPreferences()
                    disableSettings()
                    setIsEnableSynchronizeButton(true)
                }

                viewModelScope.launch(Dispatchers.Main) {
                    // UI on main.
                    Toast.makeText(context, "Synchronization status: $isExecuted",Toast.LENGTH_SHORT).show()
                }
            }

            try {
                connectionManager.sendAndWaitForResponse(
                    commands.toString(),
                    executeAfterWait
                )
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    // UI on main.
                    Toast.makeText(context, e.message.toString(),Toast.LENGTH_SHORT).show()
                }

                rollbackPreferences()
                disableSettings()
                setIsEnableSynchronizeButton(true)
            }
        }
    }

    private fun enableSettings() {
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

    private fun disableSettings() {
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

    private fun synchronizeWithSharedPreferences(settingsToSave: List<RecordingSingleSetting>) {
        settingsToSave.forEach { setting ->
            spm.saveSetting(setting.getProperties().key, setting.getValue())
        }
    }

    private fun rollbackPreferences() {
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

    private fun setIsEnableSynchronizeButton(isEnable: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            preferenceScreen.findPreference<Preference>(
                RecordingSingleSetting.Properties
                    .SYNCHRONIZE.key
            )!!.isEnabled = isEnable
        }
    }
}