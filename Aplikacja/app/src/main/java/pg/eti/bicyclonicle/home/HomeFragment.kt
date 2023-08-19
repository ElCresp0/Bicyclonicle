package pg.eti.bicyclonicle.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import pg.eti.bicyclonicle.databinding.FragmentHomeBinding
import pg.eti.bicyclonicle.views_adapters.StringAdapter

const val LOG_TAG = "HomeFragment"

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel

    private val myEnableBtIntentLauncher = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()) {
        homeViewModel.updateBluetoothStatus()
    }

    override fun onResume() {
        super.onResume()

        homeViewModel.updateBluetoothStatus()
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
        homeViewModel.setupArduinoConnection()

        return root
    }

    private fun setupViewModelLiveData() {
        // Pass the context.
        homeViewModel.context.value = context

        // TODO: change msgs when i will be paired
        //  now all msgs have bonded devices
        val textBtStatus: TextView = binding.tvBtStatus
        textBtStatus.layoutParams = layoutParamsAboveNavMenu(textBtStatus)

        homeViewModel.bluetoothStatusText.observe(viewLifecycleOwner) {
            textBtStatus.text = it
        }

        homeViewModel.bondedDevices.observe(viewLifecycleOwner) {
            val recyclerViewBondedDevices = binding.rvBondedDevices
            recyclerViewBondedDevices.layoutManager = LinearLayoutManager(context)
            var stringAdapter: StringAdapter

            val bondedDevices = homeViewModel.bondedDevices.value
            stringAdapter = if (!bondedDevices.isNullOrEmpty()) {
                // Init RecyclerView for bonded devices.
                StringAdapter(bondedDevices)
            } else {
                StringAdapter(ArrayList())
            }

            recyclerViewBondedDevices.adapter = stringAdapter
        }

        homeViewModel.enableBluetoothIntent.observe(viewLifecycleOwner) {
            myEnableBtIntentLauncher.launch(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
}