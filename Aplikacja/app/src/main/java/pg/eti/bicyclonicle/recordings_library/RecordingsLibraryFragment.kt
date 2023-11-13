package pg.eti.bicyclonicle.recordings_library

//import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.String
import pg.eti.bicyclonicle.LoadingScreen
import pg.eti.bicyclonicle.arduino_connection.services.ConnectionManager
import pg.eti.bicyclonicle.databinding.FragmentRecordingsBinding
import pg.eti.bicyclonicle.ui.record.RecordFile
import pg.eti.bicyclonicle.ui.record.RecordFileAdapter
import pg.eti.bicyclonicle.arduino_connection.services.MANAGE_CONN_TAG
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption


class RecordingsLibraryFragment() : Fragment() {

    private var context: Context? = null
    private var loadingScreen: LoadingScreen? = null
    private val REC_LIB_TAG = "RecLib"
    private var _binding: FragmentRecordingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
//    private val loadingScreen: LoadingScreen
//    private val viewModelScope = this.view.
    private lateinit var gridView: GridView
    private lateinit var arrayList: ArrayList<RecordFile>
    private lateinit var recordFileAdapter: RecordFileAdapter

    private lateinit var recordingsLibraryViewModel: RecordingsLibraryViewModel
    private lateinit var connectionManager: ConnectionManager

    init {
        if (!::connectionManager.isInitialized) {
            connectionManager = ConnectionManager.getExistInstance()
        }
//        context = activity?.applicationContext!!
//        loadingScreen = LoadingScreen(getContext()!!, resources)
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

        fetchNewFileNames()

        gridView.adapter = recordFileAdapter
        gridView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            // play video if it's downloaded or download it from esp
            val videoPath = arrayList[position].retVideoPath()
            val f = File(videoPath)
            if (f.length().toInt() == 0) {
                // video not downloaded
                Log.i("RecLib", "Downloading file")
                downloadFile(f.name)
            }
            else {
                val intent = Intent(requireContext(), VideoViewActivity::class.java)
                intent.putExtra("videoViewUri", arrayList[position].retVideoPath())
                startActivity(intent)
            }
        }

        // context = root.context
        // loadingScreen = LoadingScreen(root.context, resources)
        return root
    }

    // private fun getContext
    private fun getLoadingScreen(): LoadingScreen {
        if (loadingScreen == null) {
            loadingScreen = LoadingScreen(requireContext(), resources)
        }
        return loadingScreen!!
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
                val out: String?
                if (File(it.name).length() > 0) {
                    thumb = ThumbnailUtils.createVideoThumbnail(
                        it.absolutePath,
                        MediaStore.Images.Thumbnails.MINI_KIND
                    )
                    metaRetriever.setDataSource(it.absolutePath)

                    val duration =
                        metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val dur = duration!!.toLong()

                    val seconds = java.lang.String.valueOf(dur % 60000 / 1000)
                    val minutes = java.lang.String.valueOf(dur / 60000)
                    out = "$minutes:$seconds"
                }
                else {
                    // file is not downloaded
                    thumb = null
                    out = null
                }

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchNewFileNames() {
        val command = "ls;"


        recordingsLibraryViewModel.viewModelScope.launch(Dispatchers.IO) {
            // Non UI, long lasting operations should be made on other thread.
            var alertDialog: AlertDialog? = null
            recordingsLibraryViewModel.viewModelScope.launch(Dispatchers.Main) {
                // UI on main.
                alertDialog = getLoadingScreen().getShowedLoadingScreen()
            }


            val executeAfterWait: (Boolean, String) -> Unit = { isExecuted, message ->
                Log.i(MANAGE_CONN_TAG, "IS EXECUTED: $isExecuted")
                alertDialog!!.dismiss()

                if (isExecuted) {
                    Log.i(REC_LIB_TAG, "COMMANDS EXECUTED")
                    Log.i(REC_LIB_TAG, "message in afterWait: $message")
                    // read the list of files from the message
                    var tmpMessage = message.replace("/[^,]*/".toRegex(), "")
                    val files = tmpMessage.split(",")
                    Log.i(REC_LIB_TAG, "files: $files")
                    var f: File
                    // each file: name.ext (expected: .avi)
                    // save these into the dir as empty files with the same names (if there are no such files currently)
                    // later, when a file is selected download it if it's length is 0
                    for (file in files) {
                        f = File(file)
                        Log.i("RecLib", "checking file name: ${f.name}")
                        if (! f.exists()) {
                            Log.i("RecLib", "fetched new file name: ${f.name}")
//                            val writer = PrintWriter(f.name)
                            val fos =
                                requireContext().openFileOutput(f.name, Context.MODE_PRIVATE) // Files.newOutputStream(f.toPath(), StandardOpenOption.WRITE)
//                        val fos = openFileOutput // FileOutputStream(file, )
                            fos.close()
                            // writing to file:
                            // write a single frame for the thumbnail?
//                            writer.close()
                        }
                    }
                } else {
                    recordingsLibraryViewModel.viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "BT - failed to fetch list of files",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e(REC_LIB_TAG, "COMMANDS NOT EXECUTED")
                    }
                }
            }

            connectionManager.sendAndWaitForResponse(
                command,
                executeAfterWait
            )
        }
    }


    private fun downloadFile(fName: kotlin.String) {
        val command = "sendVideo:$fName;"

        recordingsLibraryViewModel.viewModelScope.launch(Dispatchers.IO) {
            // Non UI, long lasting operations should be made on other thread.
            var alertDialog: AlertDialog? = null
            recordingsLibraryViewModel.viewModelScope.launch(Dispatchers.Main) {
                // UI on main.
                alertDialog = getLoadingScreen().getShowedLoadingScreen()
            }


            val executeAfterWait: (Boolean, String) -> Unit = { isExecuted, message ->
                Log.i(MANAGE_CONN_TAG, "IS EXECUTED: $isExecuted")
                alertDialog!!.dismiss()

                if (isExecuted) {
                    Log.e(REC_LIB_TAG, "COMMANDS EXECUTED")
                    // read the list of files from the message
                    val alert: AlertDialog = AlertDialog.Builder(requireContext()).create()
                    alert.setTitle("BT alert")
                    alert.setMessage("BT - file transfer started")
                    alert.show()
                    // file transfer is handled in the ConnectedRunnable class, after which it will be present in the library

                } else {
                    Log.e(REC_LIB_TAG, "COMMANDS NOT EXECUTED")
                    val alert: AlertDialog = AlertDialog.Builder(requireContext()).create()
                    alert.setTitle("BT alert")
                    alert.setMessage("BT - failed to transfer file from device")
                    alert.show()
                }
            }

            connectionManager.sendAndWaitForResponse(
                command,
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