package pg.eti.bicyclonicle.recording_settings


/**
 * Class describes single preference that refers to RecordingSettingName
 * to be able to List and process all of them.
 *
 * @author jankejc
 */
class RecordingSingleSetting(
    private var name: Name,
    private var value: String
) {
    fun getName(): Name {
        return name
    }

    fun setName(name: Name) {
        this.name = name
    }

    fun getValue(): String {
        return value
    }

    fun setValue(value: String) {
        this.value = value
    }

    fun getAsCommand(): String {
        return name.name + ":" + value + ";"
    }

    /**
     * These keys should be equal to recording_settings_preferences.xml
     *
     * @author jankejc
     */
    enum class Name(val key: String) {
        SYNCHRONIZE("synchronize_button"),
        RESOLUTION("key_resolution"),
        FRAME_RATE("key_frame_rate"),
        CLIP_DURATION("key_duration"),
        MUTE_SOUND("key_mute_sound"),
        DATE_ON_CLIP("key_show_date")
    }
}