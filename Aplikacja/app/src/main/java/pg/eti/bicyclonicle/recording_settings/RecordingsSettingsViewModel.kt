package pg.eti.bicyclonicle.recording_settings

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pg.eti.bicyclonicle.LoadingScreen
import pg.eti.bicyclonicle.R
import pg.eti.bicyclonicle.arduino_connection.services.ConnectionManager
import pg.eti.bicyclonicle.arduino_connection.services.MANAGE_CONN_TAG
import pg.eti.bicyclonicle.preferences.SharedPreferencesManager
import pg.eti.bicyclonicle.recording_settings.RecordingSingleSetting.Name as settings


const val REC_SETT_TAG = "MANAGE_CONN"

// todo: singleton
class RecordingsSettingsViewModel(
    context: Context,
    private val preferenceScreen: PreferenceScreen,
    private val loadingScreen: LoadingScreen
) : ViewModel() {
    private val spm = SharedPreferencesManager.getInstance(context)
    private val changedSettings = mutableListOf<RecordingSingleSetting>()
    private lateinit var connectionManager: ConnectionManager


    init {
        if (spm.isAppFirstStart()) {
            spm.setIsAppFirstStart(false)
            appFirstStartPreferencesInit()
            // TODO?: synchronizeSettings()
        }
    }

    fun linkPreferenceOptions() {
        if (!checkIfArduinoConnected()) {
            throw Exception("Arduino is NOT connected!")
        }

        // resolution
//        val resolutionKeyString = "key_resolution"
//        var labelShowTimeMillis = spm.getInt(keyString, 1200).toLong()
//        var resolution = preferenceScreen.findPreference<SeekBarPreference>(resolutionKeyString)
//        sbpLabelShowTime!!.value = labelShowTimeMillis.toInt()
//        val floatValue = labelShowTimeMillis / 1000.0f
    }

    // todo
    private fun checkIfArduinoConnected(): Boolean {
        return false
    }

    fun synchronizeSettings() {
        synchronizeWithArduino()
//        if (synchronizeWithArduino()) {
//            Log.e(REC_SETT_TAG, "COMMANDS EXECUTED")
//            synchronizeWithSharedPreferences()
//            changedSettings.clear()
//        } else {
//            Log.e(REC_SETT_TAG, "COMMANDS NOT EXECUTED")
//            // TODO: revert changes in settings
//        }
    }

    private fun synchronizeWithArduino() {
        // TODO: send to arduino and wait for reply
        //  how long to wait

        val commands = StringBuilder()
        changedSettings.forEach { setting -> commands.append(setting.getAsCommand()) }

        if (!::connectionManager.isInitialized) {
            connectionManager = ConnectionManager.getExistInstance()
        }

        viewModelScope.launch(Dispatchers.IO) {

            var alertDialog: AlertDialog? = null
            viewModelScope.launch(Dispatchers.Main) {
                alertDialog = loadingScreen.getShowedLoadingScreen()
            }


            val executeAfterWait: (Boolean) -> Unit = { isExecuted ->
                Log.i(MANAGE_CONN_TAG, "IS EXECUTED: $isExecuted")
                alertDialog!!.dismiss()
            }

            connectionManager.sendAndWaitForResponse(
                commands.toString(),
                executeAfterWait
            )
        }
    }

    private fun synchronizeWithSharedPreferences() {

    }

    private fun appFirstStartPreferencesInit() {
        // resolution default
        changedSettings.add(
            RecordingSingleSetting(
                RecordingSingleSetting.Name.RESOLUTION,
                preferenceScreen.findPreference<ListPreference>(settings.RESOLUTION.key)!!.value
            )
        )

        // frame rate default
        changedSettings.add(
            RecordingSingleSetting(
                RecordingSingleSetting.Name.FRAME_RATE,
                preferenceScreen.findPreference<ListPreference>(settings.FRAME_RATE.key)!!.value
            )
        )

        // clip duration default
        changedSettings.add(
            RecordingSingleSetting(
                RecordingSingleSetting.Name.CLIP_DURATION,
                preferenceScreen.findPreference<ListPreference>(settings.CLIP_DURATION.key)!!.value
            )
        )

        // mute sound default
        changedSettings.add(
            RecordingSingleSetting(
                RecordingSingleSetting.Name.MUTE_SOUND,
                preferenceScreen.findPreference<CheckBoxPreference>(settings.MUTE_SOUND.key)!!
                    .isChecked.toString()
            )
        )

        // date on clip default
        changedSettings.add(
            RecordingSingleSetting(
                RecordingSingleSetting.Name.DATE_ON_CLIP,
                preferenceScreen.findPreference<CheckBoxPreference>(settings.DATE_ON_CLIP.key)!!
                    .isChecked.toString()
            )
        )
    }
}