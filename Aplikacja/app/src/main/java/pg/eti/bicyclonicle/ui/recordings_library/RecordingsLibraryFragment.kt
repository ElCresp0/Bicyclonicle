package pg.eti.bicyclonicle.ui.recordings_library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import pg.eti.bicyclonicle.R
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
    var simpleVideoView: VideoView? = null
    var mediaControls: MediaController? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val recordingsLibraryViewModel =
            ViewModelProvider(this).get(RecordingsLibraryViewModel::class.java)

        _binding = FragmentRecordingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val activity = requireActivity()

        recordingsLibraryViewModel.text.observe(viewLifecycleOwner) {
            //textView.text = it
        }

        simpleVideoView =  binding.videoView
        gridView = binding.recordGrid
        arrayList = ArrayList()
        arrayList = setDataList()

        recordFileAdapter = RecordFileAdapter(requireContext(),arrayList!!)
        gridView?.adapter = recordFileAdapter

        if (mediaControls == null) {
            // creating an object of media controller class
            mediaControls = MediaController(requireContext())

            // set the anchor view for the video view
            mediaControls!!.setAnchorView(this.simpleVideoView)
        }

        simpleVideoView!!.setMediaController(mediaControls)
        simpleVideoView!!.visibility = View.INVISIBLE;
        gridView!!.onItemClickListener = OnItemClickListener { parent, v, position, id ->
            //val clickedItem = arrayList!![position]
            val bottomNavigationView = activity.findViewById<BottomNavigationView>(
                R.id.nav_view)
            bottomNavigationView.visibility = View.GONE

            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            simpleVideoView!!.visibility = View.VISIBLE
            gridView!!.visibility = View.INVISIBLE

            simpleVideoView!!.setVideoURI(Uri.parse(arrayList!![position].retVideoPath()))

            simpleVideoView!!.requestFocus()

            simpleVideoView!!.start()

        }
        simpleVideoView!!.setOnCompletionListener {
            simpleVideoView!!.visibility = View.INVISIBLE
            gridView!!.visibility = View.VISIBLE
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            //Toast.makeText(applicationContext, "Video completed", Toast.LENGTH_LONG).show()
            true
        }

        return root
    }


    private fun setDataList() :ArrayList<RecordFile>{

        var arrayList: ArrayList<RecordFile> = ArrayList()
        val metaRetriever = MediaMetadataRetriever()
        var thumb: Bitmap? = null
        //"/data/data/pg.eti.bicyclonicle/files"
        File(this.activity?.filesDir?.absolutePath).walk().forEach {
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

                arrayList.add(RecordFile(thumb, it.name, out, null, null, it.absolutePath))
            }


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