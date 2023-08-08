package pg.eti.bicyclonicle.ui.recording_settings

import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pg.eti.bicyclonicle.databinding.FragmentRecordingSettingsBinding

import androidx.preference.PreferenceFragmentCompat
import pg.eti.bicyclonicle.R


class RecordingSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.recording_settings_preferences, rootKey)


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    //GridView gridView;
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val recordingSettingsViewModel =
            ViewModelProvider(this).get(RecordingSettingsViewModel::class.java)

        _binding = FragmentRecordingSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textRecordingSettings
        recordingSettingsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        val appVersionPreference = findPreference<EditTextPreference>("key_app_version")
        appVersionPreference?.summary = getAppVersionName(requireContext())

    }
}