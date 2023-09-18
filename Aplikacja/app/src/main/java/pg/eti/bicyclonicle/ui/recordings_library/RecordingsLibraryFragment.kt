package pg.eti.bicyclonicle.ui.recordings_library

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pg.eti.bicyclonicle.databinding.FragmentRecordingsBinding
import pg.eti.bicyclonicle.ui.record.RecordFile
import pg.eti.bicyclonicle.ui.record.RecordFileAdapter
import java.io.File
import java.lang.String


class RecordingsLibraryFragment : Fragment() {

    private var _binding: FragmentRecordingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    var gridView: GridView ? = null
    var arrayList: ArrayList<RecordFile> ? = null
    var recordFileAdapter: RecordFileAdapter ? = null
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
        recordFileAdapter = RecordFileAdapter(requireContext(),arrayList!!)
        gridView?.adapter = recordFileAdapter
//aplicationContext
        return root
    }


    private fun setDataList() :ArrayList<RecordFile>{

        var arrayList: ArrayList<RecordFile> = ArrayList()
        val metaRetriever = MediaMetadataRetriever()
        var thumb: Bitmap? = null
        //arrayList.add(Record_file(R.drawable.ic_launcher_background, "Pierwsze nagranie", "21:37", null, null))
        File("/data/data/pg.eti.bicyclonicle/files" ).walk().forEach {
            println(it)
            if(it.extension == "mp4"){
                thumb = ThumbnailUtils.createVideoThumbnail(it.absolutePath, MediaStore.Images.Thumbnails.MINI_KIND)
                metaRetriever.setDataSource(it.absolutePath)

                val duration =
                    metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val dur = duration!!.toLong()

                val seconds = String.valueOf(dur % 60000 / 1000)
                val minutes = String.valueOf(dur / 60000)
                val out = "$minutes:$seconds"

                arrayList.add(RecordFile(thumb, it.name, out, null, null))
            }


            //TODO sprawdzić rozszerzenie mp4
        }
        return arrayList
    }

    private fun saveRecordToSD(){}

    private fun saveRecordToTele(){}


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}