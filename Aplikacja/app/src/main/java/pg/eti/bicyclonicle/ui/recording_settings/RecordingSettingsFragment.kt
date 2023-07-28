package pg.eti.bicyclonicle.ui.recording_settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pg.eti.bicyclonicle.databinding.FragmentRecordingSettingsBinding

class RecordingSettingsFragment : Fragment() {

    private var _binding: FragmentRecordingSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
    }
}