package pg.eti.bicyclonicle.recording_settings

import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference


/**
 * Class describes single preference that refers to RecordingSettingName
 * to be able to List and process all of them.
 *
 * @author jankejc
 */
class RecordingSingleSetting(
    private var properties: Properties,
    private var value: String
) {
    fun getProperties(): Properties {
        return properties
    }

    fun setProperties(properties: Properties) {
        this.properties = properties
    }

    fun getValue(): String {
        return value
    }

    fun setValue(value: String) {
        this.value = value
    }

    fun getAsCommand(): String {
        return properties.key + ":" + value + ";"
    }

    /**
     * These keys should be equal to recording_settings_preferences.xml
     *
     * @author jankejc
     */
    enum class Properties(val key: String, val preferenceClass: Class<*>) {
        SYNCHRONIZE("synchronize_button", Preference::class.java),
        RESOLUTION("key_resolution", ListPreference::class.java),
//        FRAME_RATE("key_frame_rate", ListPreference::class.java),
        CLIP_DURATION("key_duration", ListPreference::class.java),
        MUTE_SOUND("key_mute_sound", CheckBoxPreference::class.java),
        DATE_ON_CLIP("key_show_date", CheckBoxPreference::class.java)
    }
}