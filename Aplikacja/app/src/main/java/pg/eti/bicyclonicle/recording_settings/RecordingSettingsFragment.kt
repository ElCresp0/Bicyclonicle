package pg.eti.bicyclonicle.recording_settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import pg.eti.bicyclonicle.LoadingScreen
import pg.eti.bicyclonicle.R

class RecordingSettingsFragment : PreferenceFragmentCompat() {
    private lateinit var recordingsSettingsViewModel: RecordingsSettingsViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.recording_settings_preferences, rootKey)

        recordingsSettingsViewModel = RecordingsSettingsViewModel(
            requireContext(),
            preferenceScreen,
            LoadingScreen.getInstance(requireContext(), resources)
        )

        // Find the custom button preference by key
        val customButtonPreference: Preference? = findPreference("synchronize_button")

        // Set a click listener for the button
        customButtonPreference?.setOnPreferenceClickListener {
            // Handle the button click here
            recordingsSettingsViewModel.synchronizeSettings()
            true
        }
    }
}