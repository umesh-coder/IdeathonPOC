package com.example.ideathonpoc.ui.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.example.ideathonpoc.R
import com.example.ideathonpoc.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var liveCaptureButton: ImageButton

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

            setupButtons()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
        }
    }

    private fun setupButtons() {
        liveCaptureButton.setOnClickListener {
            navigateToCameraFragment()
        }


    }

    private fun navigateToCameraFragment() {
        try {
            val cameraFragment = CameraFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, cameraFragment)
                .addToBackStack(null)  // Add to back stack for proper back navigation
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to CameraFragment", e)
        }
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}