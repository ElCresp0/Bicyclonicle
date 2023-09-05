package pg.eti.bicyclonicle.recording_settings


/**
 * Class describes single preference that refers to RecordingSettings
 * to be able to List and process all of them.
 *
 * @author jankejc
 */
class RecordingSingleSetting(
    private val name: String,
    private val value: String
) {
    // todo: change when arduino send message back that received everything
    private var isChanged = false

    fun getName(): String {
        return name
    }

    fun getValue(): String {
        return value
    }

    fun isChanged(): Boolean {
        return isChanged
    }
}