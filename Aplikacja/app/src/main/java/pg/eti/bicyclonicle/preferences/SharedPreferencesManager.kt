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

    init {
        resetAppLifetimeAttributes()
        // TODO: ask arduino if default settings and check if do i;
        //  if not the same then synchronise with app
    }

    fun isAppFirstStart(): Boolean {
        return true
// TODO:        return sharedPreferences.getBoolean(gp.IS_APP_FIRST_START.name, true)
    }

    fun setIsAppFirstStart(isFirstStart: Boolean) {
        editor.putBoolean(gp.IS_APP_FIRST_START.name, isFirstStart)
        editor.apply()
    }

    // todo: will this add to shared
    // todo: method to add to changed list
    // todo: get changed list as command
    // todo: change shared prefs after return message from arduino
    //  and clear list

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