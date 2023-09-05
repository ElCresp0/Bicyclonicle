package pg.eti.bicyclonicle.recording_settings

import android.content.Context
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import pg.eti.bicyclonicle.preferences.SharedPreferencesManager
import pg.eti.bicyclonicle.recording_settings.RecordingSettings as rs

// todo: singleton
class RecordingsSettingsViewModel(
    context: Context,
    private val preferenceScreen: PreferenceScreen
) {
    private val spm = SharedPreferencesManager.getInstance(context)

    init {
        if (spm.isAppFirstStart()) {
            spm.setIsAppFirstStart(false)
            appFirstStartPreferencesInit()
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

    private fun appFirstStartPreferencesInit() {
        // resolution default
        spm.addRecordingSettingPreference(
            RecordingSingleSetting(
                rs.RESOLUTION.key,
                preferenceScreen.findPreference<ListPreference>(rs.RESOLUTION.key)!!.value
            )
        )

        // frame rate default
        spm.addRecordingSettingPreference(
            RecordingSingleSetting(
                rs.FRAME_RATE.key,
                preferenceScreen.findPreference<ListPreference>(rs.FRAME_RATE.key)!!.value
            )
        )

        // clip duration default
        spm.addRecordingSettingPreference(
            RecordingSingleSetting(
                rs.CLIP_DURATION.key,
                preferenceScreen.findPreference<ListPreference>(rs.CLIP_DURATION.key)!!.value
            )
        )

        // mute sound default
        spm.addRecordingSettingPreference(
            RecordingSingleSetting(
                rs.MUTE_SOUND.key,
                preferenceScreen.findPreference<ListPreference>(rs.MUTE_SOUND.key)!!.value
            )
        )

        // date on clip default
        spm.addRecordingSettingPreference(
            RecordingSingleSetting(
                rs.DATE_ON_CLIP.key,
                preferenceScreen.findPreference<ListPreference>(rs.DATE_ON_CLIP.key)!!.value
            )
        )
    }
}