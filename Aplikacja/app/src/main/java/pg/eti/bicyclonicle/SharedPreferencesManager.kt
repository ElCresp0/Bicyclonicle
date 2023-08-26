package pg.eti.bicyclonicle

import android.content.Context
import android.content.SharedPreferences

/**
 * It is worth to notice that if shared attribute is created it won't disappear on app reinstall,
 * so we need to make singleton class.
 */

class SharedPreferencesManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("bicyclonicle_prefs", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    init {
        resetAppLifetimeAttributes()
    }

    fun isArduinoConnected(): Boolean {
        return getPref(Prefs.IS_ARDUINO_CONNECTED.name, false)
    }

    fun setArduinoConnectionStatus(isConnected: Boolean) {
        putPref(Prefs.IS_ARDUINO_CONNECTED.name, isConnected)
    }

    private fun putPref(name: String, value: String) {
        editor.putString(name, value)
        editor.apply()
    }

    private fun putPref(name: String, value: Int) {
        editor.putInt(name, value)
        editor.apply()
    }

    private fun putPref(name: String, value: Boolean) {
        editor.putBoolean(name, value)
        editor.apply()
    }

    private fun getPref(name: String, defaultValue: String): String {
        return sharedPreferences.getString(name, defaultValue)!!
    }

    private fun getPref(name: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(name, defaultValue)
    }

    private fun getPref(name: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(name, defaultValue)
    }

    private fun resetAppLifetimeAttributes() {
        putPref(Prefs.IS_ARDUINO_CONNECTED.name, false)
    }

    enum class Prefs {
        IS_ARDUINO_CONNECTED
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