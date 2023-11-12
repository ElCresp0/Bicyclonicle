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
import pg.eti.bicyclonicle.arduino_connection.enums.ConnectionStatus
import pg.eti.bicyclonicle.arduino_connection.services.ConnectionManager
import pg.eti.bicyclonicle.arduino_connection.services.MANAGE_CONN_TAG
import java.io.File
import java.io.PrintWriter
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
            // play video if it's downloaded or download it from esp
            val videoPath = arrayList[position].retVideoPath()
            val f = File(videoPath)
            if (f.length() == 0) {
                // video not downloaded
                Log.i("RecLib", "Downloading file")
                downloadFile(f.getName())
            }

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
            if (it.extension == "mp4" || it.extension == "avi") {
                // to do 
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
            // else if (it.extension == "???") {
            //     Log.i("RecLib", "file to download: $it.name")
            //     arrayList.add(RecordFile(null, it.name, "not downloaded", null, null, it.absolutePath))
            //     // probably won't work
            // }


        }
        return arrayList
    }

    override fun onResume() {
        // when the fragment is displayed, ask esp for recording list to show in the library
        val command = "ls;"

        viewModelScope.launch(Dispatchers.IO) {
            // Non UI, long lasting operations should be made on other thread.
            var alertDialog: AlertDialog? = null
            viewModelScope.launch(Dispatchers.Main) {
                // UI on main.
                alertDialog = loadingScreen.getShowedLoadingScreen()
            }


            val executeAfterWait: (Boolean, String) -> Unit = { isExecuted, message ->
                Log.i(MANAGE_CONN_TAG, "IS EXECUTED: $isExecuted")
                alertDialog!!.dismiss()

                if (isExecuted) {
                    Log.e(REC_SETT_TAG, "COMMANDS EXECUTED")
                    // read the list of files from the message
                    enableSettings()
                    setIsEnableSynchronizeButton(false)

                } else {
                    Log.e(REC_SETT_TAG, "COMMANDS NOT EXECUTED")
                    val alert: AlertDialog = AlertDialog.Builder(this).create()
                    alert.setTitle("BT alert")
                    alert.setMessage("BT - failed to fetch list of files")
                    alert.show()
                }

                viewModelScope.launch(Dispatchers.Main) {
                    // UI on main.
                    // message: 
                    // in handler perform the following to obtain a list of names
                    var tmpMessage = message.replace("/[^,]*/".toRegex(), "")
                    val files = readMessage.split(",")
                    // each file: name.ext
                    // save these into the dir as empty files with the same names (if there are no such files currently)
                    // later, when a file is selected download it if it's length is 0
                    files.forEach(
                        val f = File(it.toString())
                        if (! f.exists()) {
                            Log.i("RecLib", "fetched new file name: ${f.getName()}")
                            val writer = PrintWriter(f.getAbsolutePath())
                            // writing to file: writer.append("test string hello")
                            writer.close()
                        }
                    )
                }
            }

            connectionManager.sendAndWaitForResponse(
                commands.toString(),
                executeAfterWait
            )
        }
    }

    private fun downloadFile(fName: String) {
        val command = "sendVideo:$fName;"

        viewModelScope.launch(Dispatchers.IO) {
            // Non UI, long lasting operations should be made on other thread.
            var alertDialog: AlertDialog? = null
            viewModelScope.launch(Dispatchers.Main) {
                // UI on main.
                alertDialog = loadingScreen.getShowedLoadingScreen()
            }


            val executeAfterWait: (Boolean, String) -> Unit = { isExecuted, message ->
                Log.i(MANAGE_CONN_TAG, "IS EXECUTED: $isExecuted")
                alertDialog!!.dismiss()

                if (isExecuted) {
                    Log.e(REC_SETT_TAG, "COMMANDS EXECUTED")
                    // read the list of files from the message
                    val alert: AlertDialog = AlertDialog.Builder(this).create()
                    alert.setTitle("BT alert")
                    alert.setMessage("BT - file transfer started")
                    alert.show()
                    // file transfer is handled in the ConnectedRunnable class, after which it will be present in the library

                } else {
                    Log.e(REC_SETT_TAG, "COMMANDS NOT EXECUTED")
                    val alert: AlertDialog = AlertDialog.Builder(this).create()
                    alert.setTitle("BT alert")
                    alert.setMessage("BT - failed to transfer file from device")
                    alert.show()
                }
            }

            connectionManager.sendAndWaitForResponse(
                commands.toString(),
                executeAfterWait
            )
        }
    }

    private fun saveRecordToSD() {}

    private fun saveRecordToTele() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}