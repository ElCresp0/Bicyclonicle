package pg.eti.bicyclonicle.recording_settings

/**
 * These keys should be equal to recording_settings_preferences.xml
 *
 * @author jankejc
 */
enum class RecordingSettings(val key: String) {
    RESOLUTION("key_resolution"),
    FRAME_RATE("key_frame_rate"),
    CLIP_DURATION("key_duration"),
    MUTE_SOUND("key_mute_sound"),
    DATE_ON_CLIP("key_show_date")
}