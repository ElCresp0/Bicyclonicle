package pg.eti.bicyclonicle.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pg.eti.bicyclonicle.LoadingScreen
import pg.eti.bicyclonicle.databinding.FragmentHomeBinding

// TODO: translate toasts everywhere
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel

    private val myEnableBtIntentLauncher = registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()
    ) {

        homeViewModel.checkArduinoConnection(binding)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i(HOME_VM_TAG, "permissions granted")
            } else {
                homeViewModel.missingPermissionsDialog()
            }
        }

    override fun onResume() {
        super.onResume()

        homeViewModel.checkArduinoConnection(binding)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root


        setupViewModelLiveData()
        homeViewModel.initViewModel()

        isBluetoothConnectScanPermission(true)

        binding.btnConnectToArduino.setOnClickListener {
            homeViewModel.connectToArduino(binding)
        }

        homeViewModel.checkArduinoConnection(binding)

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

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
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

    private fun isBluetoothConnectScanPermission(ifRequest: Boolean) {
        // Scan and connect has the same "Nearby permission"

        when {
            ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
                    || Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            -> {
                homeViewModel.setIsPermissionBluetoothConnect(true)
                homeViewModel.setIsPermissionBluetoothScan(true)
            }

            else -> {
                if (ifRequest) {
                    requestPermissionLauncher.launch(
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                    isBluetoothConnectScanPermission(false)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}