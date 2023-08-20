package pg.eti.bicyclonicle.ui.recordings_library

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.GridView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pg.eti.bicyclonicle.R
import pg.eti.bicyclonicle.databinding.FragmentRecordingsBinding
import pg.eti.bicyclonicle.ui.record.Record_file
import pg.eti.bicyclonicle.ui.record.Record_file_adapter

class RecordingsLibraryFragment : Fragment() {

    private var _binding: FragmentRecordingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    var gridView: GridView ? = null
    var arrayList: ArrayList<Record_file> ? = null
    var recordFileAdapter: Record_file_adapter ? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val recordingsLibraryViewModel =
            ViewModelProvider(this).get(RecordingsLibraryViewModel::class.java)

        _binding = FragmentRecordingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //val gridView: GridView = binding.recordGrid
        recordingsLibraryViewModel.text.observe(viewLifecycleOwner) {
            //textView.text = it

        }
        gridView = binding.recordGrid
        arrayList = ArrayList()
        arrayList = setDataList()
        //record_file_adapter =
           // context?.let { Record_file_adapter(it.getApplicationContext(), arrayList!!) } //nie mam pojecia
        recordFileAdapter = Record_file_adapter(requireContext(),arrayList!!)
        gridView?.adapter = recordFileAdapter
//aplicationContext
        return root
    }


    private fun setDataList() :ArrayList<Record_file>{

        var arrayList: ArrayList<Record_file> = ArrayList()

        arrayList.add(Record_file(R.drawable.ic_launcher_background, "Pierwsze nagranie", "21:37"))
        arrayList.add(Record_file(R.drawable.ic_launcher_background, "Drugie nagranie", "20:37"))
        arrayList.add(Record_file(R.drawable.ic_launcher_background, "Trzecie nagranie", "19:37"))
        arrayList.add(Record_file(R.drawable.ic_launcher_background, "Czwarte nagranie", "18:37"))
        arrayList.add(Record_file(R.drawable.ic_launcher_background, "Piąte nagranie", "17:37"))

        return arrayList
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}