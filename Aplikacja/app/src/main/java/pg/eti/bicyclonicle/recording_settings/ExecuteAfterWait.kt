package pg.eti.bicyclonicle.recording_settings

import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pg.eti.bicyclonicle.arduino_connection.services.MANAGE_CONN_TAG
import pg.eti.bicyclonicle.preferences.SharedPreferencesManager

class ExecuteAfterWait : Runnable {
    var isExecuted: Boolean = true
    var message: String = ""
    var alertDialog: AlertDialog
    var currentSettings: List<RecordingSingleSetting>
    var rsvm: RecordingsSettingsViewModel

    public fun setProperties(b: Boolean, s: String) {
        message = s
        isExecuted = b
    }

    private fun synchronizeWithSharedPreferences(settingsToSave: List<RecordingSingleSetting>) {
        settingsToSave.forEach { setting ->
            rsvm?.getSpm()?.saveSetting(setting.getProperties().key, setting.getValue())
        }
    }

    public constructor(recordingsSettingsViewModel: RecordingsSettingsViewModel, currentSettings: List<RecordingSingleSetting>, alertDialog: AlertDialog) {
        this.rsvm = recordingsSettingsViewModel
        this.currentSettings = currentSettings
        this.alertDialog = alertDialog
    }

    public override fun run()
    {
        Log.i(MANAGE_CONN_TAG, "IS EXECUTED: $isExecuted")
        alertDialog!!.dismiss()

        if (isExecuted) {
            Log.e(REC_SETT_TAG, "COMMANDS EXECUTED")
            currentSettings?.let { synchronizeWithSharedPreferences(it) }
            rsvm?.enableSettings()
            rsvm?.setIsEnableSynchronizeButton(false)

        } else {
            Log.e(REC_SETT_TAG, "COMMANDS NOT EXECUTED")

            rsvm?.rollbackPreferences()
            rsvm?.disableSettings()
            rsvm?.setIsEnableSynchronizeButton(true)
        }

        rsvm?.viewModelScope?.launch(Dispatchers.Main) {
            // UI on main.
            if (message.isEmpty()) {
                Toast.makeText(rsvm?.context, "Synchronization status: $isExecuted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(rsvm?.context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}