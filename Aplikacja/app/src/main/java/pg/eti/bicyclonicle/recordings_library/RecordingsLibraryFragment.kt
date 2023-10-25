package pg.eti.bicyclonicle.recordings_library

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
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
    private lateinit var gridView: GridView
    private lateinit var arrayList: ArrayList<RecordFile>
    private lateinit var recordFileAdapter: RecordFileAdapter

    private lateinit var recordingsLibraryViewModel: RecordingsLibraryViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        recordingsLibraryViewModel =
            ViewModelProvider(this).get(RecordingsLibraryViewModel::class.java)

        _binding = FragmentRecordingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        gridView = binding.recordGrid

        arrayList = ArrayList()
        arrayList = setDataList()

        recordFileAdapter = RecordFileAdapter(requireContext(), arrayList)
        gridView.adapter = recordFileAdapter
        gridView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            val intent = Intent(requireContext(), VideoViewActivity::class.java)
            intent.putExtra("videoViewUri", arrayList[position].retVideoPath())
            startActivity(intent)
        }

        return root
    }

    private fun setDataList(): ArrayList<RecordFile> {

        val arrayList: ArrayList<RecordFile> = ArrayList()
        val metaRetriever = MediaMetadataRetriever()
        var thumb: Bitmap?
        //"/data/data/pg.eti.bicyclonicle/files"
        File(this.activity?.filesDir?.absolutePath).walk().forEach {
            println(it)
            if (it.extension == "mp4") {
                thumb = ThumbnailUtils.createVideoThumbnail(
                    it.absolutePath,
                    MediaStore.Images.Thumbnails.MINI_KIND
                )
                metaRetriever.setDataSource(it.absolutePath)

                val duration =
                    metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val dur = duration!!.toLong()

                val seconds = String.valueOf(dur % 60000 / 1000)
                val minutes = String.valueOf(dur / 60000)
                val out = "$minutes:$seconds"

                arrayList.add(RecordFile(thumb, it.name, out, null, null, it.absolutePath))
            }


        }
        return arrayList
    }

    private fun saveRecordToSD() {}

    private fun saveRecordToTele() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}