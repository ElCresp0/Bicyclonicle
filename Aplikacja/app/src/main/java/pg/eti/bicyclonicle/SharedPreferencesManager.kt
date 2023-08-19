package pg.eti.bicyclonicle

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("bicyclonicle_prefs", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    fun putPref(name: String, value: String) {
        editor.putString(name, value)
        editor.apply()
    }

    fun putPref(name: String, value: Int) {
        editor.putInt(name, value)
        editor.apply()
    }

    fun putPref(name: String, value: Boolean) {
        editor.putBoolean(name, value)
        editor.apply()
    }

    fun getPref(name: String, defaultValue: String): String {
        return sharedPreferences.getString(name, defaultValue)!!
    }

    fun getPref(name: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(name, defaultValue)
    }

    fun getPref(name: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(name, defaultValue)
    }

    enum class Prefs {
        IS_ARDUINO_CONNECTED
    }
}