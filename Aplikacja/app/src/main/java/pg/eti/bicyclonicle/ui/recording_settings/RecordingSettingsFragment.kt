package pg.eti.bicyclonicle.ui.recording_settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import pg.eti.bicyclonicle.R

class RecordingSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.recording_settings_preferences, rootKey)

//        val appVersionPreference = findPreference<EditTextPreference>("key_app_version")
//        appVersionPreference?.summary = getAppVersionName(requireContext())
    }
}