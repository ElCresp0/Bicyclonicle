package pg.eti.bicyclonicle.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pg.eti.bicyclonicle.LoadingScreen
import pg.eti.bicyclonicle.databinding.FragmentHomeBinding

const val HOME_FR_TAG = "HomeFragment"

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel

    private val myEnableBtIntentLauncher = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()) {

        binding.btnConnectToArduino.isEnabled = !homeViewModel.checkArduinoConnection(binding)
    }

    override fun onResume() {
        super.onResume()

        binding.btnConnectToArduino.isEnabled = !homeViewModel.checkArduinoConnection(binding)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupViewModelLiveData()
        homeViewModel.initViewModel()

        binding.btnConnectToArduino.setOnClickListener {
            homeViewModel.connectToArduino(binding)
        }

        binding.btnConnectToArduino.isEnabled = !homeViewModel.checkArduinoConnection(binding)


        return root
    }

    private fun setupViewModelLiveData() {
        // Pass the context.
        homeViewModel.context.value = context

        homeViewModel.loadingScreen.value = LoadingScreen.getInstance(requireContext(), resources)

        val textBtStatus: TextView = binding.tvBtStatus
        textBtStatus.layoutParams = layoutParamsAboveNavMenu(textBtStatus)
        homeViewModel.bluetoothStatusText.observe(viewLifecycleOwner) {
            textBtStatus.text = it
        }

        homeViewModel.enableBluetoothIntent.observe(viewLifecycleOwner) {
            myEnableBtIntentLauncher.launch(it)
        }
    }

    private fun layoutParamsAboveNavMenu(view: View): ViewGroup.MarginLayoutParams {
        // Put TextView above nav menu.
        val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.setMargins(
            layoutParams.leftMargin,
            layoutParams.topMargin,
            layoutParams.rightMargin,
            getNavigationBarHeight()
        )

        return layoutParams
    }

    private fun getNavigationBarHeight(): Int {
        val resources = resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            // If the device don't support resources.getIdentifier.
            100
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}