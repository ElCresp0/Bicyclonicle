package pg.eti.bicyclonicle.recording_settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceFragmentCompat
import pg.eti.bicyclonicle.R
import pg.eti.bicyclonicle.databinding.FragmentRecordingsBinding
import pg.eti.bicyclonicle.recordings_library.RecordingsLibraryViewModel

class RecordingSettingsFragment : PreferenceFragmentCompat() {
    private lateinit var recordingsSettingsViewModel: RecordingsSettingsViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.recording_settings_preferences, rootKey)

        recordingsSettingsViewModel = RecordingsSettingsViewModel(
            requireContext(),
            preferenceScreen
        )

        recordingsSettingsViewModel.linkPreferenceOptions()
    }
}