package pg.eti.bicyclonicle.recordings_library

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import pg.eti.bicyclonicle.databinding.ActivityVideoViewBinding

@Suppress("DEPRECATION")
class VideoViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoViewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVideoViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoView = binding.videoView
        val videoUri = intent.getSerializableExtra("videoViewUri") as String
        binding.videoView.setVideoURI(Uri.parse(videoUri))

        immersiveMode()

        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)
        videoView.start()

        supportActionBar!!.hide()
    }

    private fun immersiveMode() {
        val windowInsetsController =
            WindowCompat.getInsetsController(
                window,
                window.decorView
            )


        // Configure the behavior of the hidden system bars.
//        windowInsetsController.systemBarsBehavior =
//            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())

//            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}