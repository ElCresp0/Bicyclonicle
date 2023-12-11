package pg.eti.bicyclonicle.recordings_library

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.String
import pg.eti.bicyclonicle.LoadingScreen
import pg.eti.bicyclonicle.R
import pg.eti.bicyclonicle.arduino_connection.enums.ConnectionStatus
import pg.eti.bicyclonicle.arduino_connection.services.ConnectionManager
import pg.eti.bicyclonicle.databinding.FragmentRecordingsBinding
import pg.eti.bicyclonicle.ui.record.RecordFile
import pg.eti.bicyclonicle.ui.record.RecordFileAdapter
import pg.eti.bicyclonicle.arduino_connection.services.MANAGE_CONN_TAG
import java.io.File
import java.nio.file.Files
import kotlin.io.path.fileSize


class RecordingsLibraryFragment() : Fragment() {

    private var context: Context? = null
    private var loadingScreen: LoadingScreen? = null
    private val REC_LIB_TAG = "RecLib"
    private var _binding: FragmentRecordingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var gridView: GridView
    private lateinit var arrayList: ArrayList<RecordFile>
    private lateinit var recordFileAdapter: RecordFileAdapter

    private lateinit var recordingsLibraryViewModel: RecordingsLibraryViewModel
    private lateinit var connectionManager: ConnectionManager

    companion object {
        var noReload: Boolean = false
    }

    private val mHandler: Handler = object : Handler() {
        @SuppressLint("HandlerLeak")
        override fun handleMessage(msg: Message) {
            val navController = findNavController()
            noReload = true
            Log.i(REC_LIB_TAG, "Reloading...")
            navController.run {
                popBackStack()
                navigate(R.id.navigation_recording_library)
            }
        }
    }

    init {
        if (!::connectionManager.isInitialized) {
            connectionManager = ConnectionManager.getExistInstance()
        }
    }

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
        if (!noReload) {
            Log.i(REC_LIB_TAG, "before fetching")
            fetchNewFileNames()
            Log.i(REC_LIB_TAG, "after fetching")
        }
        noReload = false
        arrayList = setDataList()

        recordFileAdapter = RecordFileAdapter(requireContext(), arrayList)


        gridView.adapter = recordFileAdapter
        gridView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            // play video if it's downloaded or download it from esp
            val videoPath = arrayList[position].retVideoPath()
            val f = File(videoPath)
            val size = f.length()
//            Files.size(f.toPath())
//            val size: BasicFileAttributes = Files.readAttributes(f.toPath(), BasicFileAttributes)
            Log.i(REC_LIB_TAG, "file size: $size")
            if (size == 0.toLong()) {
                Log.i(REC_LIB_TAG, "downloading file")
                // video not downloaded
                downloadFile(f.name)
            }
            else {
                Log.i(REC_LIB_TAG, "displaying the file")
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
                val sdSaved: Boolean?
                val telSaved: Boolean?
                if (it.length() > 0) {
                    thumb = ThumbnailUtils.createVideoThumbnail(
                        it.absolutePath,
                        MediaStore.Images.Thumbnails.MINI_KIND
                    )
                    metaRetriever.setDataSource(it.absolutePath)

                    val duration =
                        metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val dur = duration!!.toLong()

                    val seconds = dur % 60000 / 1000
                    val minutes = dur / 60000
                    out = String.format("%02d:%02d", minutes, seconds)
                    sdSaved = false;
                    telSaved = true;
                }
                else {
                    // file is not downloaded
                    thumb = null
                    out = null
                    sdSaved = true;
                    telSaved = false;
                }
                arrayList.add(RecordFile(thumb, it.name, out, sdSaved, telSaved, it.absolutePath))
            }
        }
        return arrayList
    }

    private fun fetchNewFileNames() {
        if (connectionManager.getUpdatedArduinoConnectionStatus() != ConnectionStatus.CONNECTED) {
            recordingsLibraryViewModel.viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "BT - not connected",
                    Toast.LENGTH_LONG
                ).show()
            }
            Log.e(REC_LIB_TAG, "BT - not connected")
            return
        }
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
                    var tmpMessage = message.replace("/".toRegex(), "")
                    val files = tmpMessage.split(",")
                    Log.i(REC_LIB_TAG, "files: $files")
                    var f: File
                    // each file: name.ext (expected: .avi)
                    // save these into the dir as empty files with the same names (if there are no such files currently)
                    // later, when a file is selected download it if it's length is 0
                    for (file in files) {
                        f = File(file)
                        Log.i("RecLib", "checking file name: ${f.name}")
                        try {
                            val fis = requireContext().openFileInput(f.name)
                            fis.close()
                        }
                        catch(e: Exception) {
                            e.printStackTrace()
                            Log.i("RecLib", "fetched new file name: ${f.name}")
                            val fos = requireContext().openFileOutput(
                                f.name,
                                android.content.Context.MODE_PRIVATE
                            )
                            fos.close()
                        }
                    }
                    mHandler.sendMessage(Message())
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
        if (connectionManager.getUpdatedArduinoConnectionStatus() != ConnectionStatus.CONNECTED) {
            recordingsLibraryViewModel.viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "BT - not connected",
                    Toast.LENGTH_LONG
                ).show()
            }
            Log.e(REC_LIB_TAG, "BT - not connected")
            return
        }
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
                // alertDialog!!.dismiss()

                if (isExecuted && "sending" in message) {
                    Log.e(REC_LIB_TAG, "COMMANDS EXECUTED")
                    recordingsLibraryViewModel.viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "BT - file transfer started",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    val params = message.split(":")
                    connectionManager.receiveFileInConnectedThread(params[1], params[2].toInt(), requireContext())
                    alertDialog!!.dismiss()
                    recordingsLibraryViewModel.viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "BT - file transfer finished",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    mHandler.sendMessage(Message())
                } else {
                    Log.e(REC_LIB_TAG, "COMMANDS NOT EXECUTED")
                    recordingsLibraryViewModel.viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "BT - failed to transfer file from device",
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}