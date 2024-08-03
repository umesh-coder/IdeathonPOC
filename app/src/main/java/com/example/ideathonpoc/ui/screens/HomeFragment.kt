package com.example.ideathonpoc.ui.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.example.ideathonpoc.R
import com.example.ideathonpoc.databinding.FragmentHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val permitMap = mapOf(
        "General Permit" to listOf("Helmet", "Safety Vest"),
        "Special Permit" to listOf("Gloves", "Safety Vest"),
        "High-Risk Permit" to listOf("Helmet", "Safety Vest", "Gloves")
    )
    private var selectedPermit: String? = null
    private var selectedScanType: String? = null
    private lateinit var liveCaptureButton: LottieAnimationView
    private lateinit var permitSpinner: Spinner
    private lateinit var scanTypeSpinner: Spinner
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            liveCaptureButton = binding.liveCapture
            permitSpinner = binding.permitDropdown
            scanTypeSpinner = binding.scanningdropdown
            bottomNavigationView = requireActivity().findViewById(R.id.bottomNav)
            bottomNavigationView.visibility = View.VISIBLE
            setupPermitSpinner()
            setupScanTypeSpinner()
            setupButtons()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
        }
    }

    private fun setupPermitSpinner() {
        val permits = permitMap.keys.toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, permits)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        permitSpinner.adapter = adapter
        selectedPermit = permits[2]
      /*  permitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedPermit = permits[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }*/
    }

    private fun setupScanTypeSpinner() {
        val scanTypes = listOf("Scan one after other", "Scan everything")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, scanTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        scanTypeSpinner.adapter = adapter
        selectedScanType = scanTypes[0]
       /* scanTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedScanType = scanTypes[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }*/
    }

    private fun setupButtons() {
        liveCaptureButton.setOnClickListener {
            navigateToCameraFragment()
        }
    }

    private fun navigateToCameraFragment() {
        try {
            val requiredItems = permitMap[selectedPermit]
            if (requiredItems != null && selectedScanType != null) {
                bottomNavigationView.visibility = View.GONE
                val cameraFragment = CameraFragment().apply {
                    arguments = Bundle().apply {
                        putString("PERMIT", selectedPermit)
                        putStringArrayList("REQUIRED_ITEMS", ArrayList(requiredItems))
                        putString("SCAN_TYPE", selectedScanType)
                    }
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, cameraFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Log.e(TAG, "No permit or scan type selected, or invalid permit")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to CameraFragment", e)
        }
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}