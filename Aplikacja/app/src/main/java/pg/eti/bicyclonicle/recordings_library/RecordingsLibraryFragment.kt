package pg.eti.bicyclonicle.recordings_library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pg.eti.bicyclonicle.databinding.FragmentRecordingsBinding

class RecordingsLibraryFragment : Fragment() {

    private var _binding: FragmentRecordingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var recordingsLibraryViewModel: RecordingsLibraryViewModel

    override fun onResume() {
        super.onResume()

        recordingsLibraryViewModel.checkIfArduinoConnected()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        recordingsLibraryViewModel =
            ViewModelProvider(this).get(RecordingsLibraryViewModel::class.java)

        _binding = FragmentRecordingsBinding.inflate(inflater, container, false)
        val root: View = binding.root



        setupViewModelLiveData()
        recordingsLibraryViewModel.initViewModel()
        recordingsLibraryViewModel.checkIfArduinoConnected()

        return root
    }

    private fun setupViewModelLiveData() {
        // Pass the context.
        recordingsLibraryViewModel.context.value = context

        recordingsLibraryViewModel.isArduinoConnectedText.observe(viewLifecycleOwner) {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}