package com.example.ideathonpoc.ui.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.example.ideathonpoc.R
import com.example.ideathonpoc.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val permitMap = mapOf(
        "General Permit" to listOf("Helmet", "Safety Vest"),
        "Special Permit" to listOf("Gloves", "Safety Vest"),
        "High-Risk Permit" to listOf("Helmet", "Safety Vest", "Gloves")
    )
    private var selectedPermit: String? = null
    private lateinit var liveCaptureButton: ImageButton
    private lateinit var permitDropdown: AutoCompleteTextView

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
            permitDropdown = binding.permitDropdown

            setupPermitDropdown()
            setupButtons()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
        }
    }

    private fun setupPermitDropdown() {
        val permits = permitMap.keys.toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, permits)
        permitDropdown.setAdapter(adapter)
        permitDropdown.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                (view as AutoCompleteTextView).showDropDown()
            }
        }
        permitDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedPermit = permits[position]
        }
    }

    private fun setupButtons() {
        liveCaptureButton.setOnClickListener {
            navigateToCameraFragment()
        }


    }

    private fun navigateToCameraFragment() {
        try {
            val requiredItems = permitMap[selectedPermit]
            if (requiredItems != null) {
                val cameraFragment = CameraFragment().apply {
                    arguments = Bundle().apply {
                        putString("PERMIT", selectedPermit)
                        putStringArrayList("REQUIRED_ITEMS", ArrayList(requiredItems))
                    }
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, cameraFragment)
                    .addToBackStack(null)  // Add to back stack for proper back navigation
                    .commit()
            } else {
                Log.e(TAG, "No permit selected or invalid permit")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to CameraFragment", e)
        }
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}