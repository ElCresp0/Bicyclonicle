package pg.eti.bicyclonicle.preferences

import pg.eti.bicyclonicle.preferences.GlobalPreferences as gp

import android.content.Context
import android.content.SharedPreferences
import pg.eti.bicyclonicle.recording_settings.RecordingSingleSetting

/**
 * It is worth to notice that if shared attribute is created it won't disappear on app reinstall,
 * so we need to make singleton class.
 */

class SharedPreferencesManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("bicyclonicle_prefs", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    private val sharedRecordingPreferences: List<RecordingSingleSetting<Any>> = mutableListOf()

    init {
        resetAppLifetimeAttributes()
    }

    fun isAppFirstStart(): Boolean {
        return sharedPreferences.getBoolean(gp.IS_APP_FIRST_START.name, true)
    }

    fun setIsAppFirstStart(isFirstStart: Boolean) {
        editor.putBoolean(gp.IS_APP_FIRST_START.name, isFirstStart)
        editor.apply()
    }

    // todo: will this add to shared
    // todo: method to add to changed list
    // todo: get changed list as command
    fun addRecordingSettingPreference(singleSetting: RecordingSingleSetting) {

        editor.putString(singleSetting.getName(), singleSetting.getValue())
        editor.apply()
    }

//    fun addRecordingSettingPreference(singleSetting: RecordingSingleSetting<T>) {
//        editor.
//    }

    fun isArduinoConnected(): Boolean {
        return sharedPreferences.getBoolean(gp.IS_ARDUINO_CONNECTED.name, false)
    }

    fun setIsArduinoConnected(isConnected: Boolean) {
        editor.putBoolean(gp.IS_ARDUINO_CONNECTED.name, isConnected)
        editor.apply()
    }

    private fun resetAppLifetimeAttributes() {
        editor.putBoolean(gp.IS_ARDUINO_CONNECTED.name, false)
        editor.apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: SharedPreferencesManager? = null

        fun getInstance(context: Context): SharedPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedPreferencesManager(context).also { INSTANCE = it }
            }
        }
    }
}