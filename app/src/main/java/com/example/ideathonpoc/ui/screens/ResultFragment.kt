package com.example.ideathonpoc.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ideathonpoc.databinding.FragmentResultBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imagePath = arguments?.getString("imagePath")
        val timestamp = arguments?.getLong("timestamp")

        imagePath?.let { path ->
            val imageFile = File(path)
            if (imageFile.exists()) {
//                binding.screenshotImageView.setImageURI(android.net.Uri.fromFile(imageFile))
            }
        }

        timestamp?.let { time ->
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedTime = sdf.format(Date(time))
//            binding.timestampTextView.text = "Screenshot taken at: $formattedTime"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}