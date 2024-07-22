package com.example.ideathonpoc.ui.screens

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ideathonpoc.R
import com.example.ideathonpoc.databinding.ActivityResultBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

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
            findViewById<ImageView>(R.id.screenshotImageView).setImageBitmap(bitmap)
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