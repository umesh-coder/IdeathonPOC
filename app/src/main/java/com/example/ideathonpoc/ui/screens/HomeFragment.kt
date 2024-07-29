package com.example.ideathonpoc.ui.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.example.ideathonpoc.R
import com.example.ideathonpoc.databinding.FragmentHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var liveCaptureButton: LottieAnimationView
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
            bottomNavigationView = requireActivity().findViewById(R.id.bottomNav)
            bottomNavigationView.visibility = View.VISIBLE
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
            bottomNavigationView.visibility = View.GONE
            val cameraFragment = CameraFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, cameraFragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to CameraFragment", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}