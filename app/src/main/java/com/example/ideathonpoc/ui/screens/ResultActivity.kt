package com.example.ideathonpoc.ui.screens

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.ideathonpoc.R
import com.example.ideathonpoc.databinding.ActivityResultBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imagePath = intent.getStringExtra("imagePath")
        val timestamp = intent.getLongExtra("timestamp", 0L)

        imagePath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            val imageView = findViewById<ImageView>(R.id.screenshotImageView)
            imageView.setImageBitmap(bitmap)
            imageView.rotation = 90f // Rotate the image by 90 degrees

        }

        timestamp.let { time ->
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedTime = sdf.format(Date(time))
            binding.timestampTextView.text = "Screenshot taken at: $formattedTime"
        }
    }

    companion object {
        private const val TAG = "ResultActivity"
    }
}